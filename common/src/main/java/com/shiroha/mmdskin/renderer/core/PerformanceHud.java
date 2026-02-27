package com.shiroha.mmdskin.renderer.core;

import com.shiroha.mmdskin.NativeFunc;
import com.shiroha.mmdskin.config.ConfigManager;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import com.shiroha.mmdskin.renderer.resource.MMDTextureManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46C;

import java.util.ArrayList;
import java.util.List;

/**
 * 性能调试 HUD
 * 在屏幕左上角显示模型内存/显存占用，带半透明背景。
 */
public class PerformanceHud {
    
    private static final int BG_COLOR    = 0xB0000000; // 半透明黑色背景
    private static final int TITLE_COLOR = 0xFF55FF55; // 绿色标题
    private static final int LABEL_COLOR = 0xFFAAAAAA; // 灰色标签
    private static final int VALUE_COLOR = 0xFFFFFFFF; // 白色数值
    private static final int WARN_COLOR  = 0xFFFFAA00; // 橙色警告
    private static final int LINE_HEIGHT = 11;
    private static final int PADDING     = 5;
    private static final int INNER_PAD   = 3;
    
    private static final long REFRESH_INTERVAL_MS = 500;
    private static long lastRefreshTime = 0;
    
    // GPU 显存查询
    private static final int GL_GPU_MEM_TOTAL_NVX = 0x9048;
    private static final int GL_GPU_MEM_AVAIL_NVX = 0x9049;
    private static final int GL_VBO_FREE_ATI      = 0x87FB;
    
    private enum GpuVendor { NVIDIA, AMD, UNKNOWN }
    private static GpuVendor gpuVendor = null;
    private static long gpuTotalVram = 0;
    
    // 缓存的渲染行
    private static final List<HudLine> cachedLines = new ArrayList<>();
    private static int cachedMaxWidth = 0;
    
    private PerformanceHud() {}
    
    /**
     * HUD 渲染入口
     */
    public static void render(GuiGraphics graphics) {
        if (!ConfigManager.isDebugHudEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.options.renderDebug) return;
        
        if (gpuVendor == null) detectGpuVendor();
        
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime > REFRESH_INTERVAL_MS) {
            rebuildLines(mc.font);
            lastRefreshTime = now;
        }
        
        if (cachedLines.isEmpty()) return;
        
        Font font = mc.font;
        int totalHeight = cachedLines.size() * LINE_HEIGHT + INNER_PAD * 2;
        int totalWidth = cachedMaxWidth + INNER_PAD * 2;
        
        // 半透明背景
        graphics.fill(PADDING, PADDING, PADDING + totalWidth, PADDING + totalHeight, BG_COLOR);
        
        // 逐行渲染
        int y = PADDING + INNER_PAD;
        for (HudLine line : cachedLines) {
            graphics.drawString(font, line.text, PADDING + INNER_PAD, y, line.color, true);
            y += LINE_HEIGHT;
        }
    }
    
    /**
     * 重建所有显示行（每 500ms 调用一次）
     */
    private static void rebuildLines(Font font) {
        cachedLines.clear();
        
        // ===== 系统资源 =====
        addLine("▶ 系统资源", TITLE_COLOR);
        
        Runtime rt = Runtime.getRuntime();
        long jvmUsed = rt.totalMemory() - rt.freeMemory();
        long jvmMax = rt.maxMemory();
        addLine(String.format("  JVM   %s / %s", fmtB(jvmUsed), fmtB(jvmMax)), VALUE_COLOR);
        
        long gpuAvail = queryAvailableVram();
        if (gpuTotalVram > 0) {
            long gpuUsed = gpuTotalVram - gpuAvail;
            addLine(String.format("  GPU   %s / %s", fmtB(gpuUsed), fmtB(gpuTotalVram)), VALUE_COLOR);
        } else if (gpuAvail > 0) {
            addLine(String.format("  GPU   可用 %s", fmtB(gpuAvail)), VALUE_COLOR);
        } else {
            addLine("  GPU   N/A", LABEL_COLOR);
        }
        
        // ===== MMD 资源 =====
        addLine("", VALUE_COLOR); // 空行分隔
        addLine("▶ MMD 资源", TITLE_COLOR);
        
        List<MMDModelManager.Model> models = MMDModelManager.getLoadedModels();
        int curModels = models.size();
        int totalLoaded = MMDModelManager.getTotalModelsLoaded();
        int texCount = MMDTextureManager.getTextureCount();
        long texVram = MMDTextureManager.getTotalTextureVram();
        
        int modelPending = MMDModelManager.getCachePendingReleaseCount();
        if (modelPending > 0) {
            addLine(String.format("  模型   当前 %d  待释放 %d  累计 %d", curModels, modelPending, totalLoaded), VALUE_COLOR);
        } else {
            addLine(String.format("  模型   当前 %d  累计 %d", curModels, totalLoaded), VALUE_COLOR);
        }
        int pendingCount = MMDTextureManager.getPendingReleaseCount();
        long pendingVram = MMDTextureManager.getPendingReleaseVram();
        if (pendingCount > 0) {
            addLine(String.format("  纹理   %d 张  VRAM %s (待释放 %d 张 %s)",
                    texCount, fmtB(texVram), pendingCount, fmtB(pendingVram)), VALUE_COLOR);
        } else {
            addLine(String.format("  纹理   %d 张  VRAM %s", texCount, fmtB(texVram)), VALUE_COLOR);
        }
        
        // 汇总 RAM/VRAM
        long totalRam = 0, totalVram = 0;
        for (MMDModelManager.Model m : models) {
            totalRam += m.model.getRamUsage();
            totalVram += m.model.getVramUsage();
        }
        long totalMmdVram = totalVram + texVram;
        
        addLine(String.format("  RAM    %s", fmtB(totalRam)), VALUE_COLOR);
        addLine(String.format("  VRAM   %s (模型 %s + 纹理 %s)",
                fmtB(totalMmdVram), fmtB(totalVram), fmtB(texVram)), VALUE_COLOR);
        
        // ===== 模型详情 =====
        if (!models.isEmpty()) {
            NativeFunc nf = NativeFunc.GetInst();
            addLine("", VALUE_COLOR);
            addLine("▶ 模型详情", TITLE_COLOR);
            for (MMDModelManager.Model m : models) {
                IMMDModel model = m.model;
                long handle = model.getModelHandle();
                if (handle == 0) continue;
                
                String name = model.getModelName();
                if (name.length() > 24) name = name.substring(0, 22) + "..";
                addLine("  ○ " + name, VALUE_COLOR);
                
                // 内存占用
                long ram = model.getRamUsage();
                long vram = model.getVramUsage();
                int memColor = vram > 50 * 1024 * 1024 ? WARN_COLOR : LABEL_COLOR;
                addLine(String.format("    RAM %-10s  VRAM %s", fmtB(ram), fmtB(vram)), memColor);
                
                // 模型参数
                long idxCount = nf.GetIndexCount(handle);
                long vtxCount = nf.GetVertexCount(handle);
                int boneCount = nf.GetBoneCount(handle);
                int matCount = (int) nf.GetMaterialCount(handle);
                int vMorphs = nf.GetVertexMorphCount(handle);
                int uvMorphs = nf.GetUvMorphCount(handle);
                long faceCount = idxCount / 3;
                // 面数超过 10 万用橙色警告
                int faceColor = faceCount > 100_000 ? WARN_COLOR : LABEL_COLOR;
                addLine(String.format("    面 %s  顶点 %s  骨骼 %d  材质 %d",
                        fmtNum(faceCount), fmtNum(vtxCount), boneCount, matCount), faceColor);
                if (vMorphs > 0 || uvMorphs > 0) {
                    addLine(String.format("    Morph: 顶点 %d  UV %d", vMorphs, uvMorphs), LABEL_COLOR);
                }
            }
        }
        
        // 计算最大宽度
        cachedMaxWidth = 0;
        for (HudLine line : cachedLines) {
            int w = font.width(line.text);
            if (w > cachedMaxWidth) cachedMaxWidth = w;
        }
    }
    
    private static void addLine(String text, int color) {
        cachedLines.add(new HudLine(text, color));
    }
    
    // ===== GPU 显存查询 =====
    
    private static void detectGpuVendor() {
        try {
            String vendor = GL11.glGetString(GL11.GL_VENDOR);
            if (vendor == null) { gpuVendor = GpuVendor.UNKNOWN; return; }
            String v = vendor.toLowerCase();
            if (v.contains("nvidia")) {
                gpuVendor = GpuVendor.NVIDIA;
                int kb = GL46C.glGetInteger(GL_GPU_MEM_TOTAL_NVX);
                if (kb > 0) gpuTotalVram = (long) kb * 1024;
            } else if (v.contains("ati") || v.contains("amd")) {
                gpuVendor = GpuVendor.AMD;
            } else {
                gpuVendor = GpuVendor.UNKNOWN;
            }
        } catch (Exception e) {
            gpuVendor = GpuVendor.UNKNOWN;
        }
        while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* drain */ }
    }
    
    private static long queryAvailableVram() {
        try {
            long result = 0;
            if (gpuVendor == GpuVendor.NVIDIA) {
                int kb = GL46C.glGetInteger(GL_GPU_MEM_AVAIL_NVX);
                if (kb > 0) result = (long) kb * 1024;
            } else if (gpuVendor == GpuVendor.AMD) {
                int[] info = new int[4];
                GL46C.glGetIntegerv(GL_VBO_FREE_ATI, info);
                if (info[0] > 0) result = (long) info[0] * 1024;
            }
            while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* drain */ }
            return result;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // ===== 工具 =====
    
    private static String fmtB(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private static String fmtNum(long num) {
        if (num < 1_000) return String.valueOf(num);
        if (num < 1_000_000) return String.format("%.1fK", num / 1_000.0);
        return String.format("%.2fM", num / 1_000_000.0);
    }
    
    private record HudLine(String text, int color) {}
}
