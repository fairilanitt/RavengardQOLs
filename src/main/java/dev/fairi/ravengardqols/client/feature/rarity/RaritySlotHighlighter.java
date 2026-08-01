package dev.fairi.ravengardqols.client.feature.rarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class RaritySlotHighlighter {
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_PITCH = 18;
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
        List<HighlightedSlot> highlightedSlots = new ArrayList<>();
        Map<Long, HighlightedSlot> slotsByPosition = new HashMap<>();

        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && slot.hasItem()) {
                int x = left + slot.x;
                int y = top + slot.y;
                RarityScanner.detect(slot.getItem()).ifPresent(rarity -> {
                    HighlightedSlot highlightedSlot = new HighlightedSlot(x, y, rarity);
                    highlightedSlots.add(highlightedSlot);
                    slotsByPosition.put(positionKey(x, y), highlightedSlot);
                });
            }
        }

        for (HighlightedSlot slot : highlightedSlots) {
            HighlightedSlot leftNeighbor = slotsByPosition.get(positionKey(slot.x() - SLOT_PITCH, slot.y()));
            HighlightedSlot rightNeighbor = slotsByPosition.get(positionKey(slot.x() + SLOT_PITCH, slot.y()));
            HighlightedSlot topNeighbor = slotsByPosition.get(positionKey(slot.x(), slot.y() - SLOT_PITCH));
            HighlightedSlot bottomNeighbor = slotsByPosition.get(positionKey(slot.x(), slot.y() + SLOT_PITCH));

            drawHighlight(
                graphics,
                slot,
                leftNeighbor,
                rightNeighbor,
                topNeighbor,
                bottomNeighbor
            );
        }
    }

    private static void drawHighlight(
        GuiGraphicsExtractor graphics,
        HighlightedSlot slot,
        HighlightedSlot leftNeighbor,
        HighlightedSlot rightNeighbor,
        HighlightedSlot topNeighbor,
        HighlightedSlot bottomNeighbor
    ) {
        int x = slot.x();
        int y = slot.y();
        int right = x + SLOT_SIZE;
        int bottom = y + SLOT_SIZE;
        int color = slot.rarity().borderColor();
        boolean sameRight = hasSameRarity(slot, rightNeighbor);
        boolean sameBottom = hasSameRarity(slot, bottomNeighbor);

        if (topNeighbor == null) {
            graphics.fill(x - 2, y - 2, right + 2, y - 1, SEPARATION_EDGE);
        }
        if (bottomNeighbor == null) {
            graphics.fill(x - 2, bottom + 1, right + 2, bottom + 2, SEPARATION_EDGE);
        }
        if (leftNeighbor == null) {
            graphics.fill(x - 2, y - 1, x - 1, bottom + 1, SEPARATION_EDGE);
        }
        if (rightNeighbor == null) {
            graphics.fill(right + 1, y - 1, right + 2, bottom + 1, SEPARATION_EDGE);
        }

        graphics.fill(x - 1, y - 1, right + 1, y, color);
        graphics.fill(x - 1, y, x, bottom, color);
        if (!sameBottom) {
            graphics.fill(x - 1, bottom, right + 1, bottom + 1, color);
        }
        if (!sameRight) {
            graphics.fill(right, y, right + 1, bottom, color);
        }
    }

    private static boolean hasSameRarity(HighlightedSlot slot, HighlightedSlot neighbor) {
        return neighbor != null && neighbor.rarity() == slot.rarity();
    }

    private static long positionKey(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }

    private record HighlightedSlot(int x, int y, ItemRarity rarity) {
    }
}
