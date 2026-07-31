package dev.fairi.ravengardqols.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RavengardMainScreen extends Screen {
    private static final int PANEL_WIDTH = 332;
    private static final int PANEL_HEIGHT = 204;

    private static final int SHADOW_FAR = 0x34000000;
    private static final int SHADOW_NEAR = 0x86000000;
    private static final int FRAME_BLACK = 0xFF080C12;
    private static final int FRAME_DARK = 0xFF111A24;
    private static final int FRAME_MID = 0xFF243447;
    private static final int FRAME_LIGHT = 0xFF40556D;
    private static final int SURFACE_DARK = 0xFF111923;
    private static final int SURFACE = 0xFF1A2633;
    private static final int SURFACE_LIGHT = 0xFF2A3A4B;
    private static final int ACCENT_DARK = 0xFF15508A;
    private static final int ACCENT = 0xFF2E86DE;
    private static final int ACCENT_LIGHT = 0xFF66B7FF;
    private static final int GOLD = 0xFFE6B84C;
    private static final int TEXT = 0xFFEAF2FA;
    private static final int TEXT_MUTED = 0xFF91A4B8;
    private static final int COMMON = 0xFFD0D0D0;
    private static final int UNCOMMON = 0xFF168A35;
    private static final int RARE = 0xFF3B82F6;

    private final Screen parent;

    public RavengardMainScreen(Screen parent) {
        super(Component.literal("Ravengard QOL's"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;

        graphics.fill(left + 12, top + 14, left + PANEL_WIDTH + 12, top + PANEL_HEIGHT + 14, SHADOW_FAR);
        graphics.fill(left + 6, top + 8, left + PANEL_WIDTH + 7, top + PANEL_HEIGHT + 9, SHADOW_NEAR);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, FRAME_BLACK);
        graphics.fill(left + 2, top + 2, left + PANEL_WIDTH - 2, top + PANEL_HEIGHT - 2, FRAME_LIGHT);
        graphics.fill(left + 3, top + 3, left + PANEL_WIDTH - 3, top + PANEL_HEIGHT - 3, FRAME_DARK);
        graphics.fill(left + 7, top + 7, left + PANEL_WIDTH - 7, top + PANEL_HEIGHT - 7, SURFACE_DARK);
        graphics.fill(left + 9, top + 9, left + PANEL_WIDTH - 9, top + PANEL_HEIGHT - 9, SURFACE);

        renderHeader(graphics, left, top);
        renderSidebar(graphics, left, top);
        renderFeatureCard(graphics, left, top);
        renderRivets(graphics, left, top);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int left, int top) {
        graphics.fill(left + 9, top + 9, left + PANEL_WIDTH - 9, top + 43, FRAME_BLACK);
        graphics.fill(left + 11, top + 11, left + PANEL_WIDTH - 11, top + 40, FRAME_MID);
        graphics.fill(left + 11, top + 12, left + PANEL_WIDTH - 11, top + 14, FRAME_LIGHT);
        graphics.fill(left + 11, top + 40, left + PANEL_WIDTH - 11, top + 43, ACCENT);
        graphics.fill(left + 11, top + 40, left + PANEL_WIDTH - 75, top + 41, ACCENT_LIGHT);

        graphics.text(font, title, left + 25, top + 22, TEXT, false);
        graphics.fill(left + PANEL_WIDTH - 42, top + 20, left + PANEL_WIDTH - 24, top + 22, GOLD);
        graphics.fill(left + PANEL_WIDTH - 34, top + 13, left + PANEL_WIDTH - 32, top + 29, GOLD);
        graphics.fill(left + PANEL_WIDTH - 37, top + 16, left + PANEL_WIDTH - 29, top + 26, GOLD);
    }

    private void renderSidebar(GuiGraphicsExtractor graphics, int left, int top) {
        int railLeft = left + 9;
        int railTop = top + 43;
        graphics.fill(railLeft, railTop, railLeft + 52, top + PANEL_HEIGHT - 9, FRAME_BLACK);
        graphics.fill(railLeft + 3, railTop + 3, railLeft + 49, top + PANEL_HEIGHT - 12, FRAME_DARK);
        graphics.fill(railLeft + 4, railTop + 4, railLeft + 48, top + PANEL_HEIGHT - 13, FRAME_MID);

        int selectedTop = railTop + 13;
        graphics.fill(railLeft + 6, selectedTop + 3, railLeft + 45, selectedTop + 42, SHADOW_NEAR);
        graphics.fill(railLeft + 6, selectedTop, railLeft + 45, selectedTop + 39, ACCENT_DARK);
        graphics.fill(railLeft + 8, selectedTop + 2, railLeft + 43, selectedTop + 37, ACCENT);
        graphics.fill(railLeft + 10, selectedTop + 4, railLeft + 41, selectedTop + 35, SURFACE_LIGHT);
        graphics.fill(railLeft + 10, selectedTop + 4, railLeft + 41, selectedTop + 6, ACCENT_LIGHT);

        int gearX = railLeft + 25;
        int gearY = selectedTop + 20;
        graphics.fill(gearX - 8, gearY - 2, gearX + 9, gearY + 3, TEXT);
        graphics.fill(gearX - 2, gearY - 8, gearX + 3, gearY + 9, TEXT);
        graphics.fill(gearX - 5, gearY - 5, gearX + 6, gearY + 6, TEXT);
        graphics.fill(gearX - 2, gearY - 2, gearX + 3, gearY + 3, SURFACE_LIGHT);
    }

    private void renderFeatureCard(GuiGraphicsExtractor graphics, int left, int top) {
        int cardLeft = left + 72;
        int cardTop = top + 58;
        int cardRight = left + PANEL_WIDTH - 20;
        int cardBottom = top + PANEL_HEIGHT - 22;

        graphics.fill(cardLeft + 5, cardTop + 6, cardRight + 5, cardBottom + 6, SHADOW_NEAR);
        graphics.fill(cardLeft, cardTop, cardRight, cardBottom, FRAME_BLACK);
        graphics.fill(cardLeft + 2, cardTop + 2, cardRight - 2, cardBottom - 2, FRAME_LIGHT);
        graphics.fill(cardLeft + 3, cardTop + 3, cardRight - 3, cardBottom - 3, SURFACE_DARK);
        graphics.fill(cardLeft + 6, cardTop + 6, cardRight - 6, cardBottom - 6, SURFACE);
        graphics.fill(cardLeft + 6, cardTop + 6, cardRight - 6, cardTop + 9, ACCENT);
        graphics.fill(cardLeft + 7, cardTop + 9, cardRight - 7, cardTop + 10, ACCENT_DARK);

        graphics.text(font, Component.literal("Rarity Highlights"), cardLeft + 14, cardTop + 20, TEXT, false);
        graphics.fill(cardRight - 61, cardTop + 17, cardRight - 13, cardTop + 31, ACCENT_DARK);
        graphics.fill(cardRight - 59, cardTop + 19, cardRight - 15, cardTop + 29, SURFACE_LIGHT);
        graphics.text(font, Component.literal("ACTIVE"), cardRight - 54, cardTop + 20, ACCENT_LIGHT, false);

        int ruleY = cardTop + 40;
        graphics.fill(cardLeft + 14, ruleY, cardRight - 14, ruleY + 1, FRAME_LIGHT);
        renderRarityKey(graphics, cardLeft + 14, cardTop + 52, COMMON, "COMMON");
        renderRarityKey(graphics, cardLeft + 14, cardTop + 75, UNCOMMON, "UNCOMMON");
        renderRarityKey(graphics, cardLeft + 14, cardTop + 98, RARE, "RARE");
    }

    private void renderRarityKey(GuiGraphicsExtractor graphics, int x, int y, int color, String label) {
        graphics.fill(x + 2, y + 3, x + 18, y + 19, SHADOW_NEAR);
        graphics.fill(x, y, x + 18, y + 18, FRAME_BLACK);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, color);
        graphics.fill(x + 4, y + 4, x + 14, y + 14, SURFACE_DARK);
        graphics.fill(x + 5, y + 5, x + 13, y + 6, 0x70FFFFFF);
        graphics.text(font, label, x + 27, y + 5, TEXT_MUTED, false);
    }

    private void renderRivets(GuiGraphicsExtractor graphics, int left, int top) {
        rivet(graphics, left + 8, top + 8);
        rivet(graphics, left + PANEL_WIDTH - 9, top + 8);
        rivet(graphics, left + 8, top + PANEL_HEIGHT - 9);
        rivet(graphics, left + PANEL_WIDTH - 9, top + PANEL_HEIGHT - 9);
    }

    private void rivet(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 3, y + 3, FRAME_BLACK);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, ACCENT_LIGHT);
    }
}
