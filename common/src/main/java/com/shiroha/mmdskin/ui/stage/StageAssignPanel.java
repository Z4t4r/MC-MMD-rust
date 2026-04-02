package com.shiroha.mmdskin.ui.stage;

import com.shiroha.mmdskin.config.StagePack;
import com.shiroha.mmdskin.stage.client.viewmodel.StageLobbyViewModel;
import com.shiroha.mmdskin.stage.domain.model.StageMemberState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 多人舞台房间面板。
 * 房主侧只负责邀请/查看成员，成员侧负责查看房间成员和本地自选动作。
 */
public class StageAssignPanel {

    private static final int PANEL_WIDTH = 180;
    private static final int BG = 0xC0101418;
    private static final int BORDER = 0xFF2A3A4A;
    private static final int ACCENT = 0xFF60A0D0;
    private static final int TEXT = 0xFFDDDDDD;
    private static final int TEXT_DIM = 0xFF888888;
    private static final int HOVER = 0x30FFFFFF;
    private static final int CHECKBOX_ON = 0xFF40C080;
    private static final int CHECKBOX_OFF = 0xFF505560;
    private static final int HEADER_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 16;
    private static final int MARGIN = 4;
    private static final int MEMBER_SPLIT_HEIGHT = 0x30FFFFFF;
    private static final int STATE_PENDING_COLOR = 0xFFD0A050;
    private static final int STATE_DECLINED_COLOR = 0xFFD05050;
    private static final int STATE_BUSY_COLOR = 0xFFB06060;

    private static final int INVITE_BTN_W = 32;
    private static final int INVITE_BTN_H = 14;

    private final Font font;
    private final StageLobbyViewModel lobbyViewModel = StageLobbyViewModel.getInstance();

    private int panelX;
    private int panelY;
    private int panelH;
    private int memberListTop;
    private int memberListBottom;
    private int motionSectionTop;
    private int motionListTop;
    private int motionListBottom;
    private int inviteBtnX;
    private int inviteBtnY;

    private int memberScrollOffset;
    private int memberMaxScroll;
    private int motionScrollOffset;
    private int motionMaxScroll;

    private int hoveredMemberIndex = -1;
    private int hoveredMotionIndex = -1;
    private boolean hoverInviteBtn;
    private boolean hoverGuestCustomToggle;

    private List<StageLobbyViewModel.HostEntry> hostEntries = new ArrayList<>();
    private List<StageLobbyViewModel.MemberView> guestMembers = new ArrayList<>();
    private List<StagePack.VmdFileInfo> motionVmdFiles = new ArrayList<>();

    public StageAssignPanel(Font font) {
        this.font = font;
    }

    public void layout(int screenWidth, int screenHeight) {
        this.panelX = screenWidth - PANEL_WIDTH - MARGIN;
        this.panelY = MARGIN;
        this.panelH = screenHeight - MARGIN * 2;
        this.memberListTop = panelY + HEADER_HEIGHT;

        if (lobbyViewModel.isSessionMember()) {
            int splitY = panelY + HEADER_HEIGHT + (int) ((panelH - HEADER_HEIGHT) * 0.34f);
            this.memberListBottom = splitY - 2;
            this.motionSectionTop = splitY + 6;
            this.motionListTop = motionSectionTop + 18;
            this.motionListBottom = panelY + panelH - MARGIN;
        } else {
            this.memberListBottom = panelY + panelH - MARGIN;
            this.motionSectionTop = 0;
            this.motionListTop = 0;
            this.motionListBottom = 0;
        }

        this.inviteBtnX = panelX + PANEL_WIDTH - INVITE_BTN_W - 6;
        this.inviteBtnY = panelY + 3;
        refreshPlayers();
        updateMotionScroll();
    }

    public void setStagePack(StagePack pack) {
        motionVmdFiles = new ArrayList<>();
        if (lobbyViewModel.isSessionMember()) {
            lobbyViewModel.setLocalCustomMotionPack(pack != null ? pack.getName() : null);
        }
        if (pack != null) {
            for (StagePack.VmdFileInfo info : pack.getVmdFiles()) {
                if (info.hasBones || info.hasMorphs) {
                    motionVmdFiles.add(info);
                }
            }
        }
        if (lobbyViewModel.isSessionMember()) {
            lobbyViewModel.retainLocalCustomMotionFiles(motionVmdFiles.stream().map(info -> info.name).toList());
        }
        motionScrollOffset = 0;
        updateMotionScroll();
    }

    public void refreshPlayers() {
        if (lobbyViewModel.isSessionMember()) {
            guestMembers = lobbyViewModel.getSessionMembersView();
        } else {
            hostEntries = lobbyViewModel.getHostPanelEntries();
        }
        updateMemberScroll();
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelH, BG);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, BORDER);

        renderHeader(g, mouseX, mouseY);
        renderMemberList(g, mouseX, mouseY);
        if (lobbyViewModel.isSessionMember()) {
            renderGuestMotionArea(g, mouseX, mouseY);
        }
    }

    private void renderHeader(GuiGraphics g, int mouseX, int mouseY) {
        g.drawCenteredString(font,
                Component.translatable("gui.mmdskin.stage.session_members"),
                panelX + PANEL_WIDTH / 2,
                panelY + 6,
                ACCENT);

        hoverInviteBtn = !lobbyViewModel.isSessionMember()
                && mouseX >= inviteBtnX && mouseX <= inviteBtnX + INVITE_BTN_W
                && mouseY >= inviteBtnY && mouseY <= inviteBtnY + INVITE_BTN_H;
        if (!lobbyViewModel.isSessionMember()) {
            int btnColor = hoverInviteBtn ? HOVER : 0x20FFFFFF;
            g.fill(inviteBtnX, inviteBtnY, inviteBtnX + INVITE_BTN_W, inviteBtnY + INVITE_BTN_H, btnColor);
            g.drawCenteredString(font, "+", inviteBtnX + INVITE_BTN_W / 2, inviteBtnY + 3, ACCENT);
        }
    }

    private void renderMemberList(GuiGraphics g, int mouseX, int mouseY) {
        hoveredMemberIndex = -1;
        g.enableScissor(panelX, memberListTop, panelX + PANEL_WIDTH, memberListBottom);
        if (lobbyViewModel.isSessionMember()) {
            renderGuestMembers(g, mouseX, mouseY);
        } else {
            renderHostMembers(g, mouseX, mouseY);
        }
        g.disableScissor();
        renderScrollbar(g, memberListTop, memberListBottom, memberScrollOffset, memberMaxScroll);
    }

    private void renderHostMembers(GuiGraphics g, int mouseX, int mouseY) {
        if (hostEntries.isEmpty()) {
            g.drawCenteredString(font,
                    Component.translatable("gui.mmdskin.stage.no_nearby"),
                    panelX + PANEL_WIDTH / 2,
                    memberListTop + 4,
                    TEXT_DIM);
            return;
        }

        for (int i = 0; i < hostEntries.size(); i++) {
            StageLobbyViewModel.HostEntry entry = hostEntries.get(i);
            int itemY = memberListTop + i * ITEM_HEIGHT - memberScrollOffset;
            if (itemY + ITEM_HEIGHT < memberListTop || itemY > memberListBottom) {
                continue;
            }

            int itemX = panelX + 4;
            int itemW = PANEL_WIDTH - 8;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW
                    && mouseY >= Math.max(itemY, memberListTop)
                    && mouseY <= Math.min(itemY + ITEM_HEIGHT, memberListBottom);
            if (hovered) {
                hoveredMemberIndex = i;
                g.fill(itemX, itemY, itemX + itemW, itemY + ITEM_HEIGHT, HOVER);
            }

            g.drawString(font, truncate(entry.name(), 12), itemX + 2, itemY + 4, entry.nearby() ? TEXT : TEXT_DIM, false);
            renderMemberState(g, itemX + itemW, itemY + 4, entry.state(), entry.useHostCamera());
        }
    }

    private void renderGuestMembers(GuiGraphics g, int mouseX, int mouseY) {
        if (guestMembers.isEmpty()) {
            g.drawCenteredString(font,
                    Component.translatable("gui.mmdskin.stage.waiting_host"),
                    panelX + PANEL_WIDTH / 2,
                    memberListTop + 4,
                    TEXT_DIM);
            return;
        }

        for (int i = 0; i < guestMembers.size(); i++) {
            StageLobbyViewModel.MemberView member = guestMembers.get(i);
            int itemY = memberListTop + i * ITEM_HEIGHT - memberScrollOffset;
            if (itemY + ITEM_HEIGHT < memberListTop || itemY > memberListBottom) {
                continue;
            }

            int itemX = panelX + 4;
            int itemW = PANEL_WIDTH - 8;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW
                    && mouseY >= Math.max(itemY, memberListTop)
                    && mouseY <= Math.min(itemY + ITEM_HEIGHT, memberListBottom);
            if (hovered) {
                hoveredMemberIndex = i;
                g.fill(itemX, itemY, itemX + itemW, itemY + ITEM_HEIGHT, HOVER);
            }

            String prefix = member.local() ? "*" : member.host() ? "H" : " ";
            g.drawString(font, truncate(prefix + " " + member.name(), 12), itemX + 2, itemY + 4, TEXT, false);
            renderMemberState(g, itemX + itemW, itemY + 4, member.state(), member.useHostCamera());
        }
    }

    private void renderGuestMotionArea(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(panelX + 8, motionSectionTop - 4, panelX + PANEL_WIDTH - 8, motionSectionTop - 3, MEMBER_SPLIT_HEIGHT);

        boolean customEnabled = lobbyViewModel.isLocalCustomMotionEnabled();
        int toggleX = panelX + 6;
        int toggleY = motionSectionTop;
        int toggleW = 18;
        int toggleH = 10;

        g.drawString(font, Component.translatable("gui.mmdskin.stage.local_motion_override"), panelX + 6, motionSectionTop - 12, TEXT, false);
        hoverGuestCustomToggle = mouseX >= toggleX && mouseX <= toggleX + PANEL_WIDTH - 12
                && mouseY >= toggleY && mouseY <= toggleY + toggleH;
        g.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, customEnabled ? CHECKBOX_ON : CHECKBOX_OFF);
        int dotX = customEnabled ? toggleX + toggleW - toggleH : toggleX;
        g.fill(dotX + 1, toggleY + 1, dotX + toggleH - 1, toggleY + toggleH - 1, 0xFFFFFFFF);

        if (!customEnabled) {
            hoveredMotionIndex = -1;
            g.drawString(font,
                    Component.translatable("gui.mmdskin.stage.local_motion_fallback"),
                    panelX + 28,
                    motionSectionTop + 1,
                    TEXT_DIM,
                    false);
            return;
        }

        if (motionVmdFiles.isEmpty()) {
            hoveredMotionIndex = -1;
            g.drawString(font,
                    Component.translatable("gui.mmdskin.stage.local_motion_empty"),
                    panelX + 28,
                    motionSectionTop + 1,
                    TEXT_DIM,
                    false);
            return;
        }

        hoveredMotionIndex = -1;
        g.enableScissor(panelX, motionListTop, panelX + PANEL_WIDTH, motionListBottom);
        for (int i = 0; i < motionVmdFiles.size(); i++) {
            StagePack.VmdFileInfo info = motionVmdFiles.get(i);
            int itemY = motionListTop + i * ITEM_HEIGHT - motionScrollOffset;
            if (itemY + ITEM_HEIGHT < motionListTop || itemY > motionListBottom) {
                continue;
            }

            int itemX = panelX + 4;
            int itemW = PANEL_WIDTH - 8;
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW
                    && mouseY >= Math.max(itemY, motionListTop)
                    && mouseY <= Math.min(itemY + ITEM_HEIGHT, motionListBottom);
            if (hovered) {
                hoveredMotionIndex = i;
                g.fill(itemX, itemY, itemX + itemW, itemY + ITEM_HEIGHT, HOVER);
            }

            boolean checked = lobbyViewModel.isLocalCustomMotionSelected(info.name);
            int cbX = itemX + 2;
            int cbY = itemY + 3;
            int cbSize = 8;
            g.fill(cbX, cbY, cbX + cbSize, cbY + cbSize, checked ? CHECKBOX_ON : CHECKBOX_OFF);
            if (checked) {
                g.drawString(font, "✓", cbX + 1, cbY, 0xFFFFFFFF, false);
            }

            String fileName = info.name.toLowerCase().endsWith(".vmd")
                    ? info.name.substring(0, info.name.length() - 4)
                    : info.name;
            g.drawString(font, truncate(fileName, 14), cbX + cbSize + 4, itemY + 4, TEXT, false);

            String typeTag = info.getTypeTag();
            int tagW = font.width(typeTag);
            g.drawString(font, typeTag, itemX + itemW - tagW - 2, itemY + 4, TEXT_DIM, false);
        }
        g.disableScissor();
        renderScrollbar(g, motionListTop, motionListBottom, motionScrollOffset, motionMaxScroll);
    }

    private void renderMemberState(GuiGraphics g, int rightX, int y,
                                   StageMemberState state, boolean useHostCamera) {
        String tag;
        int color;
        if (state == null) {
            tag = "+";
            color = TEXT_DIM;
        } else switch (state) {
            case HOST -> {
                tag = "H";
                color = ACCENT;
            }
            case INVITED -> {
                tag = "...";
                color = STATE_PENDING_COLOR;
            }
            case ACCEPTED -> {
                tag = useHostCamera ? "C" : "✓";
                color = CHECKBOX_ON;
            }
            case READY -> {
                tag = useHostCamera ? "★C" : "★";
                color = CHECKBOX_ON;
            }
            case DECLINED -> {
                tag = "✗";
                color = STATE_DECLINED_COLOR;
            }
            case BUSY -> {
                tag = "!";
                color = STATE_BUSY_COLOR;
            }
            default -> {
                tag = "+";
                color = TEXT_DIM;
            }
        }
        int width = font.width(tag);
        g.drawString(font, tag, rightX - width - 2, y, color, false);
    }

    private void renderScrollbar(GuiGraphics g, int top, int bottom, int offset, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        int barX = panelX + PANEL_WIDTH - 4;
        int barH = bottom - top;
        g.fill(barX, top, barX + 2, bottom, 0x20FFFFFF);
        int thumbH = Math.max(10, barH * barH / (barH + maxScroll));
        int thumbY = top + (int) ((barH - thumbH) * ((float) offset / maxScroll));
        g.fill(barX, thumbY, barX + 2, thumbY + thumbH, ACCENT);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        if (!lobbyViewModel.isSessionMember() && hoverInviteBtn) {
            inviteAllNone();
            return true;
        }

        if (lobbyViewModel.isSessionMember() && hoverGuestCustomToggle) {
            lobbyViewModel.setLocalCustomMotionEnabled(!lobbyViewModel.isLocalCustomMotionEnabled());
            motionScrollOffset = 0;
            updateMotionScroll();
            return true;
        }

        if (hoveredMemberIndex >= 0 && !lobbyViewModel.isSessionMember() && hoveredMemberIndex < hostEntries.size()) {
            StageLobbyViewModel.HostEntry entry = hostEntries.get(hoveredMemberIndex);
            StageMemberState state = entry.state();
            if (state == null || state == StageMemberState.DECLINED || state == StageMemberState.BUSY) {
                lobbyViewModel.sendInvite(entry.uuid());
                return true;
            }
            if (state == StageMemberState.INVITED) {
                lobbyViewModel.cancelInvite(entry.uuid());
                return true;
            }
        }

        if (lobbyViewModel.isSessionMember()
                && lobbyViewModel.isLocalCustomMotionEnabled()
                && hoveredMotionIndex >= 0
                && hoveredMotionIndex < motionVmdFiles.size()) {
            lobbyViewModel.toggleLocalCustomMotion(motionVmdFiles.get(hoveredMotionIndex).name);
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isInside(mouseX, mouseY)) {
            return false;
        }

        int scrollAmount = (int) (-delta * ITEM_HEIGHT * 3);
        if (mouseY <= memberListBottom) {
            memberScrollOffset = Math.max(0, Math.min(memberMaxScroll, memberScrollOffset + scrollAmount));
            return true;
        }

        if (lobbyViewModel.isSessionMember() && lobbyViewModel.isLocalCustomMotionEnabled()) {
            motionScrollOffset = Math.max(0, Math.min(motionMaxScroll, motionScrollOffset + scrollAmount));
            return true;
        }
        return false;
    }

    public boolean isInside(double mouseX, double mouseY) {
        return mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
                && mouseY >= panelY && mouseY <= panelY + panelH;
    }

    private void inviteAllNone() {
        for (StageLobbyViewModel.HostEntry entry : hostEntries) {
            if (entry.state() == null && entry.nearby()) {
                lobbyViewModel.sendInvite(entry.uuid());
            }
        }
    }

    private void updateMemberScroll() {
        int size = lobbyViewModel.isSessionMember() ? guestMembers.size() : hostEntries.size();
        int visibleH = memberListBottom - memberListTop;
        memberMaxScroll = Math.max(0, size * ITEM_HEIGHT - visibleH);
        memberScrollOffset = Math.max(0, Math.min(memberMaxScroll, memberScrollOffset));
    }

    private void updateMotionScroll() {
        if (!lobbyViewModel.isSessionMember() || !lobbyViewModel.isLocalCustomMotionEnabled()) {
            motionMaxScroll = 0;
            motionScrollOffset = 0;
            return;
        }
        int visibleH = motionListBottom - motionListTop;
        motionMaxScroll = Math.max(0, motionVmdFiles.size() * ITEM_HEIGHT - visibleH);
        motionScrollOffset = Math.max(0, Math.min(motionMaxScroll, motionScrollOffset));
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + ".." : s;
    }
}
