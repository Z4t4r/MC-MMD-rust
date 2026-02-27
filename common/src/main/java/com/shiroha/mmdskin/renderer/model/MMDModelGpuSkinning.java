package com.shiroha.mmdskin.renderer.model;

import com.shiroha.mmdskin.NativeFunc;
import com.shiroha.mmdskin.config.ConfigManager;
import com.shiroha.mmdskin.renderer.core.IrisCompat;
import com.shiroha.mmdskin.renderer.resource.MMDTextureManager;
import com.shiroha.mmdskin.renderer.shader.ShaderConstants;
import com.shiroha.mmdskin.renderer.shader.SkinningComputeShader;
import com.shiroha.mmdskin.renderer.shader.ToonShaderCpu;
import com.shiroha.mmdskin.renderer.shader.ToonConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * GPU 蒙皮 MMD 模型渲染器
 * 
 * 使用 Compute Shader 在 GPU 上预计算蒙皮，然后通过 Minecraft 标准 ShaderInstance 管线渲染。
 * 这样 Iris 可以正确拦截渲染着色器，解决光影下模型透明的问题。
 * 
 * 流程：
 * 1. Compute Shader 读取原始顶点 + 骨骼矩阵 → 输出蒙皮后的顶点/法线
 * 2. 使用 Minecraft 标准管线（RenderSystem.getShader()）进行渲染
 * 3. Iris 拦截 ShaderInstance 替换为 G-buffer 着色器 → 光影正常工作
 */
public class MMDModelGpuSkinning extends AbstractMMDModel {
    private static SkinningComputeShader computeShader;
    private static ToonShaderCpu toonShaderCpu;
    private static final ToonConfig toonConfig = ToonConfig.getInstance();
    
    // 模型数据
    private int vertexCount;
    
    // OpenGL 资源 - VAO
    private int vertexArrayObject;
    private int indexBufferObject;
    
    // 原始数据 VBO（静态，作为 Compute Shader 的 SSBO 输入）
    private int positionBufferObject;
    private int normalBufferObject;
    private int uv0BufferObject;
    private int boneIndicesBufferObject;
    private int boneWeightsBufferObject;
    
    // Minecraft 标准顶点属性 VBO
    private int colorBufferObject;
    private int uv1BufferObject;
    private int uv2BufferObject;
    
    // Compute Shader 输出缓冲区（每实例独立，同时作为 SSBO 和 VBO）
    private int skinnedPositionsBuffer;
    private int skinnedNormalsBuffer;
    
    // 骨骼矩阵 SSBO（每实例独立，避免多模型数据冲突）
    private int boneMatrixSSBO = 0;
    
    // 缓冲区（allocateDirect 分配，由 GC 回收）
    @SuppressWarnings("unused")
    private ByteBuffer posBuffer;
    @SuppressWarnings("unused")
    private ByteBuffer norBuffer;
    @SuppressWarnings("unused")
    private ByteBuffer uv0Buffer;
    @SuppressWarnings("unused")
    private ByteBuffer colorBuffer;
    @SuppressWarnings("unused")
    private ByteBuffer uv1Buffer;
    private ByteBuffer uv2Buffer;
    private FloatBuffer boneMatricesBuffer;
    private FloatBuffer modelViewMatBuff;
    private FloatBuffer projMatBuff;
    
    // 预分配的骨骼矩阵复制缓冲区（避免每帧 allocateDirect）
    private ByteBuffer boneMatricesByteBuffer;
    
    // 顶点 Morph 数据
    private int vertexMorphCount = 0;
    private boolean morphDataUploaded = false;
    private FloatBuffer morphWeightsBuffer;
    private ByteBuffer morphWeightsByteBuffer;
    private int morphOffsetsSSBO = 0;
    private int morphWeightsSSBO = 0;
    
    // UV Morph 数据
    private int uvMorphCount = 0;
    private boolean uvMorphDataUploaded = false;
    private FloatBuffer uvMorphWeightsBuffer;
    private ByteBuffer uvMorphWeightsByteBuffer;
    private int uvMorphOffsetsSSBO = 0;
    private int uvMorphWeightsSSBO = 0;
    private int skinnedUvBuffer = 0;
    
    private int indexElementSize;
    private int indexType;
    private MMDMaterial[] mats;
    private MMDMaterial lightMapMaterial;
    
    // 光照方向（预分配复用）
    private final Vector3f light0Direction = new Vector3f();
    private final Vector3f light1Direction = new Vector3f();
    
    // 着色器属性位置（G1 优化：按 shaderProgram 缓存，避免每帧重复查询）
    private int shaderProgram;
    private int cachedShaderProgram = -1;
    private int positionLocation, normalLocation;
    private int uv0Location, uv1Location, uv2Location;
    private int colorLocation;
    // Iris 重命名的属性
    private int I_positionLocation, I_normalLocation;
    private int I_uv0Location, I_uv2Location, I_colorLocation;
    
    // G4 优化：缓存子网格数量（静态值，避免每帧 JNI 查询）
    private int subMeshCount;
    
    // G3 优化：批量子网格元数据缓冲区（每子网格 20 字节，每帧复用）
    private ByteBuffer subMeshDataBuf;
    
    // 临时存储当前 PoseStack，供 renderNormal 使用
    private PoseStack currentDeliverStack;
    
    private boolean initialized = false;
    
    private MMDModelGpuSkinning() {}
    
    /**
     * 创建 GPU 蒙皮模型（Compute Shader 方案）
     */
    public static MMDModelGpuSkinning Create(String modelFilename, String modelDir, boolean isPMD, long layerCount) {
        NativeFunc nf = getNf();
        
        // 初始化 Compute Shader（懒加载，全局共享）
        if (computeShader == null) {
            computeShader = new SkinningComputeShader();
            if (!computeShader.init()) {
                logger.error("蒙皮 Compute Shader 初始化失败，回退到 CPU 蒙皮");
                computeShader = null;
                return null;
            }
        }
        
        // 加载模型
        long model;
        if (isPMD) {
            model = nf.LoadModelPMD(modelFilename, modelDir, layerCount);
        } else {
            model = nf.LoadModelPMX(modelFilename, modelDir, layerCount);
        }
        
        if (model == 0) {
            logger.warn("无法打开模型: '{}'", modelFilename);
            return null;
        }
        
        MMDModelGpuSkinning result = createFromHandle(model, modelDir);
        if (result == null) {
            // createFromHandle 不再负责删除句柄，同步调用路径需自行清理
            nf.DeleteModel(model);
        }
        return result;
    }
    
    /**
     * 从已加载的模型句柄创建渲染实例（Phase 2：GL 资源创建，必须在渲染线程调用）
     * Phase 1（nf.LoadModelPMX/PMD）已在后台线程完成
     */
    public static MMDModelGpuSkinning createFromHandle(long model, String modelDir) {
        NativeFunc nf = getNf();
        
        // 初始化 Compute Shader（懒加载，全局共享）
        if (computeShader == null) {
            computeShader = new SkinningComputeShader();
            if (!computeShader.init()) {
                logger.error("蒙皮 Compute Shader 初始化失败，回退到 CPU 蒙皮");
                computeShader = null; // 重置，防止后续模型跳过 init 检查
                // 注意：不删除 model 句柄，由调用者（RenderModeManager）负责管理，
                // 避免多工厂回退时 use-after-free
                return null;
            }
        }
        
        // 资源追踪变量（用于异常时清理）
        int vao = 0, indexVbo = 0, posVbo = 0, norVbo = 0, uv0Vbo = 0;
        int boneIdxVbo = 0, boneWgtVbo = 0, colorVbo = 0, uv1Vbo = 0, uv2Vbo = 0;
        int[] outputBuffers = null;
        int boneMatrixSSBO = 0;
        int[] morphBuffers = null;
        FloatBuffer boneMatricesBuffer = null;
        ByteBuffer boneMatricesByteBuffer = null;
        FloatBuffer modelViewMatBuff = null;
        FloatBuffer projMatBuff = null;
        FloatBuffer morphWeightsBuffer = null;
        int[] uvMorphBuffers = null;
        FloatBuffer uvMorphWeightsBuf = null;
        int skinnedUvBuf = 0;
        ByteBuffer matMorphResultsByteBuf = null;
        ByteBuffer subMeshDataBufLocal = null;
        MMDMaterial lightMapMaterial = null;
        
        try {
            // 初始化 GPU 蒙皮数据
            nf.InitGpuSkinningData(model);
            
            BufferUploader.reset();
            
            int vertexCount = (int) nf.GetVertexCount(model);
            int boneCount = nf.GetBoneCount(model);
            
            if (boneCount > ShaderConstants.MAX_BONES) {
                logger.warn("模型骨骼数量 ({}) 超过最大支持 ({})，部分骨骼可能无法正确渲染", 
                    boneCount, ShaderConstants.MAX_BONES);
            }
            
            // 创建 VAO 和 VBO
            vao = GL46C.glGenVertexArrays();
            indexVbo = GL46C.glGenBuffers();
            posVbo = GL46C.glGenBuffers();
            norVbo = GL46C.glGenBuffers();
            uv0Vbo = GL46C.glGenBuffers();
            boneIdxVbo = GL46C.glGenBuffers();
            boneWgtVbo = GL46C.glGenBuffers();
            colorVbo = GL46C.glGenBuffers();
            uv1Vbo = GL46C.glGenBuffers();
            uv2Vbo = GL46C.glGenBuffers();
            
            GL46C.glBindVertexArray(vao);
            
            // 索引缓冲区
            int indexElementSize = (int) nf.GetIndexElementSize(model);
            int indexCount = (int) nf.GetIndexCount(model);
            int indexSize = indexCount * indexElementSize;
            long indexData = nf.GetIndices(model);
            ByteBuffer indexBuffer = ByteBuffer.allocateDirect(indexSize);
            nf.CopyDataToByteBuffer(indexBuffer, indexData, indexSize);
            indexBuffer.position(0);
            GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexVbo);
            GL46C.glBufferData(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL46C.GL_STATIC_DRAW);
            
            int indexType = switch (indexElementSize) {
                case 1 -> GL46C.GL_UNSIGNED_BYTE;
                case 2 -> GL46C.GL_UNSIGNED_SHORT;
                case 4 -> GL46C.GL_UNSIGNED_INT;
                default -> 0;
            };
            
            // 原始顶点位置（静态，用于 Compute Shader 输入）
            ByteBuffer posBuffer = ByteBuffer.allocateDirect(vertexCount * 12);
            posBuffer.order(ByteOrder.LITTLE_ENDIAN);
            int copiedPos = nf.CopyOriginalPositionsToBuffer(model, posBuffer, vertexCount);
            if (copiedPos == 0) {
                logger.warn("原始顶点位置数据复制失败");
            }
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, posVbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, posBuffer, GL46C.GL_STATIC_DRAW);
            
            // 原始法线（静态）
            ByteBuffer norBuffer = ByteBuffer.allocateDirect(vertexCount * 12);
            norBuffer.order(ByteOrder.LITTLE_ENDIAN);
            int copiedNor = nf.CopyOriginalNormalsToBuffer(model, norBuffer, vertexCount);
            if (copiedNor == 0) {
                logger.warn("原始法线数据复制失败");
            }
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, norVbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, norBuffer, GL46C.GL_STATIC_DRAW);
            
            // UV（静态）
            ByteBuffer uv0Buffer = ByteBuffer.allocateDirect(vertexCount * 8);
            uv0Buffer.order(ByteOrder.LITTLE_ENDIAN);
            long uvData = nf.GetUVs(model);
            nf.CopyDataToByteBuffer(uv0Buffer, uvData, vertexCount * 8);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv0Vbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv0Buffer, GL46C.GL_STATIC_DRAW);
            
            // 骨骼索引（静态，ivec4）
            ByteBuffer boneIndicesByteBuffer = ByteBuffer.allocateDirect(vertexCount * 16);
            boneIndicesByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            int copiedIdx = nf.CopyBoneIndicesToBuffer(model, boneIndicesByteBuffer, vertexCount);
            if (copiedIdx == 0) {
                logger.warn("骨骼索引数据复制失败");
            }
            IntBuffer boneIndicesBuffer = boneIndicesByteBuffer.asIntBuffer();
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, boneIdxVbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, boneIndicesBuffer, GL46C.GL_STATIC_DRAW);
            
            // 骨骼权重（静态，vec4）
            ByteBuffer boneWeightsByteBuffer = ByteBuffer.allocateDirect(vertexCount * 16);
            boneWeightsByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            int copiedWgt = nf.CopyBoneWeightsToBuffer(model, boneWeightsByteBuffer, vertexCount);
            if (copiedWgt == 0) {
                logger.warn("骨骼权重数据复制失败");
            }
            FloatBuffer boneWeightsFloatBuffer = boneWeightsByteBuffer.asFloatBuffer();
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, boneWgtVbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, boneWeightsFloatBuffer, GL46C.GL_STATIC_DRAW);
            
            // 顶点颜色缓冲区（Minecraft 标准属性：白色 + 全不透明）
            ByteBuffer colorBuffer = ByteBuffer.allocateDirect(vertexCount * 16);
            colorBuffer.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < vertexCount; i++) {
                colorBuffer.putFloat(1.0f);
                colorBuffer.putFloat(1.0f);
                colorBuffer.putFloat(1.0f);
                colorBuffer.putFloat(1.0f);
            }
            colorBuffer.flip();
            
            // UV1 缓冲区（overlay）— 静态数据，创建时即上传到 GPU
            ByteBuffer uv1Buffer = ByteBuffer.allocateDirect(vertexCount * 8);
            uv1Buffer.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < vertexCount; i++) {
                uv1Buffer.putInt(15);
                uv1Buffer.putInt(15);
            }
            uv1Buffer.flip();
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv1Vbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv1Buffer, GL46C.GL_STATIC_DRAW);
            
            // UV2 缓冲区（lightmap）
            ByteBuffer uv2Buffer = ByteBuffer.allocateDirect(vertexCount * 8);
            uv2Buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // 安卓兼容：上传白色 Color VBO（替代 glVertexAttrib4f 常量属性）
            // 安卓 GL 翻译层（gl4es/ANGLE）对 glVertexAttrib4f 常量属性支持不完整，
            // 导致 Color.a=0 → entity_cutout 着色器 discard → 模型全透明
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, colorVbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, colorBuffer, GL46C.GL_STATIC_DRAW);
            // 预分配 UV2 VBO（每帧更新光照数据）
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2Vbo);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, vertexCount * 8, GL46C.GL_DYNAMIC_DRAW);
            
            // 材质（记录纹理引用键）
            List<String> texKeys = new ArrayList<>();
            MMDMaterial[] mats = new MMDMaterial[(int) nf.GetMaterialCount(model)];
            for (int i = 0; i < mats.length; ++i) {
                mats[i] = new MMDMaterial();
                String texFilename = nf.GetMaterialTex(model, i);
                if (texFilename != null && !texFilename.isEmpty()) {
                    MMDTextureManager.Texture mgrTex = MMDTextureManager.GetTexture(texFilename);
                    if (mgrTex != null) {
                        mats[i].tex = mgrTex.tex;
                        mats[i].hasAlpha = mgrTex.hasAlpha;
                        MMDTextureManager.addRef(texFilename);
                        texKeys.add(texFilename);
                    }
                }
            }
            
            // lightMap 材质
            lightMapMaterial = new MMDMaterial();
            String lightMapPath = modelDir + "/lightMap.png";
            MMDTextureManager.Texture mgrTex = MMDTextureManager.GetTexture(lightMapPath);
            if (mgrTex != null) {
                lightMapMaterial.tex = mgrTex.tex;
                lightMapMaterial.hasAlpha = mgrTex.hasAlpha;
                MMDTextureManager.addRef(lightMapPath);
                texKeys.add(lightMapPath);
            } else {
                lightMapMaterial.tex = GL46C.glGenTextures();
                lightMapMaterial.ownsTexture = true;
                GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, lightMapMaterial.tex);
                ByteBuffer texBuffer = ByteBuffer.allocateDirect(16 * 16 * 4);
                texBuffer.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < 16 * 16; i++) {
                    texBuffer.put((byte) 255);
                    texBuffer.put((byte) 255);
                    texBuffer.put((byte) 255);
                    texBuffer.put((byte) 255);
                }
                texBuffer.flip();
                GL46C.glTexImage2D(GL46C.GL_TEXTURE_2D, 0, GL46C.GL_RGBA, 16, 16, 0, GL46C.GL_RGBA, GL46C.GL_UNSIGNED_BYTE, texBuffer);
                GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAX_LEVEL, 0);
                GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_LINEAR);
                GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_LINEAR);
                GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, 0);
                lightMapMaterial.hasAlpha = true;
            }
            
            // 骨骼矩阵缓冲区（统一使用 MemoryUtil，在 dispose() 中显式释放）
            boneMatricesBuffer = MemoryUtil.memAllocFloat(boneCount * 16);
            boneMatricesByteBuffer = MemoryUtil.memAlloc(boneCount * 64);
            boneMatricesByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // 创建 Compute Shader 输出缓冲区（每实例独立，双重用途：SSBO + VBO）
            outputBuffers = SkinningComputeShader.createOutputBuffers(vertexCount);
            
            // 创建骨骼矩阵 SSBO（每实例独立）
            boneMatrixSSBO = SkinningComputeShader.createBoneMatrixBuffer();
            
            // 预分配矩阵缓冲区
            modelViewMatBuff = MemoryUtil.memAllocFloat(16);
            projMatBuff = MemoryUtil.memAllocFloat(16);
            
            // 初始化顶点 Morph 数据
            nf.InitGpuMorphData(model);
            int morphCount = (int) nf.GetVertexMorphCount(model);
            if (morphCount > 0) {
                morphWeightsBuffer = MemoryUtil.memAllocFloat(morphCount);
                morphBuffers = SkinningComputeShader.createMorphBuffers(morphCount);
            }
            
            // 初始化 UV Morph 数据
            nf.InitGpuUvMorphData(model);
            int uvMorphCnt = nf.GetUvMorphCount(model);
            if (uvMorphCnt > 0) {
                uvMorphWeightsBuf = MemoryUtil.memAllocFloat(uvMorphCnt);
                uvMorphBuffers = SkinningComputeShader.createUvMorphBuffers(uvMorphCnt);
                skinnedUvBuf = SkinningComputeShader.createSkinnedUvBuffer(vertexCount);
            } else {
                // 即使没有 UV Morph，也创建蒙皮 UV 输出缓冲区用于 Compute Shader 写入
                skinnedUvBuf = SkinningComputeShader.createSkinnedUvBuffer(vertexCount);
            }
            
            // 初始化材质 Morph 结果缓冲区
            int matMorphCount = nf.GetMaterialMorphResultCount(model);
            if (matMorphCount > 0) {
                int floatCount = matMorphCount * 56;
                matMorphResultsByteBuf = MemoryUtil.memAlloc(floatCount * 4);
                matMorphResultsByteBuf.order(ByteOrder.LITTLE_ENDIAN);
            }
            
            // 构建结果
            MMDModelGpuSkinning result = new MMDModelGpuSkinning();
            result.model = model;
            result.modelDir = modelDir;
            result.vertexCount = vertexCount;
            result.vertexArrayObject = vao;
            result.indexBufferObject = indexVbo;
            result.positionBufferObject = posVbo;
            result.normalBufferObject = norVbo;
            result.uv0BufferObject = uv0Vbo;
            result.boneIndicesBufferObject = boneIdxVbo;
            result.boneWeightsBufferObject = boneWgtVbo;
            result.colorBufferObject = colorVbo;
            result.uv1BufferObject = uv1Vbo;
            result.uv2BufferObject = uv2Vbo;
            result.skinnedPositionsBuffer = outputBuffers[0];
            result.skinnedNormalsBuffer = outputBuffers[1];
            result.boneMatrixSSBO = boneMatrixSSBO;
            result.posBuffer = posBuffer;
            result.norBuffer = norBuffer;
            result.uv0Buffer = uv0Buffer;
            result.colorBuffer = colorBuffer;
            result.uv1Buffer = uv1Buffer;
            result.uv2Buffer = uv2Buffer;
            result.boneMatricesBuffer = boneMatricesBuffer;
            result.boneMatricesByteBuffer = boneMatricesByteBuffer;
            result.indexElementSize = indexElementSize;
            result.indexType = indexType;
            result.mats = mats;
            result.lightMapMaterial = lightMapMaterial;
            result.textureKeys = texKeys;
            result.modelViewMatBuff = modelViewMatBuff;
            result.projMatBuff = projMatBuff;
            result.vertexMorphCount = morphCount;
            if (morphCount > 0) {
                result.morphWeightsBuffer = morphWeightsBuffer;
                result.morphWeightsByteBuffer = ByteBuffer.allocateDirect(morphCount * 4);
                result.morphWeightsByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                result.morphOffsetsSSBO = morphBuffers[0];
                result.morphWeightsSSBO = morphBuffers[1];
            }
            // UV Morph
            result.uvMorphCount = uvMorphCnt;
            result.skinnedUvBuffer = skinnedUvBuf;
            if (uvMorphCnt > 0) {
                result.uvMorphWeightsBuffer = uvMorphWeightsBuf;
                result.uvMorphWeightsByteBuffer = ByteBuffer.allocateDirect(uvMorphCnt * 4);
                result.uvMorphWeightsByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                result.uvMorphOffsetsSSBO = uvMorphBuffers[0];
                result.uvMorphWeightsSSBO = uvMorphBuffers[1];
            }
            // 材质 Morph
            result.materialMorphResultCount = matMorphCount;
            result.materialMorphResultsByteBuffer = matMorphResultsByteBuf;
            result.subMeshCount = (int) nf.GetSubMeshCount(model);
            subMeshDataBufLocal = MemoryUtil.memAlloc(result.subMeshCount * 20);
            subMeshDataBufLocal.order(ByteOrder.LITTLE_ENDIAN);
            result.subMeshDataBuf = subMeshDataBufLocal;
            result.initialized = true;
            
            // 启用自动眨眼
            nf.SetAutoBlinkEnabled(model, true);
            
            GL46C.glBindVertexArray(0);
            return result;
            
        } catch (Exception e) {
            // 异常时清理所有已分配的 GL/内存资源
            // 注意：不清理模型句柄（model），由调用者负责清理，
            // 避免 RenderModeManager 多工厂回退时 use-after-free
            logger.error("GPU 蒙皮模型创建失败，清理资源: {}", e.getMessage());
            
            // 清理 GL 资源
            if (vao > 0) GL46C.glDeleteVertexArrays(vao);
            if (indexVbo > 0) GL46C.glDeleteBuffers(indexVbo);
            if (posVbo > 0) GL46C.glDeleteBuffers(posVbo);
            if (norVbo > 0) GL46C.glDeleteBuffers(norVbo);
            if (uv0Vbo > 0) GL46C.glDeleteBuffers(uv0Vbo);
            if (boneIdxVbo > 0) GL46C.glDeleteBuffers(boneIdxVbo);
            if (boneWgtVbo > 0) GL46C.glDeleteBuffers(boneWgtVbo);
            if (colorVbo > 0) GL46C.glDeleteBuffers(colorVbo);
            if (uv1Vbo > 0) GL46C.glDeleteBuffers(uv1Vbo);
            if (uv2Vbo > 0) GL46C.glDeleteBuffers(uv2Vbo);
            if (outputBuffers != null) {
                GL46C.glDeleteBuffers(outputBuffers[0]);
                GL46C.glDeleteBuffers(outputBuffers[1]);
            }
            if (boneMatrixSSBO > 0) GL46C.glDeleteBuffers(boneMatrixSSBO);
            if (morphBuffers != null) {
                GL46C.glDeleteBuffers(morphBuffers[0]);
                GL46C.glDeleteBuffers(morphBuffers[1]);
            }
            if (uvMorphBuffers != null) {
                GL46C.glDeleteBuffers(uvMorphBuffers[0]);
                GL46C.glDeleteBuffers(uvMorphBuffers[1]);
            }
            if (skinnedUvBuf > 0) GL46C.glDeleteBuffers(skinnedUvBuf);
            if (lightMapMaterial != null && lightMapMaterial.ownsTexture && lightMapMaterial.tex > 0) {
                GL46C.glDeleteTextures(lightMapMaterial.tex);
            }
            
            // 清理 MemoryUtil 分配的缓冲区
            if (boneMatricesBuffer != null) MemoryUtil.memFree(boneMatricesBuffer);
            if (boneMatricesByteBuffer != null) MemoryUtil.memFree(boneMatricesByteBuffer);
            if (modelViewMatBuff != null) MemoryUtil.memFree(modelViewMatBuff);
            if (projMatBuff != null) MemoryUtil.memFree(projMatBuff);
            if (morphWeightsBuffer != null) MemoryUtil.memFree(morphWeightsBuffer);
            if (uvMorphWeightsBuf != null) MemoryUtil.memFree(uvMorphWeightsBuf);
            if (matMorphResultsByteBuf != null) MemoryUtil.memFree(matMorphResultsByteBuf);
            if (subMeshDataBufLocal != null) MemoryUtil.memFree(subMeshDataBufLocal);
            
            return null;
        }
    }
    
    @Override
    protected boolean isReady() {
        return initialized;
    }
    
    @Override
    protected void onUpdate(float deltaTime) {
        getNf().UpdateAnimationOnly(model, deltaTime);
    }
    
    @Override
    protected void doRenderModel(Entity entityIn, float entityYaw, float entityPitch, Vector3f entityTrans, PoseStack deliverStack, int packedLight) {
        Minecraft MCinstance = Minecraft.getInstance();
        renderModelInternal(entityIn, entityYaw, entityPitch, entityTrans, deliverStack, MCinstance);
    }
    
    private void renderModelInternal(Entity entityIn, float entityYaw, float entityPitch, Vector3f entityTrans, PoseStack deliverStack, Minecraft MCinstance) {
        // 光照采样
        LightingHelper.LightData light = LightingHelper.sampleLight(entityIn, MCinstance);
        float lightIntensity = light.intensity();
        int blockLight = light.blockLight();
        int skyLight = light.skyLight();
        float skyDarken = light.skyDarken();
        
        light0Direction.set(1.0f, 0.75f, 0.0f).normalize();
        light1Direction.set(-1.0f, 0.75f, 0.0f).normalize();
        float yawRad = entityYaw * ((float) Math.PI / 180F);
        light0Direction.rotate(tempQuat.identity().rotateY(yawRad));
        light1Direction.rotate(tempQuat.identity().rotateY(yawRad));
        
        // 变换
        deliverStack.mulPose(tempQuat.identity().rotateY(-yawRad));
        deliverStack.mulPose(tempQuat.identity().rotateX(entityPitch * ((float) Math.PI / 180F)));
        deliverStack.translate(entityTrans.x, entityTrans.y, entityTrans.z);
        float baseScale = getModelScale();
        deliverStack.scale(baseScale, baseScale, baseScale);
        
        uploadBoneMatrices();
        if (vertexMorphCount > 0) {
            uploadMorphData();
        }
        if (uvMorphCount > 0) {
            uploadUvMorphData();
        }
        if (materialMorphResultCount > 0) {
            fetchMaterialMorphResults();
        }
        
        // Compute Shader 蒙皮（含 UV Morph）
        computeShader.dispatch(new SkinningComputeShader.DispatchParams(
            positionBufferObject, normalBufferObject,
            boneIndicesBufferObject, boneWeightsBufferObject, uv0BufferObject,
            skinnedPositionsBuffer, skinnedNormalsBuffer, skinnedUvBuffer,
            boneMatrixSSBO,
            morphOffsetsSSBO, morphWeightsSSBO, vertexMorphCount,
            uvMorphOffsetsSSBO, uvMorphWeightsSSBO, uvMorphCount,
            vertexCount
        ));
        
        // G3 优化：批量获取所有子网格元数据（1 次 JNI 替代 ~180 次/帧）
        subMeshDataBuf.clear();
        nf.BatchGetSubMeshData(model, subMeshDataBuf);
        
        boolean useToon = ConfigManager.isToonRenderingEnabled();
        if (useToon) {
            if (toonShaderCpu == null) {
                toonShaderCpu = new ToonShaderCpu();
                if (!toonShaderCpu.init()) {
                    logger.warn("ToonShaderCpu 初始化失败，回退到普通着色");
                    useToon = false;
                }
            }
        }
        
        BufferUploader.reset();
        GL46C.glBindVertexArray(vertexArrayObject);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendEquation(GL46C.GL_FUNC_ADD);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        
        modelViewMatBuff.clear();
        projMatBuff.clear();
        deliverStack.last().pose().get(modelViewMatBuff);
        RenderSystem.getProjectionMatrix().get(projMatBuff);
        
        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject);
        
        currentDeliverStack = deliverStack;
        if (useToon && toonShaderCpu != null && toonShaderCpu.isInitialized()) {
            renderToon(MCinstance, lightIntensity, blockLight, skyLight, skyDarken);
        } else {
            renderNormal(MCinstance, lightIntensity, blockLight, skyLight, skyDarken);
        }
        
        // === 清理 ===
        cleanupVertexAttributes();
        
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, 0);
        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL46C.glBindVertexArray(0);
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        
        ShaderInstance currentShader = RenderSystem.getShader();
        if (currentShader != null) {
            currentShader.clear();
        }
        BufferUploader.reset();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    /**
     * 清理所有启用的顶点属性数组
     */
    private void cleanupVertexAttributes() {
        if (positionLocation != -1) GL46C.glDisableVertexAttribArray(positionLocation);
        if (normalLocation != -1) GL46C.glDisableVertexAttribArray(normalLocation);
        if (uv0Location != -1) GL46C.glDisableVertexAttribArray(uv0Location);
        if (uv1Location != -1) GL46C.glDisableVertexAttribArray(uv1Location);
        if (uv2Location != -1) GL46C.glDisableVertexAttribArray(uv2Location);
        if (colorLocation != -1) GL46C.glDisableVertexAttribArray(colorLocation);
        if (I_positionLocation != -1) GL46C.glDisableVertexAttribArray(I_positionLocation);
        if (I_normalLocation != -1) GL46C.glDisableVertexAttribArray(I_normalLocation);
        if (I_uv0Location != -1) GL46C.glDisableVertexAttribArray(I_uv0Location);
        if (I_uv2Location != -1) GL46C.glDisableVertexAttribArray(I_uv2Location);
        if (I_colorLocation != -1) GL46C.glDisableVertexAttribArray(I_colorLocation);
    }
    
    /**
     * 普通渲染模式（通过 Minecraft 标准 ShaderInstance 管线）
     * 
     * 使用 RenderSystem.getShader() 获取当前着色器，
     * 当 Iris 激活时返回的是 Iris 的 G-buffer 着色器，从而正确写入 MRT。
     */
    private void renderNormal(Minecraft MCinstance, float lightIntensity, int blockLight, int skyLight, float skyDarken) {
        // 获取 Minecraft 当前着色器（Iris 激活时会被替换为 G-buffer 着色器）
        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            logger.error("[GPU蒙皮] RenderSystem.getShader() 返回 null，跳过渲染");
            return;
        }
        shaderProgram = shader.getId();
        
        // 安卓兼容：光照强度通过 ColorModulator uniform 传递（替代 glVertexAttrib4f 常量 Color 属性）
        // 安卓 GL 翻译层（gl4es/ANGLE）对 glVertexAttrib4f 常量属性支持不完整，
        // 导致 Color.a=0 → entity_cutout 着色器 discard → 模型全透明
        boolean irisActive = IrisCompat.isIrisShaderActive();
        float colorFactor = irisActive ? 1.0f : lightIntensity;
        RenderSystem.setShaderColor(colorFactor, colorFactor, colorFactor, 1.0f);
        
        setUniforms(shader, currentDeliverStack);
        shader.apply();
        
        GL46C.glUseProgram(shaderProgram);
        updateLocation(shaderProgram);
        
        // === UV2：填充 VBO 并绑定属性（替代 glVertexAttribI4i 常量属性，安卓兼容）===
        int blockBrightness = 16 * blockLight;
        // Iris 兼容：UV2 不应包含 skyDarken，Iris 的光照管线会自行处理昼夜变化
        int skyBrightness = irisActive ? (16 * skyLight) : Math.round((15.0f - skyDarken) * (skyLight / 15.0f) * 16);
        uv2Buffer.clear();
        for (int i = 0; i < vertexCount; i++) {
            uv2Buffer.putInt(blockBrightness);
            uv2Buffer.putInt(skyBrightness);
        }
        uv2Buffer.flip();
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2BufferObject);
        GL46C.glBufferSubData(GL46C.GL_ARRAY_BUFFER, 0, uv2Buffer);
        if (uv2Location != -1) {
            GL46C.glEnableVertexAttribArray(uv2Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2BufferObject);
            GL46C.glVertexAttribIPointer(uv2Location, 2, GL46C.GL_INT, 0, 0);
        }
        if (I_uv2Location != -1) {
            GL46C.glEnableVertexAttribArray(I_uv2Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2BufferObject);
            GL46C.glVertexAttribIPointer(I_uv2Location, 2, GL46C.GL_INT, 0, 0);
        }
        // === Color：使用白色 VBO + ColorModulator uniform 传递光照（替代 glVertexAttrib4f，安卓兼容）===
        // Color VBO 在创建时填充白色 (1,1,1,1)，光照强度已通过 setShaderColor → ColorModulator 传递
        if (colorLocation != -1) {
            GL46C.glEnableVertexAttribArray(colorLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, colorBufferObject);
            GL46C.glVertexAttribPointer(colorLocation, 4, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (I_colorLocation != -1) {
            GL46C.glEnableVertexAttribArray(I_colorLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, colorBufferObject);
            GL46C.glVertexAttribPointer(I_colorLocation, 4, GL46C.GL_FLOAT, false, 0, 0);
        }
        
        // 绑定顶点属性（标准名称）
        if (positionLocation != -1) {
            GL46C.glEnableVertexAttribArray(positionLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, skinnedPositionsBuffer);
            GL46C.glVertexAttribPointer(positionLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (normalLocation != -1) {
            GL46C.glEnableVertexAttribArray(normalLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, skinnedNormalsBuffer);
            GL46C.glVertexAttribPointer(normalLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        // UV0: 使用 Compute Shader 输出的蒙皮后 UV（含 UV Morph）
        int activeUvBuffer = (skinnedUvBuffer > 0) ? skinnedUvBuffer : uv0BufferObject;
        if (uv0Location != -1) {
            GL46C.glEnableVertexAttribArray(uv0Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, activeUvBuffer);
            GL46C.glVertexAttribPointer(uv0Location, 2, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (uv1Location != -1) {
            GL46C.glEnableVertexAttribArray(uv1Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv1BufferObject);
            GL46C.glVertexAttribIPointer(uv1Location, 2, GL46C.GL_INT, 0, 0);
        }
        
        // 绑定 Iris 重命名属性
        if (I_positionLocation != -1) {
            GL46C.glEnableVertexAttribArray(I_positionLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, skinnedPositionsBuffer);
            GL46C.glVertexAttribPointer(I_positionLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (I_normalLocation != -1) {
            GL46C.glEnableVertexAttribArray(I_normalLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, skinnedNormalsBuffer);
            GL46C.glVertexAttribPointer(I_normalLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (I_uv0Location != -1) {
            GL46C.glEnableVertexAttribArray(I_uv0Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, activeUvBuffer);
            GL46C.glVertexAttribPointer(I_uv0Location, 2, GL46C.GL_FLOAT, false, 0, 0);
        }
        
        drawAllSubMeshes(MCinstance);
    }
    
    /**
     * Toon 渲染模式（使用 ToonShaderCpu，蒙皮后的顶点数据来自 Compute Shader）
     * 
     * Iris 兼容：
     *   Iris 激活时，先通过 ExtendedShader.apply() 绑定 G-buffer FBO + MRT draw buffers，
     *   再切换到 Toon 着色器程序。Toon 片段着色器已声明 layout(location=0..3) 多输出，
     *   确保 Iris 的 draw buffers 全部被写入合理数据，避免透明。
     */
    private void renderToon(Minecraft MCinstance, float lightIntensity, int blockLight, int skyLight, float skyDarken) {
        
        // Iris 兼容：绑定 Iris G-buffer FBO（如果 Iris 光影激活）
        boolean irisActive = IrisCompat.isIrisShaderActive();
        if (irisActive) {
            ShaderInstance irisShader = RenderSystem.getShader();
            if (irisShader != null) {
                setUniforms(irisShader, currentDeliverStack);
                irisShader.apply();  // 绑定 Iris G-buffer FBO + MRT draw buffers
            }
        }
        
        // ===== 第一遍：描边 =====
        if (toonConfig.isOutlineEnabled()) {
            toonShaderCpu.useOutline();
            
            int posLoc = toonShaderCpu.getOutlinePositionLocation();
            int norLoc = toonShaderCpu.getOutlineNormalLocation();
            
            if (posLoc != -1) {
                GL46C.glEnableVertexAttribArray(posLoc);
                GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, skinnedPositionsBuffer);
                GL46C.glVertexAttribPointer(posLoc, 3, GL46C.GL_FLOAT, false, 0, 0);
            }
            if (norLoc != -1) {
                GL46C.glEnableVertexAttribArray(norLoc);
                GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, skinnedNormalsBuffer);
                GL46C.glVertexAttribPointer(norLoc, 3, GL46C.GL_FLOAT, false, 0, 0);
            }
            
            toonShaderCpu.setOutlineProjectionMatrix(projMatBuff);
            toonShaderCpu.setOutlineModelViewMatrix(modelViewMatBuff);
            toonShaderCpu.setOutlineWidth(toonConfig.getOutlineWidth());
            toonShaderCpu.setOutlineColor(
                toonConfig.getOutlineColorR(),
                toonConfig.getOutlineColorG(),
                toonConfig.getOutlineColorB()
            );
            
            GL46C.glCullFace(GL46C.GL_FRONT);
            RenderSystem.enableCull();
            
            // 绘制描边（G3 优化：从 subMeshDataBuf 读取）
            for (int i = 0; i < subMeshCount; ++i) {
                int base = i * 20;
                int materialID = subMeshDataBuf.getInt(base);
                int beginIndex = subMeshDataBuf.getInt(base + 4);
                int count      = subMeshDataBuf.getInt(base + 8);
                float edgeAlpha= subMeshDataBuf.getFloat(base + 12);
                boolean visible= subMeshDataBuf.get(base + 16) != 0;
                
                if (!visible) continue;
                if (getEffectiveMaterialAlpha(materialID, edgeAlpha) < 0.001f) continue;
                
                long startPos = (long) beginIndex * indexElementSize;
                GL46C.glDrawElements(GL46C.GL_TRIANGLES, count, indexType, startPos);
            }
            
            // 恢复背面剔除
            GL46C.glCullFace(GL46C.GL_BACK);
            
            // 禁用描边着色器的顶点属性
            if (posLoc != -1) GL46C.glDisableVertexAttribArray(posLoc);
            if (norLoc != -1) GL46C.glDisableVertexAttribArray(norLoc);
        }
        
        // ===== 第二遍：主体（Toon 着色） =====
        toonShaderCpu.useMain();
        
        int toonPosLoc = toonShaderCpu.getPositionLocation();
        int toonNorLoc = toonShaderCpu.getNormalLocation();
        int uvLoc = toonShaderCpu.getUv0Location();
        
        if (toonPosLoc != -1) {
            GL46C.glEnableVertexAttribArray(toonPosLoc);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, skinnedPositionsBuffer);
            GL46C.glVertexAttribPointer(toonPosLoc, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (toonNorLoc != -1) {
            GL46C.glEnableVertexAttribArray(toonNorLoc);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, skinnedNormalsBuffer);
            GL46C.glVertexAttribPointer(toonNorLoc, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (uvLoc != -1) {
            GL46C.glEnableVertexAttribArray(uvLoc);
            int toonUvBuffer = (skinnedUvBuffer > 0) ? skinnedUvBuffer : uv0BufferObject;
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, toonUvBuffer);
            GL46C.glVertexAttribPointer(uvLoc, 2, GL46C.GL_FLOAT, false, 0, 0);
        }
        
        toonShaderCpu.setProjectionMatrix(projMatBuff);
        toonShaderCpu.setModelViewMatrix(modelViewMatBuff);
        toonShaderCpu.setSampler0(0);
        toonShaderCpu.setLightIntensity(lightIntensity);
        toonShaderCpu.setToonLevels(toonConfig.getToonLevels());
        toonShaderCpu.setRimLight(toonConfig.getRimPower(), toonConfig.getRimIntensity());
        toonShaderCpu.setShadowColor(
            toonConfig.getShadowColorR(),
            toonConfig.getShadowColorG(),
            toonConfig.getShadowColorB()
        );
        toonShaderCpu.setSpecular(toonConfig.getSpecularPower(), toonConfig.getSpecularIntensity());
        
        drawAllSubMeshes(MCinstance);
        
        if (toonPosLoc != -1) GL46C.glDisableVertexAttribArray(toonPosLoc);
        if (toonNorLoc != -1) GL46C.glDisableVertexAttribArray(toonNorLoc);
        if (uvLoc != -1) GL46C.glDisableVertexAttribArray(uvLoc);
        
        GL46C.glUseProgram(0);
    }
    
    /**
     * 绘制所有子网格
     */
    private void drawAllSubMeshes(Minecraft MCinstance) {
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        
        // G3 优化：从预填充的 subMeshDataBuf 读取元数据（0 次 JNI 调用）
        for (int i = 0; i < subMeshCount; ++i) {
            int base = i * 20;
            int materialID  = subMeshDataBuf.getInt(base);
            int beginIndex  = subMeshDataBuf.getInt(base + 4);
            int vertCount   = subMeshDataBuf.getInt(base + 8);
            float alpha     = subMeshDataBuf.getFloat(base + 12);
            boolean visible = subMeshDataBuf.get(base + 16) != 0;
            boolean bothFace= subMeshDataBuf.get(base + 17) != 0;
            
            if (!visible) continue;
            if (getEffectiveMaterialAlpha(materialID, alpha) < 0.001f) continue;
            
            if (bothFace) {
                RenderSystem.disableCull();
            } else {
                RenderSystem.enableCull();
            }
            
            int texId;
            if (mats[materialID].tex == 0) {
                texId = MCinstance.getTextureManager().getTexture(TextureManager.INTENTIONAL_MISSING_TEXTURE).getId();
            } else {
                texId = mats[materialID].tex;
            }
            RenderSystem.setShaderTexture(0, texId);
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, texId);
            
            long startPos = (long) beginIndex * indexElementSize;
            GL46C.glDrawElements(GL46C.GL_TRIANGLES, vertCount, indexType, startPos);
        }
    }
    
    /**
     * 上传骨骼矩阵到 Compute Shader 的 SSBO
     */
    private void uploadBoneMatrices() {
        boneMatricesByteBuffer.clear();
        
        int copiedBones = nf.CopySkinningMatricesToBuffer(model, boneMatricesByteBuffer);
        if (copiedBones == 0) return;
        
        boneMatricesBuffer.clear();
        boneMatricesByteBuffer.position(0);
        // G2 优化：批量拷贝替代逐 float 循环（消除 copiedBones*16 次迭代）
        FloatBuffer floatView = boneMatricesByteBuffer.asFloatBuffer();
        floatView.limit(copiedBones * 16);
        boneMatricesBuffer.put(floatView);
        boneMatricesBuffer.flip();
        
        computeShader.uploadBoneMatrices(boneMatrixSSBO, boneMatricesBuffer, copiedBones);
    }
    
    /**
     * 上传 Morph 数据到 Compute Shader 的 SSBO
     */
    private void uploadMorphData() {
        if (vertexMorphCount <= 0) return;
        
        // 首次上传偏移数据（静态）
        if (!morphDataUploaded) {
            long offsetsSize = nf.GetGpuMorphOffsetsSize(model);
            if (offsetsSize > 0) {
                // 边界检查：避免 long 截断为负数导致 memAlloc 异常
                if (offsetsSize > Integer.MAX_VALUE) {
                    logger.error("Morph 数据过大 ({} bytes)，超过 2GB 限制，跳过 GPU Morph", offsetsSize);
                    vertexMorphCount = 0; // 禁用 Morph 以避免后续错误
                } else {
                    // 使用 MemoryUtil.memAlloc 分配原生内存，避免 Java 直接内存池 OOM
                    ByteBuffer offsetsBuffer = MemoryUtil.memAlloc((int) offsetsSize);
                    offsetsBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    try {
                        nf.CopyGpuMorphOffsetsToBuffer(model, offsetsBuffer);
                        computeShader.uploadMorphOffsets(morphOffsetsSSBO, offsetsBuffer);
                        morphDataUploaded = true;
                    } finally {
                        MemoryUtil.memFree(offsetsBuffer);
                    }
                }
            }
        }
        
        // 每帧更新权重（复用预分配缓冲区）
        if (morphWeightsBuffer != null && morphWeightsByteBuffer != null) {
            morphWeightsByteBuffer.clear();
            nf.CopyGpuMorphWeightsToBuffer(model, morphWeightsByteBuffer);
            morphWeightsBuffer.clear();
            morphWeightsByteBuffer.position(0);
            morphWeightsBuffer.put(morphWeightsByteBuffer.asFloatBuffer());
            morphWeightsBuffer.flip();
            computeShader.updateMorphWeights(morphWeightsSSBO, morphWeightsBuffer);
        }
    }
    
    /**
     * 上传 UV Morph 数据到 Compute Shader 的 SSBO
     */
    private void uploadUvMorphData() {
        if (uvMorphCount <= 0) return;
        
        // 首次上传偏移数据（静态）
        if (!uvMorphDataUploaded) {
            long offsetsSize = nf.GetGpuUvMorphOffsetsSize(model);
            if (offsetsSize > 0 && offsetsSize <= Integer.MAX_VALUE) {
                ByteBuffer offsetsBuffer = MemoryUtil.memAlloc((int) offsetsSize);
                offsetsBuffer.order(ByteOrder.LITTLE_ENDIAN);
                try {
                    nf.CopyGpuUvMorphOffsetsToBuffer(model, offsetsBuffer);
                    computeShader.uploadUvMorphOffsets(uvMorphOffsetsSSBO, offsetsBuffer);
                    uvMorphDataUploaded = true;
                } finally {
                    MemoryUtil.memFree(offsetsBuffer);
                }
            }
        }
        
        // 每帧更新权重
        if (uvMorphWeightsBuffer != null && uvMorphWeightsByteBuffer != null) {
            uvMorphWeightsByteBuffer.clear();
            nf.CopyGpuUvMorphWeightsToBuffer(model, uvMorphWeightsByteBuffer);
            uvMorphWeightsBuffer.clear();
            uvMorphWeightsByteBuffer.position(0);
            uvMorphWeightsBuffer.put(uvMorphWeightsByteBuffer.asFloatBuffer());
            uvMorphWeightsBuffer.flip();
            computeShader.updateUvMorphWeights(uvMorphWeightsSSBO, uvMorphWeightsBuffer);
        }
    }
    
    
    /**
     * 更新着色器属性位置（基于当前绑定的着色器程序）
     * 支持 Minecraft 标准属性和 Iris 重命名属性
     */
    private void updateLocation(int program) {
        // G1 优化：着色器程序未变时跳过 11 次 glGetAttribLocation 查询
        if (program == cachedShaderProgram) return;
        cachedShaderProgram = program;
        
        positionLocation = GlStateManager._glGetAttribLocation(program, "Position");
        normalLocation = GlStateManager._glGetAttribLocation(program, "Normal");
        uv0Location = GlStateManager._glGetAttribLocation(program, "UV0");
        uv1Location = GlStateManager._glGetAttribLocation(program, "UV1");
        uv2Location = GlStateManager._glGetAttribLocation(program, "UV2");
        colorLocation = GlStateManager._glGetAttribLocation(program, "Color");
        
        // Iris 重命名属性（Iris 会将标准属性名加上 "iris_" 前缀）
        I_positionLocation = GlStateManager._glGetAttribLocation(program, "iris_Position");
        I_normalLocation = GlStateManager._glGetAttribLocation(program, "iris_Normal");
        I_uv0Location = GlStateManager._glGetAttribLocation(program, "iris_UV0");
        I_uv2Location = GlStateManager._glGetAttribLocation(program, "iris_UV2");
        I_colorLocation = GlStateManager._glGetAttribLocation(program, "iris_Color");
    }
    
    private void setUniforms(ShaderInstance shader, PoseStack deliverStack) {
        setupShaderUniforms(shader, deliverStack, light0Direction, light1Direction, lightMapMaterial.tex);
    }
    
    @Override
    public long getVramUsage() {
        if (!initialized) return 0;
        NativeFunc nf = getNf();
        long total = 0;
        int indexCount = (int) nf.GetIndexCount(model);
        // IBO
        total += (long) indexCount * indexElementSize;
        // pos + normal + uv0 VBO（静态输入）
        total += (long) vertexCount * 12 * 2;
        total += (long) vertexCount * 8;
        // boneIdx + boneWgt VBO
        total += (long) vertexCount * 16 * 2;
        // color + uv1 + uv2 VBO
        total += (long) vertexCount * 16;
        total += (long) vertexCount * 8 * 2;
        // Compute Shader 输出 SSBO（skinned pos + skinned nor）
        total += (long) vertexCount * 12 * 2;
        // Bone matrix SSBO（固定分配 MAX_BONES 大小）
        total += (long) ShaderConstants.MAX_BONES * 64;
        // Morph SSBOs（使用 JNI 精确查询实际分配大小）
        if (vertexMorphCount > 0) {
            total += nf.GetGpuMorphOffsetsSize(model); // offsets
            total += (long) vertexMorphCount * 4;       // weights
        }
        // UV Morph SSBOs
        if (uvMorphCount > 0) {
            total += nf.GetGpuUvMorphOffsetsSize(model); // offsets
            total += (long) uvMorphCount * 4;             // weights
        }
        // Skinned UV buffer（无论是否有 UV Morph 都会分配）
        if (skinnedUvBuffer > 0) {
            total += (long) vertexCount * 8;
        }
        return total;
    }
    
    @Override
    public long getRamUsage() {
        if (!initialized) return 0;
        long rustRam = getNf().GetModelMemoryUsage(model);
        // Java 侧堆外内存：6 个逐顶点 ByteBuffer
        long javaRam = (long) vertexCount * 64; // pos(12)+nor(12)+uv0(8)+color(16)+uv1(8)+uv2(8)
        // MemoryUtil 预分配缓冲区
        javaRam += 128; // modelViewMat(64)+projMat(64)
        // 骨骼矩阵缓冲区（FloatBuffer + ByteBuffer）
        if (boneMatricesBuffer != null) {
            javaRam += (long) boneMatricesBuffer.capacity() * 4;
        }
        if (boneMatricesByteBuffer != null) {
            javaRam += boneMatricesByteBuffer.capacity();
        }
        // Morph 权重缓冲区
        if (morphWeightsBuffer != null) {
            javaRam += (long) morphWeightsBuffer.capacity() * 4;
        }
        if (morphWeightsByteBuffer != null) {
            javaRam += morphWeightsByteBuffer.capacity();
        }
        // UV Morph 权重缓冲区
        if (uvMorphWeightsBuffer != null) {
            javaRam += (long) uvMorphWeightsBuffer.capacity() * 4;
        }
        if (uvMorphWeightsByteBuffer != null) {
            javaRam += uvMorphWeightsByteBuffer.capacity();
        }
        // 材质 Morph 缓冲区
        if (materialMorphResultCount > 0) {
            javaRam += (long) materialMorphResultCount * 56 * 4 * 2;
        }
        // 子网格元数据缓冲区
        if (subMeshDataBuf != null) {
            javaRam += subMeshDataBuf.capacity();
        }
        return rustRam + javaRam;
    }
    
    @Override
    public void dispose() {
        if (!initialized) return;
        initialized = false;
        releaseTextures();
        disposeModelHandle();
        
        // 释放 OpenGL 资源
        GL46C.glDeleteVertexArrays(vertexArrayObject);
        GL46C.glDeleteBuffers(indexBufferObject);
        GL46C.glDeleteBuffers(positionBufferObject);
        GL46C.glDeleteBuffers(normalBufferObject);
        GL46C.glDeleteBuffers(uv0BufferObject);
        GL46C.glDeleteBuffers(boneIndicesBufferObject);
        GL46C.glDeleteBuffers(boneWeightsBufferObject);
        GL46C.glDeleteBuffers(colorBufferObject);
        GL46C.glDeleteBuffers(uv1BufferObject);
        GL46C.glDeleteBuffers(uv2BufferObject);
        GL46C.glDeleteBuffers(skinnedPositionsBuffer);
        GL46C.glDeleteBuffers(skinnedNormalsBuffer);
        
        // 释放每实例 SSBO
        if (boneMatrixSSBO > 0) GL46C.glDeleteBuffers(boneMatrixSSBO);
        if (morphOffsetsSSBO > 0) GL46C.glDeleteBuffers(morphOffsetsSSBO);
        if (morphWeightsSSBO > 0) GL46C.glDeleteBuffers(morphWeightsSSBO);
        if (uvMorphOffsetsSSBO > 0) GL46C.glDeleteBuffers(uvMorphOffsetsSSBO);
        if (uvMorphWeightsSSBO > 0) GL46C.glDeleteBuffers(uvMorphWeightsSSBO);
        if (skinnedUvBuffer > 0) GL46C.glDeleteBuffers(skinnedUvBuffer);
        boneMatrixSSBO = 0; morphOffsetsSSBO = 0; morphWeightsSSBO = 0;
        uvMorphOffsetsSSBO = 0; uvMorphWeightsSSBO = 0; skinnedUvBuffer = 0;
        
        // 释放自建的 lightMap 纹理
        if (lightMapMaterial != null && lightMapMaterial.ownsTexture && lightMapMaterial.tex > 0) {
            GL46C.glDeleteTextures(lightMapMaterial.tex);
            lightMapMaterial.tex = 0;
        }
        
        // 释放 MemoryUtil 分配的缓冲区
        disposeMaterialMorphBuffers();
        if (boneMatricesBuffer != null) { MemoryUtil.memFree(boneMatricesBuffer); boneMatricesBuffer = null; }
        if (boneMatricesByteBuffer != null) { MemoryUtil.memFree(boneMatricesByteBuffer); boneMatricesByteBuffer = null; }
        if (morphWeightsBuffer != null) { MemoryUtil.memFree(morphWeightsBuffer); morphWeightsBuffer = null; }
        morphWeightsByteBuffer = null;
        if (uvMorphWeightsBuffer != null) { MemoryUtil.memFree(uvMorphWeightsBuffer); uvMorphWeightsBuffer = null; }
        uvMorphWeightsByteBuffer = null;
        if (modelViewMatBuff != null) { MemoryUtil.memFree(modelViewMatBuff); modelViewMatBuff = null; }
        if (projMatBuff != null) { MemoryUtil.memFree(projMatBuff); projMatBuff = null; }
        if (subMeshDataBuf != null) { MemoryUtil.memFree(subMeshDataBuf); subMeshDataBuf = null; }
    }
    
    /** @deprecated 使用 {@link #dispose()} 替代 */
    @Deprecated
    public static void Delete(MMDModelGpuSkinning model) {
        if (model != null) model.dispose();
    }
    
}
