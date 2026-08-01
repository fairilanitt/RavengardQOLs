package dev.fairi.ravengardqols.client.feature.rarity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RaritySlotHighlighter {
    private static final int SLOT_SIZE = 16;
    private static boolean enabled = true;

    private RaritySlotHighlighter() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggleEnabled() {
        enabled = !enabled;
    }

    public static void highlightContainerSlots(
        AbstractContainerScreen<?> screen,
        GuiGraphicsExtractor graphics,
        int left,
        int top
    ) {
        if (!enabled) {
            return;
        }

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || !slot.hasItem()) {
                continue;
            }

            ItemStack stack = slot.getItem();
            int x = left + slot.x;
            int y = top + slot.y;
            RarityScanner.detect(stack).ifPresent(rarity -> {
                drawHighlight(graphics, x, y, rarity.borderColor());
                graphics.item(stack, x, y);
                graphics.itemDecorations(Minecraft.getInstance().font, stack, x, y);
            });
        }
    }

    private static void drawHighlight(GuiGraphicsExtractor graphics, int x, int y, int color) {
        int right = x + SLOT_SIZE;
        int bottom = y + SLOT_SIZE;

        graphics.fill(x, y, right, y + 1, color);
        graphics.fill(x, bottom - 1, right, bottom, color);
        graphics.fill(x, y + 1, x + 1, bottom - 1, color);
        graphics.fill(right - 1, y + 1, right, bottom - 1, color);
    }
}
