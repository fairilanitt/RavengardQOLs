package dev.fairi.ravengardqols.client.gui;

import dev.fairi.ravengardqols.client.feature.inventory.InventoryLedgerPanel;
import dev.fairi.ravengardqols.client.feature.rarity.RaritySlotHighlighter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RavengardMainScreen extends Screen {
    private static final int PANEL_WIDTH = 392;
    private static final int PANEL_HEIGHT = 232;
    private static final int SIDEBAR_WIDTH = 122;

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
    private static final int TEXT = 0xFFEAF2FA;
    private static final int TEXT_MUTED = 0xFF91A4B8;

    private final Screen parent;
    private Category selectedCategory = Category.VISUAL;

    public RavengardMainScreen(Screen parent) {
        super(Component.literal("Ravengard QOL's"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int tabLeft = left;

        addRenderableWidget(
            new RavengardSectionButton(
                tabLeft,
                top + 50,
                132,
                24,
                Component.literal("Visual"),
                selectedCategory == Category.VISUAL,
                () -> selectCategory(Category.VISUAL)
            )
        );
        addRenderableWidget(
            new RavengardSectionButton(
                tabLeft,
                top + 74,
                132,
                24,
                Component.literal("General"),
                selectedCategory == Category.GENERAL,
                () -> selectCategory(Category.GENERAL)
            )
        );

        if (selectedCategory == Category.VISUAL) {
            int cardRight = left + PANEL_WIDTH - 9;
            addRenderableWidget(
                new RavengardToggleSwitch(
                    cardRight - 61,
                    top + 93,
                    Component.literal("Rarity overlays"),
                    RaritySlotHighlighter::isEnabled,
                    RaritySlotHighlighter::toggleEnabled
                )
            );
            addRenderableWidget(
                new RavengardToggleSwitch(
                    cardRight - 61,
                    top + 141,
                    Component.literal("Loot Value"),
                    InventoryLedgerPanel::isEnabled,
                    InventoryLedgerPanel::toggleEnabled
                )
            );
        }
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
        renderSettingsSection(graphics, left, top);
        renderRivets(graphics, left, top);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void selectCategory(Category category) {
        if (selectedCategory != category) {
            selectedCategory = category;
            rebuildWidgets();
        }
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int left, int top) {
        graphics.fill(left + 9, top + 9, left + PANEL_WIDTH - 9, top + 43, FRAME_BLACK);
        graphics.fill(left + 11, top + 11, left + PANEL_WIDTH - 11, top + 40, FRAME_MID);
        graphics.fill(left + 11, top + 12, left + PANEL_WIDTH - 11, top + 14, FRAME_LIGHT);
        graphics.fill(left + 11, top + 40, left + PANEL_WIDTH - 11, top + 43, ACCENT);
        graphics.fill(left + 11, top + 40, left + PANEL_WIDTH - 88, top + 41, ACCENT_LIGHT);
        graphics.text(font, title, left + 25, top + 22, TEXT, false);
    }

    private void renderSettingsSection(GuiGraphicsExtractor graphics, int left, int top) {
        int cardLeft = left + SIDEBAR_WIDTH + 9;
        int cardTop = top + 43;
        int cardRight = left + PANEL_WIDTH - 9;
        int cardBottom = top + PANEL_HEIGHT - 9;

        graphics.fill(cardLeft, cardTop, cardRight, cardBottom, FRAME_BLACK);
        graphics.fill(cardLeft + 2, cardTop + 2, cardRight - 2, cardBottom - 2, FRAME_LIGHT);
        graphics.fill(cardLeft + 3, cardTop + 3, cardRight - 3, cardBottom - 3, SURFACE_DARK);
        graphics.fill(cardLeft + 6, cardTop + 6, cardRight - 6, cardBottom - 6, SURFACE);
        graphics.fill(cardLeft + 6, cardTop + 6, cardRight - 6, cardTop + 9, ACCENT);
        graphics.fill(cardLeft + 7, cardTop + 9, cardRight - 7, cardTop + 10, ACCENT_DARK);

        graphics.text(font, Component.literal(selectedCategory.label), cardLeft + 14, cardTop + 20, TEXT, false);
        graphics.fill(cardLeft + 14, cardTop + 35, cardRight - 14, cardTop + 36, FRAME_LIGHT);

        if (selectedCategory == Category.VISUAL) {
            renderOptionRow(graphics, cardLeft + 12, cardTop + 40, cardRight - 12, "Rarity overlays");
            renderOptionRow(graphics, cardLeft + 12, cardTop + 88, cardRight - 12, "Loot Value");
        }
    }

    private void renderOptionRow(GuiGraphicsExtractor graphics, int left, int top, int right, String label) {
        int bottom = top + 36;
        graphics.fill(left, top, right, bottom, FRAME_BLACK);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, SURFACE_LIGHT);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, SURFACE_DARK);
        graphics.fill(left + 3, top + 3, right - 3, top + 4, 0x3FFFFFFF);
        graphics.fill(left + 3, top + 4, left + 5, bottom - 3, ACCENT_DARK);
        graphics.text(font, Component.literal(label), left + 13, top + 14, TEXT_MUTED, false);
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

    private enum Category {
        VISUAL("Visual"),
        GENERAL("General");

        private final String label;

        Category(String label) {
            this.label = label;
        }
    }
}
