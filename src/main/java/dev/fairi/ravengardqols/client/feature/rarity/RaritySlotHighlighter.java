package dev.fairi.ravengardqols.client.feature.rarity;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class RaritySlotHighlighter {
    private static final int SLOT_SIZE = 16;
    private static final int SEPARATION_EDGE = 0xB0000000;
    private static boolean enabled = true;

    private RaritySlotHighlighter() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggleEnabled() {
        enabled = !enabled;
    }

    public static void highlightContainerSlots(ScreenEvent.Render.Foreground event) {
        if (!enabled || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int left = screen.getLeftPos();
        int top = screen.getTopPos();

        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && slot.hasItem()) {
                highlightStack(graphics, slot.getItem(), left + slot.x, top + slot.y);
            }
        }
    }

    private static void highlightStack(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        RarityScanner.detect(stack).ifPresent(rarity -> drawHighlight(graphics, x, y, rarity));
    }

    private static void drawHighlight(GuiGraphicsExtractor graphics, int x, int y, ItemRarity rarity) {
        int right = x + SLOT_SIZE;
        int bottom = y + SLOT_SIZE;

        graphics.fill(x - 2, y - 2, right + 2, y - 1, SEPARATION_EDGE);
        graphics.fill(x - 2, bottom + 1, right + 2, bottom + 2, SEPARATION_EDGE);
        graphics.fill(x - 2, y - 1, x - 1, bottom + 1, SEPARATION_EDGE);
        graphics.fill(right + 1, y - 1, right + 2, bottom + 1, SEPARATION_EDGE);

        graphics.fill(x - 1, y - 1, right + 1, y, rarity.borderColor());
        graphics.fill(x - 1, bottom, right + 1, bottom + 1, rarity.borderColor());
        graphics.fill(x - 1, y, x, bottom, rarity.borderColor());
        graphics.fill(right, y, right + 1, bottom, rarity.borderColor());
    }
}
