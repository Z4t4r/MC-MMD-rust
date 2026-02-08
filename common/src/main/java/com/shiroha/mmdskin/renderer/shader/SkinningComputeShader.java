package com.shiroha.mmdskin.renderer.shader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL46C;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * GPU 蒙皮 Compute Shader
 * 
 * 在 GPU 上预计算蒙皮后的顶点位置和法线，输出到缓冲区。
 * 渲染阶段使用 Minecraft 标准 ShaderInstance 管线，确保 Iris 可以正确拦截。
 * 
 * 原理：
 * - 原始顶点数据（位置、法线、骨骼索引/权重）作为 SSBO 输入
 * - 骨骼矩阵和 Morph 数据作为 SSBO 输入
 * - Compute Shader 计算蒙皮后的位置和法线，写入输出 SSBO
 * - 输出缓冲区随后作为 VBO 绑定到标准渲染管线
 * 
 * SSBO 绑定布局：
 * - binding 0: 原始顶点位置（只读）
 * - binding 1: 原始顶点法线（只读）
 * - binding 2: 骨骼索引（只读）
 * - binding 3: 骨骼权重（只读）
 * - binding 4: 骨骼矩阵（只读，每帧更新）
 * - binding 5: Morph 偏移数据（只读，静态）
 * - binding 6: Morph 权重（只读，每帧更新）
 * - binding 7: 蒙皮后顶点位置（写入）
 * - binding 8: 蒙皮后顶点法线（写入）
 */
public class SkinningComputeShader {
    private static final Logger logger = LogManager.getLogger();
    
    private static final int LOCAL_SIZE_X = 256;
    
    // 最大骨骼数量
    public static int MAX_BONES = 2048;
    
    // Compute Shader 程序
    private int program = 0;
    private boolean initialized = false;
    
    // Uniform locations
    private int vertexCountLocation = -1;
    private int morphCountLocation = -1;
    private int maxBonesLocation = -1;
    private int uvMorphCountLocation = -1;
    
    // SSBO 绑定点常量
    private static final int BINDING_ORIG_POSITIONS = 0;
    private static final int BINDING_ORIG_NORMALS = 1;
    private static final int BINDING_BONE_INDICES = 2;
    private static final int BINDING_BONE_WEIGHTS = 3;
    private static final int BINDING_BONE_MATRICES = 4;
    private static final int BINDING_MORPH_OFFSETS = 5;
    private static final int BINDING_MORPH_WEIGHTS = 6;
    private static final int BINDING_SKINNED_POSITIONS = 7;
    private static final int BINDING_SKINNED_NORMALS = 8;
    private static final int BINDING_ORIG_UVS = 9;
    private static final int BINDING_UV_MORPH_OFFSETS = 10;
    private static final int BINDING_UV_MORPH_WEIGHTS = 11;
    private static final int BINDING_SKINNED_UVS = 12;
    
    
    // Compute Shader 源码
    private static final String COMPUTE_SHADER_SOURCE = """
        #version 430 core
        
        layout(local_size_x = 256) in;
        
        // 输入数据（只读）
        layout(std430, binding = 0) readonly buffer OriginalPositions {
            float origPositions[];
        };
        
        layout(std430, binding = 1) readonly buffer OriginalNormals {
            float origNormals[];
        };
        
        layout(std430, binding = 2) readonly buffer BoneIndicesBuffer {
            int boneIndices[];
        };
        
        layout(std430, binding = 3) readonly buffer BoneWeightsBuffer {
            float boneWeights[];
        };
        
        layout(std430, binding = 4) readonly buffer BoneMatrices {
            mat4 boneMatrices[];
        };
        
        // 顶点 Morph 数据（只读）
        layout(std430, binding = 5) readonly buffer MorphOffsets {
            float morphOffsets[];
        };
        
        layout(std430, binding = 6) readonly buffer MorphWeights {
            float morphWeights[];
        };
        
        // 输出数据（写入）
        layout(std430, binding = 7) writeonly buffer SkinnedPositions {
            float skinnedPositions[];
        };
        
        layout(std430, binding = 8) writeonly buffer SkinnedNormals {
            float skinnedNormals[];
        };
        
        // UV Morph 数据（只读）
        layout(std430, binding = 9) readonly buffer OriginalUVs {
            float origUVs[];
        };
        
        layout(std430, binding = 10) readonly buffer UvMorphOffsets {
            float uvMorphOffsets[];
        };
        
        layout(std430, binding = 11) readonly buffer UvMorphWeights {
            float uvMorphWeights[];
        };
        
        // 蒙皮后 UV（写入）
        layout(std430, binding = 12) writeonly buffer SkinnedUVs {
            float skinnedUVs[];
        };
        
        uniform int VertexCount;
        uniform int MorphCount;
        uniform int MaxBones;
        uniform int UvMorphCount;
        
        void main() {
            uint vid = gl_GlobalInvocationID.x;
            if (vid >= VertexCount) return;
            
            uint base3 = vid * 3;
            uint base4 = vid * 4;
            uint base2 = vid * 2;
            
            // 读取原始位置和法线
            vec3 pos = vec3(origPositions[base3], origPositions[base3 + 1], origPositions[base3 + 2]);
            vec3 nor = vec3(origNormals[base3], origNormals[base3 + 1], origNormals[base3 + 2]);
            
            // 应用顶点 Morph 偏移
            if (MorphCount > 0) {
                for (int m = 0; m < MorphCount && m < 128; m++) {
                    float w = morphWeights[m];
                    if (w > 0.001) {
                        uint offsetIdx = uint(m) * uint(VertexCount) * 3u + vid * 3u;
                        pos.x += morphOffsets[offsetIdx] * w;
                        pos.y += morphOffsets[offsetIdx + 1u] * w;
                        pos.z += morphOffsets[offsetIdx + 2u] * w;
                    }
                }
            }
            
            // 读取骨骼数据
            ivec4 bi = ivec4(
                boneIndices[base4], boneIndices[base4 + 1],
                boneIndices[base4 + 2], boneIndices[base4 + 3]
            );
            vec4 bw = vec4(
                boneWeights[base4], boneWeights[base4 + 1],
                boneWeights[base4 + 2], boneWeights[base4 + 3]
            );
            
            // 计算蒙皮矩阵（归一化权重）
            float totalWeight = 0.0;
            for (int i = 0; i < 4; i++) {
                if (bi[i] >= 0 && bi[i] < MaxBones) {
                    totalWeight += bw[i];
                }
            }
            
            mat4 skinMatrix = mat4(0.0);
            if (totalWeight > 0.001) {
                float invWeight = 1.0 / totalWeight;
                for (int i = 0; i < 4; i++) {
                    if (bi[i] >= 0 && bi[i] < MaxBones && bw[i] > 0.0) {
                        skinMatrix += boneMatrices[bi[i]] * (bw[i] * invWeight);
                    }
                }
            } else {
                skinMatrix = mat4(1.0);
            }
            
            // 应用蒙皮变换
            vec4 skinnedPos = skinMatrix * vec4(pos, 1.0);
            mat3 normalMat = mat3(skinMatrix);
            vec3 skinnedNor = normalize(normalMat * nor);
            
            // 写入位置/法线输出
            skinnedPositions[base3] = skinnedPos.x;
            skinnedPositions[base3 + 1] = skinnedPos.y;
            skinnedPositions[base3 + 2] = skinnedPos.z;
            
            skinnedNormals[base3] = skinnedNor.x;
            skinnedNormals[base3 + 1] = skinnedNor.y;
            skinnedNormals[base3 + 2] = skinnedNor.z;
            
            // 应用 UV Morph 偏移并写入输出（UvMorphCount < 0 表示无 UV 处理）
            if (UvMorphCount >= 0) {
                float u = origUVs[base2];
                float v = origUVs[base2 + 1];
                if (UvMorphCount > 0) {
                    for (int m = 0; m < UvMorphCount && m < 32; m++) {
                        float w = uvMorphWeights[m];
                        if (abs(w) > 0.001) {
                            uint uvIdx = uint(m) * uint(VertexCount) * 2u + vid * 2u;
                            u += uvMorphOffsets[uvIdx] * w;
                            v += uvMorphOffsets[uvIdx + 1u] * w;
                        }
                    }
                }
                skinnedUVs[base2] = u;
                skinnedUVs[base2 + 1] = v;
            }
        }
        """;
    
    /**
     * 初始化 Compute Shader
     */
    public boolean init() {
        if (initialized) return true;
        
        try {
            int computeShader = GL43C.glCreateShader(GL43C.GL_COMPUTE_SHADER);
            GL43C.glShaderSource(computeShader, COMPUTE_SHADER_SOURCE);
            GL43C.glCompileShader(computeShader);
            
            if (GL43C.glGetShaderi(computeShader, GL43C.GL_COMPILE_STATUS) == GL43C.GL_FALSE) {
                String log = GL43C.glGetShaderInfoLog(computeShader, 8192).trim();
                logger.error("蒙皮 Compute Shader 编译失败: {}", log);
                GL43C.glDeleteShader(computeShader);
                return false;
            }
            
            program = GL43C.glCreateProgram();
            GL43C.glAttachShader(program, computeShader);
            GL43C.glLinkProgram(program);
            
            if (GL43C.glGetProgrami(program, GL43C.GL_LINK_STATUS) == GL43C.GL_FALSE) {
                String log = GL43C.glGetProgramInfoLog(program, 8192).trim();
                logger.error("蒙皮 Compute Shader 链接失败: {}", log);
                GL43C.glDeleteProgram(program);
                GL43C.glDeleteShader(computeShader);
                program = 0;
                return false;
            }
            
            GL43C.glDeleteShader(computeShader);
            
            // 获取 Uniform locations
            vertexCountLocation = GL43C.glGetUniformLocation(program, "VertexCount");
            morphCountLocation = GL43C.glGetUniformLocation(program, "MorphCount");
            maxBonesLocation = GL43C.glGetUniformLocation(program, "MaxBones");
            uvMorphCountLocation = GL43C.glGetUniformLocation(program, "UvMorphCount");
            
            initialized = true;
            logger.info("蒙皮 Compute Shader 初始化成功");
            return true;
            
        } catch (Exception e) {
            logger.error("蒙皮 Compute Shader 初始化异常", e);
            return false;
        }
    }
    
    /**
     * 创建一对输出缓冲区（蒙皮后的顶点位置和法线）
     * 返回 int[2]：{skinnedPositionsBuffer, skinnedNormalsBuffer}
     * 调用方负责管理这些缓冲区的生命周期
     */
    public static int[] createOutputBuffers(int vertexCount) {
        long bufferSize = (long) vertexCount * 3 * 4; // vec3 * sizeof(float)
        
        int posBuffer = GL46C.glGenBuffers();
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, posBuffer);
        GL46C.glBufferData(GL46C.GL_COPY_WRITE_BUFFER, bufferSize, GL46C.GL_DYNAMIC_COPY);
        
        int norBuffer = GL46C.glGenBuffers();
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, norBuffer);
        GL46C.glBufferData(GL46C.GL_COPY_WRITE_BUFFER, bufferSize, GL46C.GL_DYNAMIC_COPY);
        
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
        return new int[]{posBuffer, norBuffer};
    }
    
    /**
     * 执行蒙皮计算（含 UV Morph）
     * 
     * @param origPosBuffer       原始顶点位置 VBO
     * @param origNorBuffer       原始顶点法线 VBO
     * @param boneIdxBuffer       骨骼索引 VBO
     * @param boneWgtBuffer       骨骼权重 VBO
     * @param outSkinnedPosBuffer 输出蒙皮后位置缓冲区
     * @param outSkinnedNorBuffer 输出蒙皮后法线缓冲区
     * @param boneMatrixSSBO      骨骼矩阵 SSBO
     * @param morphOffsetsSSBO    顶点 Morph 偏移 SSBO
     * @param morphWeightsSSBO    顶点 Morph 权重 SSBO
     * @param vertexCount         顶点数量
     * @param morphCount          顶点 Morph 数量
     * @param origUvBuffer        原始 UV VBO
     * @param uvMorphOffsetsSSBO  UV Morph 偏移 SSBO
     * @param uvMorphWeightsSSBO  UV Morph 权重 SSBO
     * @param outSkinnedUvBuffer  输出蒙皮后 UV 缓冲区
     * @param uvMorphCount        UV Morph 数量
     */
    public void dispatch(int origPosBuffer, int origNorBuffer,
                         int boneIdxBuffer, int boneWgtBuffer,
                         int outSkinnedPosBuffer, int outSkinnedNorBuffer,
                         int boneMatrixSSBO,
                         int morphOffsetsSSBO, int morphWeightsSSBO,
                         int vertexCount, int morphCount,
                         int origUvBuffer,
                         int uvMorphOffsetsSSBO, int uvMorphWeightsSSBO,
                         int outSkinnedUvBuffer, int uvMorphCount) {
        if (!initialized || program == 0) return;
        
        int savedProgram = GL46C.glGetInteger(GL46C.GL_CURRENT_PROGRAM);
        var savedSSBO = new SSBOBindings();
        
        GL43C.glUseProgram(program);
        
        if (vertexCountLocation >= 0) GL43C.glUniform1i(vertexCountLocation, vertexCount);
        if (morphCountLocation >= 0) GL43C.glUniform1i(morphCountLocation, morphCount);
        if (maxBonesLocation >= 0) GL43C.glUniform1i(maxBonesLocation, MAX_BONES);
        if (uvMorphCountLocation >= 0) GL43C.glUniform1i(uvMorphCountLocation, uvMorphCount);
        
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_ORIG_POSITIONS, origPosBuffer);
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_ORIG_NORMALS, origNorBuffer);
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_BONE_INDICES, boneIdxBuffer);
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_BONE_WEIGHTS, boneWgtBuffer);
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_BONE_MATRICES, boneMatrixSSBO);
        if (morphCount > 0 && morphOffsetsSSBO != 0) {
            GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_MORPH_OFFSETS, morphOffsetsSSBO);
        }
        if (morphCount > 0 && morphWeightsSSBO != 0) {
            GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_MORPH_WEIGHTS, morphWeightsSSBO);
        }
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_SKINNED_POSITIONS, outSkinnedPosBuffer);
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_SKINNED_NORMALS, outSkinnedNorBuffer);
        
        // UV 相关 SSBO
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_ORIG_UVS, origUvBuffer);
        if (uvMorphCount > 0 && uvMorphOffsetsSSBO != 0) {
            GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_UV_MORPH_OFFSETS, uvMorphOffsetsSSBO);
        }
        if (uvMorphCount > 0 && uvMorphWeightsSSBO != 0) {
            GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_UV_MORPH_WEIGHTS, uvMorphWeightsSSBO);
        }
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING_SKINNED_UVS, outSkinnedUvBuffer);
        
        int groupCount = (vertexCount + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
        GL43C.glDispatchCompute(groupCount, 1, 1);
        
        GL43C.glMemoryBarrier(GL43C.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT | GL43C.GL_SHADER_STORAGE_BARRIER_BIT);
        
        savedSSBO.restore();
        GL43C.glUseProgram(savedProgram);
    }
    
    /**
     * 执行蒙皮计算（无 UV Morph 的兼容版本）
     */
    public void dispatch(int origPosBuffer, int origNorBuffer,
                         int boneIdxBuffer, int boneWgtBuffer,
                         int outSkinnedPosBuffer, int outSkinnedNorBuffer,
                         int boneMatrixSSBO,
                         int morphOffsetsSSBO, int morphWeightsSSBO,
                         int vertexCount, int morphCount) {
        // 无 UV 处理：传 -1 作为 uvMorphCount 哨兵值，shader 中跳过 UV 读写
        dispatch(origPosBuffer, origNorBuffer,
                 boneIdxBuffer, boneWgtBuffer,
                 outSkinnedPosBuffer, outSkinnedNorBuffer,
                 boneMatrixSSBO,
                 morphOffsetsSSBO, morphWeightsSSBO,
                 vertexCount, morphCount,
                 0, 0, 0, 0, -1);
    }
    
    /**
     * 上传骨骼矩阵到指定 SSBO（每实例独立）
     */
    public void uploadBoneMatrices(int boneMatrixSSBO, FloatBuffer matrices, int boneCount) {
        if (!initialized || boneMatrixSSBO == 0) return;
        
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, boneMatrixSSBO);
        int actualBones = Math.min(boneCount, MAX_BONES);
        int savedLimit = matrices.limit();
        matrices.limit(actualBones * 16);
        matrices.position(0);
        GL46C.glBufferSubData(GL46C.GL_COPY_WRITE_BUFFER, 0, matrices);
        matrices.limit(savedLimit);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
    }
    
    /**
     * 上传 Morph 偏移数据到指定 SSBO（静态，只需上传一次）
     */
    public void uploadMorphOffsets(int morphOffsetsSSBO, ByteBuffer data) {
        if (!initialized || morphOffsetsSSBO == 0) return;
        
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, morphOffsetsSSBO);
        GL46C.glBufferData(GL46C.GL_COPY_WRITE_BUFFER, data, GL46C.GL_STATIC_DRAW);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
    }
    
    /**
     * 更新 Morph 权重到指定 SSBO（每帧调用）
     */
    public void updateMorphWeights(int morphWeightsSSBO, FloatBuffer weights) {
        if (!initialized || morphWeightsSSBO == 0) return;
        
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, morphWeightsSSBO);
        weights.position(0);
        GL46C.glBufferSubData(GL46C.GL_COPY_WRITE_BUFFER, 0, weights);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
    }
    
    /**
     * 创建一对 Morph SSBO（每模型实例独立）
     * 返回 int[2]：{morphOffsetsSSBO, morphWeightsSSBO}
     * 调用方负责管理生命周期
     */
    public static int[] createMorphBuffers(int morphCount) {
        int offsetsSSBO = GL46C.glGenBuffers();
        int weightsSSBO = GL46C.glGenBuffers();
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, weightsSSBO);
        GL46C.glBufferData(GL46C.GL_COPY_WRITE_BUFFER, (long) morphCount * 4, GL46C.GL_DYNAMIC_DRAW);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
        return new int[]{offsetsSSBO, weightsSSBO};
    }
    
    /**
     * 创建 UV Morph SSBO（每模型实例独立）
     * 返回 int[2]：{uvMorphOffsetsSSBO, uvMorphWeightsSSBO}
     */
    public static int[] createUvMorphBuffers(int uvMorphCount) {
        int offsetsSSBO = GL46C.glGenBuffers();
        int weightsSSBO = GL46C.glGenBuffers();
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, weightsSSBO);
        GL46C.glBufferData(GL46C.GL_COPY_WRITE_BUFFER, (long) uvMorphCount * 4, GL46C.GL_DYNAMIC_DRAW);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
        return new int[]{offsetsSSBO, weightsSSBO};
    }
    
    /**
     * 创建蒙皮后 UV 输出缓冲区
     */
    public static int createSkinnedUvBuffer(int vertexCount) {
        int buffer = GL46C.glGenBuffers();
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, buffer);
        GL46C.glBufferData(GL46C.GL_COPY_WRITE_BUFFER, (long) vertexCount * 2 * 4, GL46C.GL_DYNAMIC_COPY);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
        return buffer;
    }
    
    /**
     * 上传 UV Morph 偏移数据到指定 SSBO
     */
    public void uploadUvMorphOffsets(int uvMorphOffsetsSSBO, java.nio.ByteBuffer data) {
        if (!initialized || uvMorphOffsetsSSBO == 0) return;
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, uvMorphOffsetsSSBO);
        GL46C.glBufferData(GL46C.GL_COPY_WRITE_BUFFER, data, GL46C.GL_STATIC_DRAW);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
    }
    
    /**
     * 更新 UV Morph 权重到指定 SSBO
     */
    public void updateUvMorphWeights(int uvMorphWeightsSSBO, FloatBuffer weights) {
        if (!initialized || uvMorphWeightsSSBO == 0) return;
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, uvMorphWeightsSSBO);
        weights.position(0);
        GL46C.glBufferSubData(GL46C.GL_COPY_WRITE_BUFFER, 0, weights);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 创建骨骼矩阵 SSBO（每模型实例独立）
     * 调用方负责管理生命周期
     */
    public static int createBoneMatrixBuffer() {
        int ssbo = GL46C.glGenBuffers();
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, ssbo);
        GL46C.glBufferData(GL46C.GL_COPY_WRITE_BUFFER, (long) MAX_BONES * 64, GL46C.GL_DYNAMIC_DRAW);
        GL46C.glBindBuffer(GL46C.GL_COPY_WRITE_BUFFER, 0);
        return ssbo;
    }
    
    /**
     * 释放资源
     */
    public void cleanup() {
        if (program > 0) {
            GL43C.glDeleteProgram(program);
            program = 0;
        }
        initialized = false;
    }
}
