package com.shiroha.mmdskin.ui.wheel;

import com.shiroha.mmdskin.NativeFunc;
import com.shiroha.mmdskin.config.PathConstants;
import com.shiroha.mmdskin.config.UIConstants;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import com.shiroha.mmdskin.ui.config.ModelSelectorConfig;
import com.shiroha.mmdskin.ui.config.MorphWheelConfig;
import com.shiroha.mmdskin.ui.config.MorphWheelConfigScreen;
import com.shiroha.mmdskin.ui.network.MorphWheelNetworkHandler;
import com.shiroha.mmdskin.util.KeyMappingUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 表情选择轮盘界面
 * 与动作轮盘保持一致的UI风格
 */
public class MorphWheelScreen extends AbstractWheelScreen {
    private static final Logger logger = LogManager.getLogger();
    private static final WheelStyle STYLE = new WheelStyle(
            0.80f, 0.25f,
            0xFF60A0D0, 0xCC60A0D0, 0x60FFFFFF,
            0xE0182030, 0xFF60A0D0, 0x80000000
    );
    
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    
    private final KeyMapping triggerKey;
    
    private List<MorphSlot> morphSlots = new ArrayList<>();
    
    private static class MorphSlot {
        String displayName;
        String morphName;
        String filePath;
        
        MorphSlot(String displayName, String morphName, String filePath) {
            this.displayName = displayName;
            this.morphName = morphName;
            this.filePath = filePath;
        }
    }
    
    public MorphWheelScreen(KeyMapping triggerKey) {
        super(Component.translatable("gui.mmdskin.morph_wheel"), STYLE);
        this.triggerKey = triggerKey;
    }

    @Override
    protected int getSlotCount() {
        return morphSlots.size();
    }
    
    @Override
    protected void init() {
        super.init();
        initWheelLayout();
        initMorphSlots();
        
        this.addRenderableWidget(Button.builder(
            Component.literal("⚙"), btn -> {
                this.minecraft.setScreen(new MorphWheelConfigScreen(this));
            }).bounds(this.width - 28, this.height - 28, 22, 22).build());
    }
    
    private void initMorphSlots() {
        morphSlots.clear();
        
        MorphWheelConfig config = MorphWheelConfig.getInstance();
        List<MorphWheelConfig.MorphEntry> displayed = config.getDisplayedMorphs();
        
        for (MorphWheelConfig.MorphEntry entry : displayed) {
            // 构建文件路径
            String filePath = getMorphFilePath(entry);
            morphSlots.add(new MorphSlot(entry.displayName, entry.morphName, filePath));
        }
        
        // 添加"重置表情"选项
        morphSlots.add(new MorphSlot(Component.translatable("gui.mmdskin.reset_morph").getString(), "__reset__", null));
        
        logger.info("表情轮盘: 加载 {} 个槽位", morphSlots.size());
    }
    
    private String getMorphFilePath(MorphWheelConfig.MorphEntry entry) {
        if (entry.source == null) {
            return PathConstants.getCustomMorphPath(entry.morphName);
        }
        switch (entry.source) {
            case "DEFAULT":
                return PathConstants.getDefaultMorphPath(entry.morphName);
            case "CUSTOM":
                return PathConstants.getCustomMorphPath(entry.morphName);
            case "MODEL":
                if (entry.modelName != null) {
                    return PathConstants.getModelMorphPath(entry.modelName, entry.morphName);
                }
                return PathConstants.getCustomMorphPath(entry.morphName);
            default:
                return PathConstants.getCustomMorphPath(entry.morphName);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 半透明背景
        guiGraphics.fill(0, 0, width, height, 0x80000000);
        
        updateSelectedSlot(mouseX, mouseY);
        
        if (!morphSlots.isEmpty()) {
            renderHighlight(guiGraphics);
            renderDividerLines(guiGraphics);
            renderOuterRing(guiGraphics);
            renderMorphLabels(guiGraphics);
        } else {
            String hint = Component.translatable("gui.mmdskin.morph_wheel.no_morphs").getString();
            int hintWidth = font.width(hint);
            guiGraphics.drawString(font, hint, centerX - hintWidth / 2, centerY - 4, TEXT_COLOR);
        }
        
        // 中心圆 + 标题 + 选中名称
        String centerText = Component.translatable("gui.mmdskin.morph_wheel.select").getString();
        if (selectedSlot >= 0 && selectedSlot < morphSlots.size()) {
            centerText = morphSlots.get(selectedSlot).displayName;
        }
        renderCenterCircle(guiGraphics, centerText, 0xFF60A0D0);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderMorphLabels(GuiGraphics guiGraphics) {
        int segments = morphSlots.size();
        double segmentAngle = 360.0 / segments;
        
        for (int i = 0; i < segments; i++) {
            MorphSlot slot = morphSlots.get(i);
            double midAngle = Math.toRadians(-90 + (i + 0.5) * segmentAngle);
            int labelRadius = (innerRadius + outerRadius) / 2;
            
            int labelX = centerX + (int) (Math.cos(midAngle) * labelRadius);
            int labelY = centerY + (int) (Math.sin(midAngle) * labelRadius);
            
            String label = slot.displayName;
            if (label.length() > 8) {
                label = label.substring(0, 7) + "..";
            }
            
            int textWidth = font.width(label);
            int textColor = (i == selectedSlot) ? 0xFFFFFF00 : TEXT_COLOR;
            
            guiGraphics.drawString(font, label, labelX - textWidth / 2 + 1, labelY - 4 + 1, style.textShadow(), false);
            guiGraphics.drawString(font, label, labelX - textWidth / 2, labelY - 4, textColor, false);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selectedSlot >= 0 && selectedSlot < morphSlots.size()) {
            executeMorph(morphSlots.get(selectedSlot));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (triggerKey != null) {
            com.mojang.blaze3d.platform.InputConstants.Key boundKey = KeyMappingUtil.getBoundKey(triggerKey);
            if (boundKey != null && boundKey.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM && boundKey.getValue() == keyCode) {
                // 松开按键时执行选中的表情
                if (selectedSlot >= 0 && selectedSlot < morphSlots.size()) {
                    executeMorph(morphSlots.get(selectedSlot));
                }
                this.onClose();
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
    
    private void executeMorph(MorphSlot slot) {
        logger.info("执行表情: {}", slot.displayName);
        
        // 获取当前玩家的模型
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // 从配置获取玩家选择的模型
        String playerName = mc.player.getName().getString();
        String selectedModel = ModelSelectorConfig.getInstance().getPlayerModel(playerName);
        
        // 如果是默认渲染，不处理
        if (selectedModel == null || selectedModel.isEmpty() || UIConstants.DEFAULT_MODEL_NAME.equals(selectedModel)) {
            logger.warn("当前使用默认渲染，无法应用表情");
            return;
        }
        
        MMDModelManager.Model m = MMDModelManager.GetModel(selectedModel, playerName);
        if (m == null) {
            logger.warn("未找到玩家模型: {}", selectedModel);
            return;
        }
        
        long modelHandle = m.model.getModelHandle();
        NativeFunc nf = NativeFunc.GetInst();
        
        if ("__reset__".equals(slot.morphName)) {
            // 重置所有表情
            nf.ResetAllMorphs(modelHandle);
            logger.info("已重置所有表情");
        } else {
            // 应用 VPD 表情/姿势
            if (slot.filePath != null) {
                int result = nf.ApplyVpdMorph(modelHandle, slot.filePath);
                if (result >= 0) {
                    // 解码返回值: 高16位为骨骼数，低16位为 Morph 数
                    int boneCount = (result >> 16) & 0xFFFF;
                    int morphCount = result & 0xFFFF;
                    logger.info("应用表情成功: {}, 匹配 {} 个 Morph, {} 个 Bone", slot.filePath, morphCount, boneCount);
                } else if (result == -1) {
                    logger.error("VPD 文件加载失败: {}", slot.filePath);
                } else if (result == -2) {
                    logger.error("模型不存在, handle={}", modelHandle);
                }
            }
        }
        
        // 发送网络同步（如果需要）
        MorphWheelNetworkHandler.sendMorphToServer(slot.morphName);
    }
}
