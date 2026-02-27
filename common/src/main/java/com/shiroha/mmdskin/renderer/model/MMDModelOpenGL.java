package com.shiroha.mmdskin.renderer.model;

import com.shiroha.mmdskin.MmdSkinClient;
import com.shiroha.mmdskin.config.ConfigManager;
import com.shiroha.mmdskin.renderer.core.IrisCompat;
import com.shiroha.mmdskin.renderer.resource.MMDTextureManager;
import com.shiroha.mmdskin.renderer.shader.ShaderProvider;
import com.shiroha.mmdskin.renderer.shader.ToonShaderCpu;
import com.shiroha.mmdskin.renderer.shader.ToonConfig;
import com.shiroha.mmdskin.NativeFunc;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

/**
 * MMD模型OpenGL渲染实现
 * 负责MMD模型的OpenGL渲染逻辑
 */
public class MMDModelOpenGL extends AbstractMMDModel {
    static boolean isShaderInited = false;
    static int MMDShaderProgram;
    public static boolean isMMDShaderEnabled = false;
    private static ToonShaderCpu toonShaderCpu;
    private static final ToonConfig toonConfig = ToonConfig.getInstance();
    int shaderProgram;

    int positionLocation;
    int normalLocation;
    int uv0Location, uv1Location, uv2Location;
    int colorLocation;
    int projMatLocation;
    int modelViewLocation;
    int sampler0Location, sampler1Location, sampler2Location;
    int light0Location, light1Location;

    int K_positionLocation;
    int K_normalLocation;
    int K_uv0Location, K_uv2Location;
    int K_projMatLocation;
    int K_modelViewLocation;
    int K_sampler0Location, K_sampler2Location;
    int KAIMyLocationV;
    int KAIMyLocationF;

    int I_positionLocation;
    int I_normalLocation;
    int I_uv0Location, I_uv2Location;
    int I_colorLocation;

    int vertexCount;
    ByteBuffer posBuffer, colorBuffer, norBuffer, uv0Buffer, uv1Buffer, uv2Buffer;
    int vertexArrayObject;
    int indexBufferObject;
    int vertexBufferObject;
    int colorBufferObject;
    int normalBufferObject;
    int texcoordBufferObject;
    int uv1BufferObject;
    int uv2BufferObject;
    int indexElementSize;
    int indexType;
    MMDMaterial[] mats;
    MMDMaterial lightMapMaterial;
    final Vector3f light0Direction = new Vector3f();
    final Vector3f light1Direction = new Vector3f();
    
    private FloatBuffer modelViewMatBuff;          // 预分配的矩阵缓冲区
    private FloatBuffer projMatBuff;
    private FloatBuffer light0Buff;                  // 预分配的光照缓冲区
    private FloatBuffer light1Buff;

    // 性能优化：缓存着色器程序ID，避免每帧重复查询属性位置
    private int cachedShaderProgram = -1;
    // 性能优化：标记是否有 UV Morph，无则跳过每帧 UV 重传
    private boolean hasUvMorph = false;
    // 性能优化：缓存子网格数量 + 批量元数据缓冲区（消除逐子网格 JNI 调用）
    private int subMeshCount;
    private ByteBuffer subMeshDataBuf;

    MMDModelOpenGL() {
    }

    public static void InitShader() {
        //Init Shader
        ShaderProvider.Init();
        if (ShaderProvider.isReady()) {
            MMDShaderProgram = ShaderProvider.getProgram();
        } else {
            logger.warn("MMD Shader 初始化失败，已自动禁用自定义着色器");
            MMDShaderProgram = 0;
            isMMDShaderEnabled = false;
        }
        isShaderInited = true;
    }

    public static MMDModelOpenGL Create(String modelFilename, String modelDir, boolean isPMD, long layerCount) {
        if (!isShaderInited && isMMDShaderEnabled)
            InitShader();
        NativeFunc nf = getNf();
        long model;
        if (isPMD)
            model = nf.LoadModelPMD(modelFilename, modelDir, layerCount);
        else
            model = nf.LoadModelPMX(modelFilename, modelDir, layerCount);
        if (model == 0) {
            logger.warn(String.format("Cannot open model: '%s'.", modelFilename));
            return null;
        }
        MMDModelOpenGL result = createFromHandle(model, modelDir);
        if (result == null) {
            nf.DeleteModel(model);
        }
        return result;
    }
    
    /**
     * 从已加载的模型句柄创建渲染实例（Phase 2：GL 资源创建，必须在渲染线程调用）
     * Phase 1（nf.LoadModelPMX/PMD）已在后台线程完成
     */
    public static MMDModelOpenGL createFromHandle(long model, String modelDir) {
        if (!isShaderInited && isMMDShaderEnabled)
            InitShader();
        NativeFunc nf = getNf();
        BufferUploader.reset();
        
        // 资源追踪变量（用于异常时清理）
        int vertexArrayObject = 0, indexBufferObject = 0;
        int positionBufferObject = 0, colorBufferObject = 0, normalBufferObject = 0;
        int uv0BufferObject = 0, uv1BufferObject = 0, uv2BufferObject = 0;
        MMDMaterial lightMapMaterial = null;
        FloatBuffer modelViewMatBuff = null, projMatBuff = null;
        FloatBuffer light0Buff = null, light1Buff = null;
        ByteBuffer matMorphResultsByteBuf = null;
        
        try {
            vertexArrayObject = GL46C.glGenVertexArrays();
            indexBufferObject = GL46C.glGenBuffers();
            positionBufferObject = GL46C.glGenBuffers();
            colorBufferObject = GL46C.glGenBuffers();
            normalBufferObject = GL46C.glGenBuffers();
            uv0BufferObject = GL46C.glGenBuffers();
            uv1BufferObject = GL46C.glGenBuffers();
            uv2BufferObject = GL46C.glGenBuffers();

            int vertexCount = (int) nf.GetVertexCount(model);
            ByteBuffer posBuffer = MemoryUtil.memAlloc(vertexCount * 12); //float * 3
            ByteBuffer colorBuffer = MemoryUtil.memAlloc(vertexCount * 16); //float * 4
            ByteBuffer norBuffer = MemoryUtil.memAlloc(vertexCount * 12); //float * 3
            ByteBuffer uv0Buffer = MemoryUtil.memAlloc(vertexCount * 8); //float * 2
            ByteBuffer uv1Buffer = MemoryUtil.memAlloc(vertexCount * 8); //int * 2
            ByteBuffer uv2Buffer = MemoryUtil.memAlloc(vertexCount * 8); //int * 2
            colorBuffer.order(ByteOrder.LITTLE_ENDIAN);
            uv1Buffer.order(ByteOrder.LITTLE_ENDIAN);
            uv2Buffer.order(ByteOrder.LITTLE_ENDIAN);

            GL46C.glBindVertexArray(vertexArrayObject);
            //Init indexBufferObject
            int indexElementSize = (int) nf.GetIndexElementSize(model);
            int indexCount = (int) nf.GetIndexCount(model);
            int indexSize = indexCount * indexElementSize;
            long indexData = nf.GetIndices(model);
            ByteBuffer indexBuffer = MemoryUtil.memAlloc(indexSize);
            nf.CopyDataToByteBuffer(indexBuffer, indexData, indexSize);
            indexBuffer.position(0);
            GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject);
            GL46C.glBufferData(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL46C.GL_STATIC_DRAW);
            MemoryUtil.memFree(indexBuffer); // 一次性上传后立即释放

            int indexType = switch (indexElementSize) {
                case 1 -> GL46C.GL_UNSIGNED_BYTE;
                case 2 -> GL46C.GL_UNSIGNED_SHORT;
                case 4 -> GL46C.GL_UNSIGNED_INT;
                default -> 0;
            };

            //Material（记录纹理引用键）
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

            //lightMap
            lightMapMaterial = new MMDMaterial();
            String lightMapPath = modelDir + "/lightMap.png";
            MMDTextureManager.Texture mgrTex = MMDTextureManager.GetTexture(lightMapPath);
            if (mgrTex != null) {
                lightMapMaterial.tex = mgrTex.tex;
                lightMapMaterial.hasAlpha = mgrTex.hasAlpha;
                MMDTextureManager.addRef(lightMapPath);
                texKeys.add(lightMapPath);
            }else{
                lightMapMaterial.tex = GL46C.glGenTextures();
                lightMapMaterial.ownsTexture = true;
                GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, lightMapMaterial.tex);
                ByteBuffer texBuffer = ByteBuffer.allocateDirect(16*16*4);
                texBuffer.order(ByteOrder.LITTLE_ENDIAN);
                for(int i=0;i<16*16;i++){
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

            for(int i=0; i<vertexCount; i++){
                colorBuffer.putFloat(1.0f);
                colorBuffer.putFloat(1.0f);
                colorBuffer.putFloat(1.0f);
                colorBuffer.putFloat(1.0f);
            }
            colorBuffer.flip();

            for(int i=0; i<vertexCount; i++){
                uv1Buffer.putInt(15);
                uv1Buffer.putInt(15);
            }
            uv1Buffer.flip();

            // 性能优化：预分配动态 VBO 大小（后续使用 glBufferSubData 仅更新数据，避免每帧重分配 GPU 内存）
            int posAndNorSize = vertexCount * 12;
            int uv0Size = vertexCount * 8;
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, positionBufferObject);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, posAndNorSize, GL46C.GL_DYNAMIC_DRAW);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, posAndNorSize, GL46C.GL_DYNAMIC_DRAW);
            // UV0：加载初始数据并上传；无 UV Morph 时作为静态数据，有 UV Morph 时每帧更新
            long uv0Data = nf.GetUVs(model);
            nf.CopyDataToByteBuffer(uv0Buffer, uv0Data, uv0Size);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv0BufferObject);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv0Buffer, GL46C.GL_DYNAMIC_DRAW);
            
            // 性能优化：uv1 是静态数据（永远是 {15, 15}），只在创建时上传一次
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv1BufferObject);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv1Buffer, GL46C.GL_STATIC_DRAW);
            // 安卓兼容：上传白色 Color VBO + 预分配 UV2 VBO
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, colorBufferObject);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, colorBuffer, GL46C.GL_STATIC_DRAW);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2BufferObject);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, vertexCount * 8, GL46C.GL_DYNAMIC_DRAW);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, 0);

            MMDModelOpenGL result = new MMDModelOpenGL();
            result.model = model;
            result.modelDir = modelDir;
            result.vertexCount = vertexCount;
            result.posBuffer = posBuffer;
            result.colorBuffer = colorBuffer;
            result.norBuffer = norBuffer;
            result.uv0Buffer = uv0Buffer;
            result.uv1Buffer = uv1Buffer;
            result.uv2Buffer = uv2Buffer;
            result.indexBufferObject = indexBufferObject;
            result.vertexBufferObject = positionBufferObject;
            result.colorBufferObject = colorBufferObject;
            result.texcoordBufferObject = uv0BufferObject;
            result.uv1BufferObject = uv1BufferObject;
            result.uv2BufferObject = uv2BufferObject;
            result.normalBufferObject = normalBufferObject;
            result.vertexArrayObject = vertexArrayObject;
            result.indexElementSize = indexElementSize;
            result.indexType = indexType;
            result.mats = mats;
            result.lightMapMaterial = lightMapMaterial;
            result.hasUvMorph = nf.GetUvMorphCount(model) > 0;
            result.textureKeys = texKeys;
            
            // 预分配矩阵缓冲区（避免每帧分配）
            modelViewMatBuff = MemoryUtil.memAllocFloat(16);
            projMatBuff = MemoryUtil.memAllocFloat(16);
            light0Buff = MemoryUtil.memAllocFloat(3);
            light1Buff = MemoryUtil.memAllocFloat(3);
            result.modelViewMatBuff = modelViewMatBuff;
            result.projMatBuff = projMatBuff;
            result.light0Buff = light0Buff;
            result.light1Buff = light1Buff;
            
            // 初始化材质 Morph 结果缓冲区
            int matMorphCount = nf.GetMaterialMorphResultCount(model);
            if (matMorphCount > 0) {
                int floatCount = matMorphCount * 56;
                result.materialMorphResultCount = matMorphCount;
                matMorphResultsByteBuf = MemoryUtil.memAlloc(floatCount * 4);
                matMorphResultsByteBuf.order(ByteOrder.LITTLE_ENDIAN);
                result.materialMorphResultsByteBuffer = matMorphResultsByteBuf;
            }
            
            // 初始化子网格批量元数据缓冲区（消除逐子网格 JNI 调用）
            result.subMeshCount = (int) nf.GetSubMeshCount(model);
            result.subMeshDataBuf = MemoryUtil.memAlloc(result.subMeshCount * 20);
            result.subMeshDataBuf.order(ByteOrder.LITTLE_ENDIAN);
            
            // 启用自动眨眼
            nf.SetAutoBlinkEnabled(model, true);
            
            return result;
            
        } catch (Exception e) {
            // 异常时清理所有已分配的 GL/内存资源
            // 注意：不清理模型句柄（model），由调用者负责清理，
            // 避免 RenderModeManager 多工厂回退时 use-after-free
            logger.error("CPU 蒙皮模型创建失败，清理资源: {}", e.getMessage());
            
            if (vertexArrayObject > 0) GL46C.glDeleteVertexArrays(vertexArrayObject);
            if (indexBufferObject > 0) GL46C.glDeleteBuffers(indexBufferObject);
            if (positionBufferObject > 0) GL46C.glDeleteBuffers(positionBufferObject);
            if (colorBufferObject > 0) GL46C.glDeleteBuffers(colorBufferObject);
            if (normalBufferObject > 0) GL46C.glDeleteBuffers(normalBufferObject);
            if (uv0BufferObject > 0) GL46C.glDeleteBuffers(uv0BufferObject);
            if (uv1BufferObject > 0) GL46C.glDeleteBuffers(uv1BufferObject);
            if (uv2BufferObject > 0) GL46C.glDeleteBuffers(uv2BufferObject);
            if (lightMapMaterial != null && lightMapMaterial.ownsTexture && lightMapMaterial.tex > 0) {
                GL46C.glDeleteTextures(lightMapMaterial.tex);
            }
            
            if (modelViewMatBuff != null) MemoryUtil.memFree(modelViewMatBuff);
            if (projMatBuff != null) MemoryUtil.memFree(projMatBuff);
            if (light0Buff != null) MemoryUtil.memFree(light0Buff);
            if (light1Buff != null) MemoryUtil.memFree(light1Buff);
            if (matMorphResultsByteBuf != null) MemoryUtil.memFree(matMorphResultsByteBuf);
            
            return null;
        }
    }

    @Override
    public void dispose() {
        releaseTextures();
        disposeModelHandle();
        
        // 释放 MemoryUtil 分配的逐帧 ByteBuffer
        if (posBuffer != null) { MemoryUtil.memFree(posBuffer); posBuffer = null; }
        if (colorBuffer != null) { MemoryUtil.memFree(colorBuffer); colorBuffer = null; }
        if (norBuffer != null) { MemoryUtil.memFree(norBuffer); norBuffer = null; }
        if (uv0Buffer != null) { MemoryUtil.memFree(uv0Buffer); uv0Buffer = null; }
        if (uv1Buffer != null) { MemoryUtil.memFree(uv1Buffer); uv1Buffer = null; }
        if (uv2Buffer != null) { MemoryUtil.memFree(uv2Buffer); uv2Buffer = null; }
        
        // 释放预分配的矩阵缓冲区
        if (modelViewMatBuff != null) { MemoryUtil.memFree(modelViewMatBuff); modelViewMatBuff = null; }
        if (projMatBuff != null) { MemoryUtil.memFree(projMatBuff); projMatBuff = null; }
        if (light0Buff != null) { MemoryUtil.memFree(light0Buff); light0Buff = null; }
        if (light1Buff != null) { MemoryUtil.memFree(light1Buff); light1Buff = null; }
        if (subMeshDataBuf != null) { MemoryUtil.memFree(subMeshDataBuf); subMeshDataBuf = null; }
        disposeMaterialMorphBuffers();
        
        // 释放自建的 lightMap 纹理（来自 MMDTextureManager 的不在此删除）
        if (lightMapMaterial != null && lightMapMaterial.ownsTexture && lightMapMaterial.tex > 0) {
            GL46C.glDeleteTextures(lightMapMaterial.tex);
            lightMapMaterial.tex = 0;
        }
        
        // 删除 OpenGL 资源
        GL46C.glDeleteVertexArrays(vertexArrayObject);
        GL46C.glDeleteBuffers(indexBufferObject);
        GL46C.glDeleteBuffers(vertexBufferObject);
        GL46C.glDeleteBuffers(colorBufferObject);
        GL46C.glDeleteBuffers(normalBufferObject);
        GL46C.glDeleteBuffers(texcoordBufferObject);
        GL46C.glDeleteBuffers(uv1BufferObject);
        GL46C.glDeleteBuffers(uv2BufferObject);
    }

    @Override
    public long getVramUsage() {
        long total = 0;
        // IBO
        int indexCount = (int) getNf().GetIndexCount(model);
        total += (long) indexCount * indexElementSize;
        // pos + normal VBO (dynamic)
        total += (long) vertexCount * 12 * 2;
        // color VBO (static)
        total += (long) vertexCount * 16;
        // uv0 + uv1 + uv2 VBO
        total += (long) vertexCount * 8 * 3;
        return total;
    }
    
    @Override
    public long getRamUsage() {
        if (model == 0) return 0;
        long rustRam = getNf().GetModelMemoryUsage(model);
        // Java 侧堆外内存：6 个逐顶点 ByteBuffer
        long javaRam = (long) vertexCount * 64; // pos(12)+color(16)+nor(12)+uv0(8)+uv1(8)+uv2(8)
        // MemoryUtil 预分配缓冲区
        javaRam += 152; // modelViewMat(64)+projMat(64)+light0(12)+light1(12)
        // 材质 Morph 缓冲区
        if (materialMorphResultCount > 0) {
            javaRam += (long) materialMorphResultCount * 56 * 4 * 2;
        }
        return rustRam + javaRam;
    }
    
    @Override
    protected void onUpdate(float deltaTime) {
        getNf().UpdateModel(model, deltaTime);
    }

    @Override
    protected void doRenderModel(Entity entityIn, float entityYaw, float entityPitch, Vector3f entityTrans, PoseStack deliverStack, int packedLight) {
        Minecraft MCinstance = Minecraft.getInstance();
        LightingHelper.LightData light = LightingHelper.sampleLight(entityIn, MCinstance);
        float lightIntensity = light.intensity();
        int blockLight = light.blockLight();
        int skyLight = light.skyLight();
        float skyDarken = light.skyDarken();
        
        light0Direction.set(1.0f, 0.75f, 0.0f).normalize();
        light1Direction.set(-1.0f, 0.75f, 0.0f).normalize();
        float yawRad = entityYaw * ((float)Math.PI / 180F);
        light0Direction.rotate(tempQuat.identity().rotateY(yawRad));
        light1Direction.rotate(tempQuat.identity().rotateY(yawRad));

        deliverStack.mulPose(tempQuat.identity().rotateY(-yawRad));
        deliverStack.mulPose(tempQuat.identity().rotateX(entityPitch*((float)Math.PI / 180F)));
        deliverStack.translate(entityTrans.x, entityTrans.y, entityTrans.z);
        float baseScale = getModelScale();
        deliverStack.scale(baseScale, baseScale, baseScale);
        
        // 获取材质 Morph 结果
        fetchMaterialMorphResults();
        
        // 批量获取所有子网格元数据（1 次 JNI 替代逐子网格调用）
        subMeshDataBuf.clear();
        nf.BatchGetSubMeshData(model, subMeshDataBuf);
        
        // 检查是否启用 Toon 渲染
        boolean useToon = ConfigManager.isToonRenderingEnabled();
        if (useToon) {
            // 初始化 Toon 着色器（懒加载）
            if (toonShaderCpu == null) {
                toonShaderCpu = new ToonShaderCpu();
                if (!toonShaderCpu.init()) {
                    logger.warn("ToonShaderCpu 初始化失败，回退到普通着色");
                    useToon = false;
                }
            }
        }
        
        if (useToon && toonShaderCpu != null && toonShaderCpu.isInitialized()) {
            // Toon 渲染模式
            renderToon(MCinstance, lightIntensity, deliverStack);
            return;
        }
        
        // 普通渲染模式
        // 安卓兼容：光照强度通过 ColorModulator uniform 传递（替代 glVertexAttrib4f 常量 Color 属性）
        // 安卓 GL 翻译层（gl4es/ANGLE）对 glVertexAttrib4f 常量属性支持不完整，
        // 导致 Color.a=0 → entity_cutout 着色器 discard → 模型全透明
        boolean irisActive = IrisCompat.isIrisShaderActive();
        float colorFactor = irisActive ? 1.0f : lightIntensity;
        RenderSystem.setShaderColor(colorFactor, colorFactor, colorFactor, 1.0f);
        
        if(MmdSkinClient.usingMMDShader == 0){
            ShaderInstance mcShader = RenderSystem.getShader();
            if (mcShader == null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                return;
            }
            shaderProgram = mcShader.getId();
            setUniforms(mcShader, deliverStack);
            mcShader.apply();
        }
        if(MmdSkinClient.usingMMDShader == 1){
            shaderProgram = MMDShaderProgram;
            GlStateManager._glUseProgram(shaderProgram);
        }
        
        updateLocation(shaderProgram);

        BufferUploader.reset();
        GL46C.glBindVertexArray(vertexArrayObject);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendEquation(GL46C.GL_FUNC_ADD);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // === 上传顶点数据到 VBO（使用 glBufferSubData 仅更新数据，避免每帧重分配 GPU 内存）===
        int posAndNorSize = vertexCount * 12; // float * 3
        long posData = nf.GetPoss(model);
        nf.CopyDataToByteBuffer(posBuffer, posData, posAndNorSize);
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vertexBufferObject);
        GL46C.glBufferSubData(GL46C.GL_ARRAY_BUFFER, 0, posBuffer);

        long normalData = nf.GetNormals(model);
        nf.CopyDataToByteBuffer(norBuffer, normalData, posAndNorSize);
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
        GL46C.glBufferSubData(GL46C.GL_ARRAY_BUFFER, 0, norBuffer);

        // 性能优化：无 UV Morph 时跳过 UV0 重传（已在创建时上传）
        if (hasUvMorph) {
            int uv0Size = vertexCount * 8; // float * 2
            long uv0Data = nf.GetUVs(model);
            nf.CopyDataToByteBuffer(uv0Buffer, uv0Data, uv0Size);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, texcoordBufferObject);
            GL46C.glBufferSubData(GL46C.GL_ARRAY_BUFFER, 0, uv0Buffer);
        }

        // 性能优化：uv1 已在创建时上传，无需每帧重传

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
        if (K_uv2Location != -1) {
            GL46C.glEnableVertexAttribArray(K_uv2Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2BufferObject);
            GL46C.glVertexAttribIPointer(K_uv2Location, 2, GL46C.GL_INT, 0, 0);
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

        // === 绑定顶点属性（数据已在 VBO 中，只需设置指针）===
        if (positionLocation != -1) {
            GL46C.glEnableVertexAttribArray(positionLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vertexBufferObject);
            GL46C.glVertexAttribPointer(positionLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (normalLocation != -1) {
            GL46C.glEnableVertexAttribArray(normalLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
            GL46C.glVertexAttribPointer(normalLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (uv0Location != -1) {
            GL46C.glEnableVertexAttribArray(uv0Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, texcoordBufferObject);
            GL46C.glVertexAttribPointer(uv0Location, 2, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (uv1Location != -1) {
            GL46C.glEnableVertexAttribArray(uv1Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv1BufferObject);
            GL46C.glVertexAttribIPointer(uv1Location, 2, GL46C.GL_INT, 0, 0);
        }

        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject);

        // 使用预分配的矩阵缓冲区（避免每帧分配）
        modelViewMatBuff.clear();
        projMatBuff.clear();
        deliverStack.last().pose().get(modelViewMatBuff);
        RenderSystem.getProjectionMatrix().get(projMatBuff);

        //upload Uniforms(MMDShader)
        if(MmdSkinClient.usingMMDShader == 1){
            RenderSystem.glUniformMatrix4(modelViewLocation, false, modelViewMatBuff);
            RenderSystem.glUniformMatrix4(projMatLocation, false, projMatBuff);

            if(light0Location != -1){
                light0Buff.clear();
                light0Buff.put(light0Direction.x);
                light0Buff.put(light0Direction.y);
                light0Buff.put(light0Direction.z);
                light0Buff.flip();
                RenderSystem.glUniform3(light0Location, light0Buff);
            }
            if(light1Location != -1){
                light1Buff.clear();
                light1Buff.put(light1Direction.x);
                light1Buff.put(light1Direction.y);
                light1Buff.put(light1Direction.z);
                light1Buff.flip();
                RenderSystem.glUniform3(light1Location, light1Buff);
            }
            if(sampler0Location != -1){
                GL46C.glUniform1i(sampler0Location, 0);
            }
            if(sampler1Location != -1){
                RenderSystem.activeTexture(GL46C.GL_TEXTURE1);
                RenderSystem.bindTexture(lightMapMaterial.tex);
                GL46C.glUniform1i(sampler1Location, 1);
            }
            if(sampler2Location != -1){
                RenderSystem.activeTexture(GL46C.GL_TEXTURE2);
                RenderSystem.bindTexture(lightMapMaterial.tex);
                GL46C.glUniform1i(sampler2Location, 2);
            }
        }

        // K_* 属性（自定义着色器属性）— 复用已上传的 VBO，无需重复 glBufferData
        if (K_positionLocation != -1) {
            GL46C.glEnableVertexAttribArray(K_positionLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vertexBufferObject);
            GL46C.glVertexAttribPointer(K_positionLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (K_normalLocation != -1) {
            GL46C.glEnableVertexAttribArray(K_normalLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
            GL46C.glVertexAttribPointer(K_normalLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (K_uv0Location != -1) {
            GL46C.glEnableVertexAttribArray(K_uv0Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, texcoordBufferObject);
            GL46C.glVertexAttribPointer(K_uv0Location, 2, GL46C.GL_FLOAT, false, 0, 0);
        }
        if(K_projMatLocation != -1){
            projMatBuff.position(0);
            RenderSystem.glUniformMatrix4(K_projMatLocation, false, projMatBuff);
        }
        if(K_modelViewLocation != -1){
            modelViewMatBuff.position(0);
            RenderSystem.glUniformMatrix4(K_modelViewLocation, false, modelViewMatBuff);
        }
        if(K_sampler0Location != -1){
            GL46C.glUniform1i(K_sampler0Location, 0);
        }
        if(K_sampler2Location != -1){
            RenderSystem.activeTexture(GL46C.GL_TEXTURE2);
            RenderSystem.bindTexture(lightMapMaterial.tex);
            GL46C.glUniform1i(K_sampler2Location, 2);
        }
        if(KAIMyLocationV != -1)
            GL46C.glUniform1i(KAIMyLocationV, 1);
        
        if(KAIMyLocationF != -1)
            GL46C.glUniform1i(KAIMyLocationF, 1);

        // Iris 属性 — 复用已上传的 VBO，无需重复 glBufferData
        if (I_positionLocation != -1) {
            GL46C.glEnableVertexAttribArray(I_positionLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vertexBufferObject);
            GL46C.glVertexAttribPointer(I_positionLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (I_normalLocation != -1) {
            GL46C.glEnableVertexAttribArray(I_normalLocation);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
            GL46C.glVertexAttribPointer(I_normalLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (I_uv0Location != -1) {
            GL46C.glEnableVertexAttribArray(I_uv0Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, texcoordBufferObject);
            GL46C.glVertexAttribPointer(I_uv0Location, 2, GL46C.GL_FLOAT, false, 0, 0);
        }

        //Draw（从预填充的 subMeshDataBuf 读取元数据，0 次逐子网格 JNI 调用）
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
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
            if (mats[materialID].tex == 0)
                texId = MCinstance.getTextureManager().getTexture(TextureManager.INTENTIONAL_MISSING_TEXTURE).getId();
            else
                texId = mats[materialID].tex;
            RenderSystem.setShaderTexture(0, texId);
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, texId);
            long startPos = (long) beginIndex * indexElementSize;

            GL46C.glDrawElements(GL46C.GL_TRIANGLES, vertCount, indexType, startPos);
        }

        if(KAIMyLocationV != -1)
            GL46C.glUniform1i(KAIMyLocationV, 0);
        if(KAIMyLocationF != -1)
            GL46C.glUniform1i(KAIMyLocationF, 0);

        // === 关键：恢复 OpenGL 状态，防止与 Iris 冲突 ===
        // 禁用所有启用的顶点属性数组
        if (positionLocation != -1) GL46C.glDisableVertexAttribArray(positionLocation);
        if (normalLocation != -1) GL46C.glDisableVertexAttribArray(normalLocation);
        if (uv0Location != -1) GL46C.glDisableVertexAttribArray(uv0Location);
        if (uv1Location != -1) GL46C.glDisableVertexAttribArray(uv1Location);
        if (uv2Location != -1) GL46C.glDisableVertexAttribArray(uv2Location);
        if (colorLocation != -1) GL46C.glDisableVertexAttribArray(colorLocation);
        if (K_positionLocation != -1) GL46C.glDisableVertexAttribArray(K_positionLocation);
        if (K_normalLocation != -1) GL46C.glDisableVertexAttribArray(K_normalLocation);
        if (K_uv0Location != -1) GL46C.glDisableVertexAttribArray(K_uv0Location);
        if (K_uv2Location != -1) GL46C.glDisableVertexAttribArray(K_uv2Location);
        if (I_positionLocation != -1) GL46C.glDisableVertexAttribArray(I_positionLocation);
        if (I_normalLocation != -1) GL46C.glDisableVertexAttribArray(I_normalLocation);
        if (I_uv0Location != -1) GL46C.glDisableVertexAttribArray(I_uv0Location);
        if (I_uv2Location != -1) GL46C.glDisableVertexAttribArray(I_uv2Location);
        if (I_colorLocation != -1) GL46C.glDisableVertexAttribArray(I_colorLocation);
        
        // 解绑缓冲区
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, 0);
        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, 0);
        
        // 解绑 VAO（重要：让 Minecraft/Iris 使用自己的 VAO）
        GL46C.glBindVertexArray(0);
        
        // 确保纹理单元恢复到 TEXTURE0
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);

        ShaderInstance currentShader = RenderSystem.getShader();
        if (currentShader != null) {
            currentShader.clear();
        }
        BufferUploader.reset();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Toon 渲染模式（CPU 蒙皮版本）
     * 两遍渲染：1. 描边（背面扩张）2. 主体（卡通着色）
     * 
     * Iris 兼容：
     *   Iris 激活时，先通过 ExtendedShader.apply() 绑定 G-buffer FBO + MRT draw buffers，
     *   再切换到 Toon 着色器程序。Toon 片段着色器已声明 layout(location=0..3) 多输出，
     *   确保 Iris 的 draw buffers 全部被写入合理数据，避免透明。
     */
    private void renderToon(Minecraft MCinstance, float lightIntensity, PoseStack deliverStack) {
        BufferUploader.reset();
        GL46C.glBindVertexArray(vertexArrayObject);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendEquation(GL46C.GL_FUNC_ADD);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        
        // Iris 兼容：绑定 Iris G-buffer FBO（如果 Iris 光影激活）
        boolean irisActive = IrisCompat.isIrisShaderActive();
        if (irisActive) {
            ShaderInstance irisShader = RenderSystem.getShader();
            if (irisShader != null) {
                setUniforms(irisShader, deliverStack);
                irisShader.apply();  // 绑定 Iris G-buffer FBO + MRT draw buffers
            }
        }
        
        // 获取蒙皮后的顶点数据（由 Rust 引擎计算）并一次性上传到 VBO（两遍共用）
        int posAndNorSize = vertexCount * 12;
        long posData = nf.GetPoss(model);
        nf.CopyDataToByteBuffer(posBuffer, posData, posAndNorSize);
        long normalData = nf.GetNormals(model);
        nf.CopyDataToByteBuffer(norBuffer, normalData, posAndNorSize);
        
        // 上传顶点数据到 VBO（glBufferSubData，描边和主体两遍共用，避免重复上传）
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vertexBufferObject);
        GL46C.glBufferSubData(GL46C.GL_ARRAY_BUFFER, 0, posBuffer);
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
        GL46C.glBufferSubData(GL46C.GL_ARRAY_BUFFER, 0, norBuffer);
        if (hasUvMorph) {
            int uv0Size = vertexCount * 8;
            long uv0Data = nf.GetUVs(model);
            nf.CopyDataToByteBuffer(uv0Buffer, uv0Data, uv0Size);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, texcoordBufferObject);
            GL46C.glBufferSubData(GL46C.GL_ARRAY_BUFFER, 0, uv0Buffer);
        }
        
        // 设置矩阵
        modelViewMatBuff.clear();
        projMatBuff.clear();
        deliverStack.last().pose().get(modelViewMatBuff);
        RenderSystem.getProjectionMatrix().get(projMatBuff);
        
        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject);
        
        // ===== 第一遍：描边 =====
        if (toonConfig.isOutlineEnabled()) {
            toonShaderCpu.useOutline();
            
            int posLoc = toonShaderCpu.getOutlinePositionLocation();
            int norLoc = toonShaderCpu.getOutlineNormalLocation();
            
            // 设置顶点属性（VBO 数据已上传，只需绑定属性指针）
            if (posLoc != -1) {
                GL46C.glEnableVertexAttribArray(posLoc);
                GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vertexBufferObject);
                GL46C.glVertexAttribPointer(posLoc, 3, GL46C.GL_FLOAT, false, 0, 0);
            }
            if (norLoc != -1) {
                GL46C.glEnableVertexAttribArray(norLoc);
                GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
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
            
            // 正面剔除，只绘制背面（扩张后的背面形成描边）
            GL46C.glCullFace(GL46C.GL_FRONT);
            RenderSystem.enableCull();
            
            // 绘制描边（从 subMeshDataBuf 读取元数据）
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
        
        int posLoc = toonShaderCpu.getPositionLocation();
        int norLoc = toonShaderCpu.getNormalLocation();
        int uvLoc = toonShaderCpu.getUv0Location();
        
        // 设置顶点属性（VBO 数据已上传，只需绑定属性指针）
        if (posLoc != -1) {
            GL46C.glEnableVertexAttribArray(posLoc);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vertexBufferObject);
            GL46C.glVertexAttribPointer(posLoc, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (norLoc != -1) {
            GL46C.glEnableVertexAttribArray(norLoc);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
            GL46C.glVertexAttribPointer(norLoc, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        if (uvLoc != -1) {
            GL46C.glEnableVertexAttribArray(uvLoc);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, texcoordBufferObject);
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
        
        // 绘制所有子网格（从 subMeshDataBuf 读取元数据）
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
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
        
        // 清理顶点属性
        if (posLoc != -1) GL46C.glDisableVertexAttribArray(posLoc);
        if (norLoc != -1) GL46C.glDisableVertexAttribArray(norLoc);
        if (uvLoc != -1) GL46C.glDisableVertexAttribArray(uvLoc);
        
        // 解绑缓冲区和 VAO
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, 0);
        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL46C.glBindVertexArray(0);
        
        // 恢复默认着色器
        GL46C.glUseProgram(0);
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        BufferUploader.reset();
    }


    void updateLocation(int shaderProgram){
        if (shaderProgram == cachedShaderProgram) return;
        cachedShaderProgram = shaderProgram;
        positionLocation = GlStateManager._glGetAttribLocation(shaderProgram, "Position");
        normalLocation = GlStateManager._glGetAttribLocation(shaderProgram, "Normal");
        uv0Location = GlStateManager._glGetAttribLocation(shaderProgram, "UV0");
        uv1Location = GlStateManager._glGetAttribLocation(shaderProgram, "UV1");
        uv2Location = GlStateManager._glGetAttribLocation(shaderProgram, "UV2");
        colorLocation = GlStateManager._glGetAttribLocation(shaderProgram, "Color");
        projMatLocation = GlStateManager._glGetUniformLocation(shaderProgram, "ProjMat");
        modelViewLocation = GlStateManager._glGetUniformLocation(shaderProgram, "ModelViewMat");
        sampler0Location = GlStateManager._glGetUniformLocation(shaderProgram, "Sampler0");
        sampler1Location = GlStateManager._glGetUniformLocation(shaderProgram, "Sampler1");
        sampler2Location = GlStateManager._glGetUniformLocation(shaderProgram, "Sampler2");
        light0Location = GlStateManager._glGetUniformLocation(shaderProgram, "Light0_Direction");
        light1Location = GlStateManager._glGetUniformLocation(shaderProgram, "Light1_Direction");

        K_positionLocation = GlStateManager._glGetAttribLocation(shaderProgram, "K_Position");
        K_normalLocation = GlStateManager._glGetAttribLocation(shaderProgram, "K_Normal");
        K_uv0Location = GlStateManager._glGetAttribLocation(shaderProgram, "K_UV0");
        K_uv2Location = GlStateManager._glGetAttribLocation(shaderProgram, "K_UV2");
        K_projMatLocation = GlStateManager._glGetUniformLocation(shaderProgram, "K_ProjMat");
        K_modelViewLocation = GlStateManager._glGetUniformLocation(shaderProgram, "K_ModelViewMat");
        K_sampler0Location = GlStateManager._glGetUniformLocation(shaderProgram, "K_Sampler0");
        K_sampler2Location = GlStateManager._glGetUniformLocation(shaderProgram, "K_Sampler2");
        KAIMyLocationV = GlStateManager._glGetUniformLocation(shaderProgram, "MMDShaderV");
        KAIMyLocationF = GlStateManager._glGetUniformLocation(shaderProgram, "MMDShaderF");

        I_positionLocation = GlStateManager._glGetAttribLocation(shaderProgram, "iris_Position");
        I_normalLocation = GlStateManager._glGetAttribLocation(shaderProgram, "iris_Normal");
        I_uv0Location = GlStateManager._glGetAttribLocation(shaderProgram, "iris_UV0");
        I_uv2Location = GlStateManager._glGetAttribLocation(shaderProgram, "iris_UV2");
        I_colorLocation = GlStateManager._glGetAttribLocation(shaderProgram, "iris_Color");
    }

    public void setUniforms(ShaderInstance shader, PoseStack deliverStack) {
        setupShaderUniforms(shader, deliverStack, light0Direction, light1Direction, lightMapMaterial.tex);
    }
}
