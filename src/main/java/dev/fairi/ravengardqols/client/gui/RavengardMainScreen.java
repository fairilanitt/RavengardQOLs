package dev.fairi.ravengardqols.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RavengardMainScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 184;

    private static final int SHADOW = 0x70000000;
    private static final int FRAME_DARK = 0xFF2D2923;
    private static final int FRAME_MID = 0xFF5A4C3B;
    private static final int BRASS_DARK = 0xFF7C5B2D;
    private static final int BRASS = 0xFFC79A55;
    private static final int BRASS_LIGHT = 0xFFE0BB78;
    private static final int PAPER_DARK = 0xFFB6A27F;
    private static final int PAPER = 0xFFD6C6A5;
    private static final int PAPER_LIGHT = 0xFFE8DCBF;
    private static final int INK = 0xFF3B332A;

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

        graphics.fill(left + 6, top + 7, left + PANEL_WIDTH + 6, top + PANEL_HEIGHT + 7, SHADOW);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, FRAME_DARK);
        graphics.fill(left + 2, top + 2, left + PANEL_WIDTH - 2, top + PANEL_HEIGHT - 2, BRASS_DARK);
        graphics.fill(left + 4, top + 4, left + PANEL_WIDTH - 4, top + PANEL_HEIGHT - 4, FRAME_MID);
        graphics.fill(left + 7, top + 7, left + PANEL_WIDTH - 7, top + PANEL_HEIGHT - 7, PAPER_DARK);
        graphics.fill(left + 9, top + 9, left + PANEL_WIDTH - 9, top + PANEL_HEIGHT - 9, PAPER);

        renderHeader(graphics, left, top);
        renderSidebar(graphics, left, top);
        renderBlankWorkspace(graphics, left, top);
        renderRivets(graphics, left, top);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int left, int top) {
        graphics.fill(left + 9, top + 9, left + PANEL_WIDTH - 9, top + 39, FRAME_DARK);
        graphics.fill(left + 11, top + 11, left + PANEL_WIDTH - 11, top + 36, FRAME_MID);
        graphics.fill(left + 11, top + 36, left + PANEL_WIDTH - 11, top + 39, BRASS);

        graphics.text(font, title, left + 24, top + 20, 0xFFF2E5C8, false);
        graphics.fill(left + PANEL_WIDTH - 38, top + 17, left + PANEL_WIDTH - 24, top + 19, BRASS);
        graphics.fill(left + PANEL_WIDTH - 33, top + 12, left + PANEL_WIDTH - 31, top + 24, BRASS);
    }

    private void renderSidebar(GuiGraphicsExtractor graphics, int left, int top) {
        int railLeft = left + 9;
        int railTop = top + 39;
        graphics.fill(railLeft, railTop, railLeft + 48, top + PANEL_HEIGHT - 9, FRAME_DARK);
        graphics.fill(railLeft + 3, railTop + 3, railLeft + 45, top + PANEL_HEIGHT - 12, FRAME_MID);

        int selectedTop = railTop + 13;
        graphics.fill(railLeft + 6, selectedTop, railLeft + 42, selectedTop + 36, BRASS_DARK);
        graphics.fill(railLeft + 8, selectedTop + 2, railLeft + 40, selectedTop + 34, BRASS);
        graphics.fill(railLeft + 11, selectedTop + 5, railLeft + 37, selectedTop + 31, PAPER_LIGHT);

        int gearX = railLeft + 24;
        int gearY = selectedTop + 18;
        graphics.fill(gearX - 8, gearY - 2, gearX + 9, gearY + 3, INK);
        graphics.fill(gearX - 2, gearY - 8, gearX + 3, gearY + 9, INK);
        graphics.fill(gearX - 5, gearY - 5, gearX + 6, gearY + 6, INK);
        graphics.fill(gearX - 2, gearY - 2, gearX + 3, gearY + 3, PAPER_LIGHT);
    }

    private void renderBlankWorkspace(GuiGraphicsExtractor graphics, int left, int top) {
        int contentLeft = left + 65;
        int contentTop = top + 50;
        int contentRight = left + PANEL_WIDTH - 20;
        int contentBottom = top + PANEL_HEIGHT - 20;

        graphics.fill(contentLeft, contentTop, contentRight, contentBottom, PAPER_DARK);
        graphics.fill(contentLeft + 2, contentTop + 2, contentRight - 2, contentBottom - 2, PAPER_LIGHT);
        graphics.fill(contentLeft + 8, contentTop + 15, contentRight - 8, contentTop + 17, BRASS_LIGHT);
        graphics.fill(contentLeft + 8, contentBottom - 17, contentRight - 8, contentBottom - 15, PAPER_DARK);
    }

    private void renderRivets(GuiGraphicsExtractor graphics, int left, int top) {
        rivet(graphics, left + 8, top + 8);
        rivet(graphics, left + PANEL_WIDTH - 9, top + 8);
        rivet(graphics, left + 8, top + PANEL_HEIGHT - 9);
        rivet(graphics, left + PANEL_WIDTH - 9, top + PANEL_HEIGHT - 9);
    }

    private void rivet(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 3, y + 3, FRAME_DARK);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, BRASS_LIGHT);
    }
}
