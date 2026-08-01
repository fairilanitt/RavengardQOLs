package dev.fairi.ravengardqols.client.feature.inventory;

import dev.fairi.ravengardqols.client.feature.rarity.ItemRarity;
import dev.fairi.ravengardqols.client.feature.rarity.RarityScanner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class InventoryLedgerPanel {
    private static final int IDEAL_WIDTH = 146;
    private static final int MINIMUM_WIDTH = 104;
    private static final int ROW_HEIGHT = 23;
    private static final int HEADER_HEIGHT = 35;
    private static final int FOOTER_HEIGHT = 20;

    private static final int SHADOW = 0xA0000000;
    private static final int IRON_BLACK = 0xFF090806;
    private static final int IRON = 0xFF29261F;
    private static final int IRON_LIGHT = 0xFF514A3A;
    private static final int WOOD_DARK = 0xFF1A100B;
    private static final int WOOD = 0xFF302014;
    private static final int WOOD_LIGHT = 0xFF49311D;
    private static final int GOLD_DARK = 0xFF6F4814;
    private static final int GOLD = 0xFFC59535;
    private static final int GOLD_LIGHT = 0xFFF1D27A;
    private static final int PARCHMENT = 0xFFE7D3A2;
    private static final int PARCHMENT_MUTED = 0xFFBFAE84;

    private static final Identifier COMMON_ICON = texture("common.png");
    private static final Identifier UNCOMMON_ICON = texture("uncommon.png");
    private static final Identifier RARE_ICON = texture("rare.png");
    private static final Identifier CROWN_ICON = texture("crown.png");

    private static AbstractContainerScreen<?> activeScreen;
    private static PanelBounds panelBounds;
    private static int scrollOffset;
    private static int maximumScroll;
    private static boolean enabled;

    private InventoryLedgerPanel() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggleEnabled() {
        enabled = !enabled;
        if (!enabled) {
            activeScreen = null;
            panelBounds = null;
            scrollOffset = 0;
            maximumScroll = 0;
        }
    }

    public static void render(ScreenEvent.Render.Foreground event) {
        if (!enabled || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        if (activeScreen != screen) {
            activeScreen = screen;
            scrollOffset = 0;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        List<ListedItem> items = collectItems(screen);
        PanelBounds bounds = calculateBounds(graphics, screen);
        panelBounds = bounds;

        int contentTop = bounds.top() + HEADER_HEIGHT;
        int contentBottom = bounds.bottom() - FOOTER_HEIGHT;
        int visibleRows = Math.max(1, (contentBottom - contentTop) / ROW_HEIGHT);
        maximumScroll = Math.max(0, items.size() - visibleRows);
        scrollOffset = Mth.clamp(scrollOffset, 0, maximumScroll);

        renderFrame(graphics, font, bounds, items.size());
        graphics.enableScissor(bounds.left() + 5, contentTop, bounds.right() - 5, contentBottom);
        int lastItem = Math.min(items.size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < lastItem; index++) {
            int rowTop = contentTop + (index - scrollOffset) * ROW_HEIGHT;
            renderItemRow(graphics, font, bounds, rowTop, items.get(index));
        }
        graphics.disableScissor();

        renderFooter(graphics, font, bounds, items);
        renderScrollbar(graphics, bounds, contentTop, contentBottom, visibleRows, items.size());
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!enabled || event.getScreen() != activeScreen || panelBounds == null || maximumScroll == 0) {
            return;
        }
        if (!panelBounds.contains(event.getMouseX(), event.getMouseY())) {
            return;
        }

        int direction = (int) Math.signum(event.getScrollDeltaY());
        scrollOffset = Mth.clamp(scrollOffset - direction * 2, 0, maximumScroll);
        event.setCanceled(true);
    }

    private static List<ListedItem> collectItems(AbstractContainerScreen<?> screen) {
        List<ListedItem> items = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || !slot.hasItem()) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (stack.is(Items.NETHER_STAR)) {
                continue;
            }
            RarityScanner.detect(stack).ifPresent(rarity ->
                items.add(new ListedItem(stack, rarity, SellPriceScanner.detect(stack), slot.index))
            );
        }

        items.sort(
            Comparator.comparingInt((ListedItem item) -> item.rarity().sortPriority()).reversed()
                .thenComparing((ListedItem item) -> item.sellPrice().orElse(0L), Comparator.reverseOrder())
                .thenComparing(item -> item.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(ListedItem::slotIndex)
        );
        return List.copyOf(items);
    }

    private static PanelBounds calculateBounds(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen) {
        int screenRight = screen.getLeftPos() + screen.getImageWidth();
        int availableRight = graphics.guiWidth() - screenRight - 6;
        int panelWidth = availableRight >= MINIMUM_WIDTH
            ? Math.min(IDEAL_WIDTH, availableRight)
            : Math.min(132, graphics.guiWidth() - 12);
        int right = availableRight >= MINIMUM_WIDTH ? screenRight + 5 + panelWidth : graphics.guiWidth() - 6;
        int left = right - panelWidth;
        int top = Math.max(5, screen.getTopPos());
        int bottom = Math.min(graphics.guiHeight() - 5, screen.getTopPos() + screen.getImageHeight());
        if (bottom - top < 92) {
            top = 5;
            bottom = graphics.guiHeight() - 5;
        }
        return new PanelBounds(left, top, right, bottom);
    }

    private static void renderFrame(GuiGraphicsExtractor graphics, Font font, PanelBounds bounds, int itemCount) {
        graphics.fill(bounds.left() + 7, bounds.top() + 8, bounds.right() + 7, bounds.bottom() + 8, SHADOW);
        graphics.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), IRON_BLACK);
        graphics.fill(bounds.left() + 2, bounds.top() + 2, bounds.right() - 2, bounds.bottom() - 2, GOLD_DARK);
        graphics.fill(bounds.left() + 3, bounds.top() + 3, bounds.right() - 3, bounds.bottom() - 3, GOLD);
        graphics.fill(bounds.left() + 5, bounds.top() + 5, bounds.right() - 5, bounds.bottom() - 5, IRON);
        graphics.fill(bounds.left() + 7, bounds.top() + 7, bounds.right() - 7, bounds.bottom() - 7, WOOD_DARK);

        graphics.fill(bounds.left() + 7, bounds.top() + 7, bounds.right() - 7, bounds.top() + HEADER_HEIGHT - 2, WOOD);
        graphics.fill(bounds.left() + 8, bounds.top() + 8, bounds.right() - 8, bounds.top() + 10, WOOD_LIGHT);
        graphics.fill(bounds.left() + 7, bounds.top() + HEADER_HEIGHT - 4, bounds.right() - 7, bounds.top() + HEADER_HEIGHT - 2, GOLD);
        graphics.fill(bounds.left() + 8, bounds.top() + HEADER_HEIGHT - 4, bounds.right() - 35, bounds.top() + HEADER_HEIGHT - 3, GOLD_LIGHT);

        renderTexture(graphics, CROWN_ICON, (bounds.left() + bounds.right()) / 2 - 5, bounds.top() + 7, 10, 12);
        String title = "Loot Value";
        graphics.text(font, title, (bounds.left() + bounds.right() - font.width(title)) / 2, bounds.top() + 20, PARCHMENT, false);
        String count = String.valueOf(itemCount);
        graphics.text(font, count, bounds.right() - 11 - font.width(count), bounds.top() + 20, GOLD_LIGHT, false);

        rivet(graphics, bounds.left() + 5, bounds.top() + 5);
        rivet(graphics, bounds.right() - 6, bounds.top() + 5);
        rivet(graphics, bounds.left() + 5, bounds.bottom() - 6);
        rivet(graphics, bounds.right() - 6, bounds.bottom() - 6);
    }

    private static void renderItemRow(GuiGraphicsExtractor graphics, Font font, PanelBounds bounds, int rowTop, ListedItem item) {
        int left = bounds.left() + 7;
        int right = bounds.right() - 7;
        graphics.fill(left + 2, rowTop + 3, right + 2, rowTop + ROW_HEIGHT + 1, 0x72000000);
        graphics.fill(left, rowTop + 1, right, rowTop + ROW_HEIGHT - 1, IRON_BLACK);
        graphics.fill(left + 1, rowTop + 2, right - 1, rowTop + ROW_HEIGHT - 2, WOOD);
        graphics.fill(left + 1, rowTop + 2, right - 1, rowTop + ROW_HEIGHT - 2, item.rarity().fillColor());
        graphics.fill(left + 2, rowTop + 3, left + 4, rowTop + ROW_HEIGHT - 3, item.rarity().borderColor());
        graphics.fill(left + 4, rowTop + 3, right - 2, rowTop + 4, WOOD_LIGHT);

        graphics.item(item.stack(), left + 6, rowTop + 3);
        graphics.itemDecorations(font, item.stack(), left + 6, rowTop + 3);

        int textLeft = left + 26;
        int textWidth = Math.max(12, right - textLeft - 3);
        String itemName = font.plainSubstrByWidth(item.stack().getHoverName().getString(), textWidth);
        graphics.text(font, itemName, textLeft, rowTop + 3, item.rarity().borderColor(), false);

        int detailY = rowTop + 12;
        renderTexture(graphics, rarityIcon(item.rarity()), textLeft, detailY, 9, 16);
        if (item.sellPrice().isPresent()) {
            int crownX = textLeft + 12;
            renderTexture(graphics, CROWN_ICON, crownX, detailY, 9, 12);
            String price = String.valueOf(item.sellPrice().getAsLong());
            price = font.plainSubstrByWidth(price, Math.max(8, right - crownX - 14));
            graphics.text(font, price, crownX + 11, rowTop + 13, GOLD_LIGHT, false);
        }
    }

    private static void renderFooter(GuiGraphicsExtractor graphics, Font font, PanelBounds bounds, List<ListedItem> items) {
        int footerTop = bounds.bottom() - FOOTER_HEIGHT;
        graphics.fill(bounds.left() + 7, footerTop, bounds.right() - 7, bounds.bottom() - 7, WOOD);
        graphics.fill(bounds.left() + 7, footerTop, bounds.right() - 7, footerTop + 2, GOLD_DARK);

        long totalValue = 0L;
        for (ListedItem item : items) {
            if (item.sellPrice().isPresent()) {
                totalValue += item.sellPrice().getAsLong() * item.stack().getCount();
            }
        }
        String label = "TOTAL";
        int labelX = bounds.left() + 11;
        graphics.text(font, label, labelX, footerTop + 7, PARCHMENT_MUTED, false);
        int crownX = labelX + font.width(label) + 4;
        renderTexture(graphics, CROWN_ICON, crownX, footerTop + 5, 9, 12);
        String total = font.plainSubstrByWidth(String.valueOf(totalValue), Math.max(8, bounds.right() - crownX - 18));
        graphics.text(font, total, crownX + 11, footerTop + 7, GOLD_LIGHT, false);
    }

    private static void renderScrollbar(
        GuiGraphicsExtractor graphics,
        PanelBounds bounds,
        int contentTop,
        int contentBottom,
        int visibleRows,
        int itemCount
    ) {
        if (itemCount <= visibleRows) {
            return;
        }
        int trackHeight = contentBottom - contentTop;
        int thumbHeight = Math.max(10, trackHeight * visibleRows / itemCount);
        int travel = trackHeight - thumbHeight;
        int thumbTop = contentTop + travel * scrollOffset / maximumScroll;
        graphics.fill(bounds.right() - 8, contentTop, bounds.right() - 6, contentBottom, IRON_BLACK);
        graphics.fill(bounds.right() - 8, thumbTop, bounds.right() - 6, thumbTop + thumbHeight, GOLD_LIGHT);
    }

    private static Identifier rarityIcon(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> COMMON_ICON;
            case UNCOMMON -> UNCOMMON_ICON;
            case RARE -> RARE_ICON;
        };
    }

    private static void renderTexture(
        GuiGraphicsExtractor graphics,
        Identifier texture,
        int x,
        int y,
        int size,
        int sourceSize
    ) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, sourceSize, sourceSize, sourceSize, sourceSize);
    }

    private static Identifier texture(String fileName) {
        return Identifier.fromNamespaceAndPath("ravengardqols", "textures/gui/loot/" + fileName);
    }

    private static void rivet(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 2, y + 2, IRON_BLACK);
        graphics.fill(x, y, x + 1, y + 1, GOLD_LIGHT);
    }

    private record ListedItem(ItemStack stack, ItemRarity rarity, OptionalLong sellPrice, int slotIndex) {
    }

    private record PanelBounds(int left, int top, int right, int bottom) {
        int width() {
            return right - left;
        }

        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }
}
