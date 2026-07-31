package dev.fairi.ravengardqols.client.feature.rarity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class RaritySlotHighlighter {
    private static final int SLOT_SIZE = 16;
    private static final int SEPARATION_EDGE = 0xB0000000;
    private static final int INNER_GLEAM = 0x4CFFFFFF;

    private RaritySlotHighlighter() {
    }

    public static void highlightContainerSlots(ScreenEvent.Render.Foreground event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
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

    public static void highlightHotbarSlots(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player.isSpectator()) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int left = graphics.guiWidth() / 2 - 90;
        int top = graphics.guiHeight() - 19;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            highlightStack(graphics, stack, left + slot * 20 + 2, top);
        }
    }

    private static void highlightStack(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        RarityScanner.detect(stack).ifPresent(rarity -> drawHighlight(graphics, x, y, rarity));
    }

    private static void drawHighlight(GuiGraphicsExtractor graphics, int x, int y, ItemRarity rarity) {
        int right = x + SLOT_SIZE;
        int bottom = y + SLOT_SIZE;

        graphics.fill(x - 1, y - 1, right + 1, bottom + 1, SEPARATION_EDGE);
        graphics.fill(x, y, right, bottom, rarity.fillColor());
        graphics.fill(x, y, right, y + 1, rarity.borderColor());
        graphics.fill(x, bottom - 1, right, bottom, rarity.borderColor());
        graphics.fill(x, y + 1, x + 1, bottom - 1, rarity.borderColor());
        graphics.fill(right - 1, y + 1, right, bottom - 1, rarity.borderColor());
        graphics.fill(x + 1, y + 1, right - 1, y + 2, INNER_GLEAM);
        graphics.fill(x + 1, y + 2, x + 2, bottom - 1, INNER_GLEAM);
    }
}
