package com.shiroha.mmdskin.ui.selector;

import com.shiroha.mmdskin.ui.config.ModelSelectorConfig;
import com.shiroha.mmdskin.ui.network.ModelSelectorNetworkHandler;

import com.shiroha.mmdskin.config.UIConstants;
import com.shiroha.mmdskin.renderer.model.ModelInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型选择界面 — 简约右侧面板风格
 * 右侧面板列出所有模型，左侧留空用于模型预览
 */
public class ModelSelectorScreen extends Screen {
    private static final Logger logger = LogManager.getLogger();
    
    // 右侧面板布局
    private static final int PANEL_WIDTH = 140;
    private static final int PANEL_MARGIN = 4;
    private static final int HEADER_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 14;
    private static final int ITEM_SPACING = 1;
    private static final int SETTINGS_BTN_SIZE = 8;
    
    // 统一简约配色
    private static final int COLOR_PANEL_BG = 0xC0101418;
    private static final int COLOR_PANEL_BORDER = 0xFF2A3A4A;
    private static final int COLOR_ITEM_HOVER = 0x30FFFFFF;
    private static final int COLOR_ITEM_SELECTED = 0x40306040;
    private static final int COLOR_ACCENT = 0xFF60A0D0;
    private static final int COLOR_TEXT = 0xFFDDDDDD;
    private static final int COLOR_TEXT_DIM = 0xFF888888;
    private static final int COLOR_TEXT_SELECTED = 0xFFAAFFAA;
    private static final int COLOR_SEPARATOR = 0x30FFFFFF;
    private static final int COLOR_SETTINGS_BTN = 0xFF888888;
    private static final int COLOR_SETTINGS_BTN_HOVER = 0xFFDDDDDD;
    
    private final List<ModelCardEntry> modelCards;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private String currentModel;
    private int hoveredCardIndex = -1;
    private boolean hoveredOnSettingsBtn = false;
    
    // 面板区域缓存
    private int panelX, panelY, panelH;
    private int listTop, listBottom;

    public ModelSelectorScreen() {
        super(Component.translatable("gui.mmdskin.model_selector"));
        this.modelCards = new ArrayList<>();
        this.currentModel = ModelSelectorConfig.getInstance().getSelectedModel();
        loadAvailableModels();
    }

    private void loadAvailableModels() {
        modelCards.clear();
        
        modelCards.add(new ModelCardEntry(UIConstants.DEFAULT_MODEL_NAME, null));
        
        List<ModelInfo> models = ModelInfo.scanModels();
        for (ModelInfo info : models) {
            modelCards.add(new ModelCardEntry(info.getFolderName(), info));
        }
        
        logger.info("共加载 {} 个模型选项", modelCards.size());
    }

    @Override
    protected void init() {
        super.init();
        
        panelX = this.width - PANEL_WIDTH - PANEL_MARGIN;
        panelY = PANEL_MARGIN;
        panelH = this.height - PANEL_MARGIN * 2;
        
        listTop = panelY + HEADER_HEIGHT;
        listBottom = panelY + panelH - FOOTER_HEIGHT;
        
        int contentHeight = modelCards.size() * (ITEM_HEIGHT + ITEM_SPACING);
        int visibleHeight = listBottom - listTop;
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        
        // 底部按钮
        int btnY = listBottom + 4;
        int btnW = (PANEL_WIDTH - 12) / 2;
        
        this.addRenderableWidget(Button.builder(Component.literal("刷新"), btn -> refreshModels())
            .bounds(panelX + 4, btnY, btnW, 14).build());
        
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> this.onClose())
            .bounds(panelX + 8 + btnW, btnY, btnW, 14).build());
    }

    private void refreshModels() {
        loadAvailableModels();
        scrollOffset = 0;
        this.clearWidgets();
        this.init();
        logger.info("模型列表已刷新");
    }

    private void selectModel(ModelCardEntry card) {
        this.currentModel = card.displayName;
        ModelSelectorConfig.getInstance().setSelectedModel(card.displayName);
        
        ModelSelectorNetworkHandler.sendModelChangeToServer(card.displayName);
        
        // 仅重载本地玩家的模型（不影响其他远程玩家的模型和动画）
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            String playerName = mc.player.getName().getString();
            com.shiroha.mmdskin.renderer.model.MMDModelManager.forceReloadPlayerModels(playerName);
        }
        
        logger.info("玩家选择模型: {}", card.displayName);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 右侧面板背景
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelH, COLOR_PANEL_BG);
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelH, COLOR_PANEL_BORDER);
        
        // 头部
        renderHeader(guiGraphics);
        
        // 模型列表（裁剪区域）
        guiGraphics.enableScissor(panelX, listTop, panelX + PANEL_WIDTH, listBottom);
        renderModelList(guiGraphics, mouseX, mouseY);
        guiGraphics.disableScissor();
        
        // 滚动条
        renderScrollbar(guiGraphics);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics guiGraphics) {
        int cx = panelX + PANEL_WIDTH / 2;
        guiGraphics.drawCenteredString(this.font, this.title, cx, panelY + 4, COLOR_ACCENT);
        
        String info = modelCards.size() - 1 + " 个模型";
        guiGraphics.drawCenteredString(this.font, info, cx, panelY + 16, COLOR_TEXT_DIM);
        
        guiGraphics.fill(panelX + 8, listTop - 2, panelX + PANEL_WIDTH - 8, listTop - 1, COLOR_SEPARATOR);
    }

    private void renderModelList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        hoveredCardIndex = -1;
        hoveredOnSettingsBtn = false;
        
        for (int i = 0; i < modelCards.size(); i++) {
            ModelCardEntry card = modelCards.get(i);
            int itemY = listTop + i * (ITEM_HEIGHT + ITEM_SPACING) - scrollOffset;
            
            if (itemY + ITEM_HEIGHT < listTop || itemY > listBottom) continue;
            
            boolean isSelected = card.displayName.equals(currentModel);
            boolean isHovered = mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
                             && mouseY >= Math.max(itemY, listTop) 
                             && mouseY <= Math.min(itemY + ITEM_HEIGHT, listBottom);
            
            // 检测设置按钮悬停
            boolean isSettingsBtnHovered = false;
            if (isHovered && card.modelInfo != null) {
                int btnX = panelX + PANEL_WIDTH - SETTINGS_BTN_SIZE - 2;
                int btnY = itemY + (ITEM_HEIGHT - SETTINGS_BTN_SIZE) / 2;
                isSettingsBtnHovered = mouseX >= btnX && mouseX <= btnX + SETTINGS_BTN_SIZE
                                   && mouseY >= btnY && mouseY <= btnY + SETTINGS_BTN_SIZE;
            }
            
            if (isHovered) {
                hoveredCardIndex = i;
                hoveredOnSettingsBtn = isSettingsBtnHovered;
            }
            
            renderItem(guiGraphics, card, panelX, itemY, PANEL_WIDTH, isSelected, isHovered, isSettingsBtnHovered);
        }
    }

    private void renderItem(GuiGraphics guiGraphics, ModelCardEntry card, int x, int y, int w,
                             boolean isSelected, boolean isHovered, boolean isSettingsBtnHovered) {
        // 背景
        if (isSelected) {
            guiGraphics.fill(x, y, x + w, y + ITEM_HEIGHT, COLOR_ITEM_SELECTED);
            // 左侧选中指示条
            guiGraphics.fill(x, y + 1, x + 2, y + ITEM_HEIGHT - 1, COLOR_ACCENT);
        } else if (isHovered) {
            guiGraphics.fill(x, y, x + w, y + ITEM_HEIGHT, COLOR_ITEM_HOVER);
        }
        
        int textX = x + 8;
        boolean hasSettingsBtn = card.modelInfo != null;
        
        // 模型名称（为设置按钮预留空间）
        int maxNameLen = hasSettingsBtn ? 12 : 16;
        String displayName = truncate(card.displayName, maxNameLen);
        int nameColor = isSelected ? COLOR_TEXT_SELECTED : COLOR_TEXT;
        guiGraphics.drawString(this.font, displayName, textX, y + 3, nameColor);
        
        // 右侧区域：设置按钮 或 选中标记
        if (hasSettingsBtn && isHovered) {
            // 设置按钮（齿轮图标），仅鼠标悬停时显示
            int btnX = x + w - SETTINGS_BTN_SIZE - 2;
            int btnY = y + (ITEM_HEIGHT - SETTINGS_BTN_SIZE) / 2;
            int btnColor = isSettingsBtnHovered ? COLOR_SETTINGS_BTN_HOVER : COLOR_SETTINGS_BTN;
            
            if (isSettingsBtnHovered) {
                guiGraphics.fill(btnX - 1, btnY - 1, btnX + SETTINGS_BTN_SIZE + 1, btnY + SETTINGS_BTN_SIZE + 1, 0x40FFFFFF);
            }
            guiGraphics.drawString(this.font, "\u2699", btnX, btnY, btnColor);
        } else if (isSelected) {
            guiGraphics.drawString(this.font, "\u2713", x + w - 10, y + 3, COLOR_ACCENT);
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics) {
        if (maxScroll <= 0) return;
        
        int barX = panelX + PANEL_WIDTH - 4;
        int barH = listBottom - listTop;
        
        // 轨道
        guiGraphics.fill(barX, listTop, barX + 2, listBottom, 0x20FFFFFF);
        
        // 滑块
        int thumbH = Math.max(16, barH * barH / (barH + maxScroll));
        int thumbY = listTop + (int)((barH - thumbH) * ((float) scrollOffset / maxScroll));
        guiGraphics.fill(barX, thumbY, barX + 2, thumbY + thumbH, COLOR_ACCENT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredCardIndex >= 0 && hoveredCardIndex < modelCards.size()) {
            ModelCardEntry card = modelCards.get(hoveredCardIndex);
            
            if (hoveredOnSettingsBtn && card.modelInfo != null) {
                // 点击设置按钮 -> 打开模型独立设置界面
                Minecraft.getInstance().setScreen(
                    new ModelSettingsScreen(card.displayName, this));
                return true;
            }
            
            // 点击条目主体 -> 选择模型
            selectModel(card);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 仅面板区域响应滚动
        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(scrollY * 24)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }

    /**
     * 模型卡片条目
     */
    private static class ModelCardEntry {
        final String displayName;
        final ModelInfo modelInfo;

        ModelCardEntry(String displayName, ModelInfo modelInfo) {
            this.displayName = displayName;
            this.modelInfo = modelInfo;
        }
    }
}
