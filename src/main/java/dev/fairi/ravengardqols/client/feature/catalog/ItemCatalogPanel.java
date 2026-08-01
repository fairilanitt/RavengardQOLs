package dev.fairi.ravengardqols.client.feature.catalog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ItemCatalogPanel {
    private static final int SLOT_SIZE = 18;
    private static final int HEADER_HEIGHT = 19;
    private static final int SEARCH_HEIGHT = 18;
    private static final int BACKGROUND = 0xB0101010;
    private static final int BORDER = 0xFF777777;
    private static final int BORDER_DARK = 0xFF252525;
    private static final int SLOT = 0xAA1C1C1C;
    private static final int SLOT_HOVERED = 0xAAB8B8B8;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFFA0A0A0;

    private static InventoryScreen activeScreen;
    private static EditBox searchBox;
    private static PanelLayout layout;

    private ItemCatalogPanel() {
    }

    public static void render(
        Screen screen,
        GuiGraphicsExtractor graphics,
        int screenLeft,
        int screenTop,
        int imageWidth,
        int imageHeight,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        if (!(screen instanceof InventoryScreen inventoryScreen)) {
            clearIfScreenChanged(screen);
            return;
        }

        PanelLayout currentLayout = calculateLayout(graphics, screenLeft, screenTop, imageWidth, imageHeight);
        layout = currentLayout;
        ensureSearchBox(inventoryScreen, currentLayout);

        ItemCatalogController controller = ItemCatalogController.get();
        controller.setPageSize(currentLayout.capacity());
        Font font = Minecraft.getInstance().font;

        graphics.fill(currentLayout.left(), currentLayout.top(), currentLayout.right(), currentLayout.bottom(), BACKGROUND);
        graphics.fill(currentLayout.left(), currentLayout.top(), currentLayout.right(), currentLayout.top() + 1, BORDER);
        graphics.fill(currentLayout.left(), currentLayout.bottom() - 1, currentLayout.right(), currentLayout.bottom(), BORDER_DARK);

        renderPageControls(graphics, font, currentLayout, controller);
        if (!controller.isConnected()) {
            String message = "Not connected";
            graphics.text(
                font,
                message,
                (currentLayout.left() + currentLayout.right() - font.width(message)) / 2,
                (currentLayout.gridTop() + currentLayout.gridBottom() - font.lineHeight) / 2,
                TEXT_MUTED,
                false
            );
        } else {
            renderItems(graphics, font, currentLayout, controller, mouseX, mouseY);
        }

        searchBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    public static boolean onKeyPressed(Screen screen, KeyEvent event) {
        if (screen != activeScreen || searchBox == null) {
            return false;
        }
        if (event.hasControlDown() && event.key() == GLFW.GLFW_KEY_F) {
            searchBox.setFocused(true);
            return true;
        }
        return searchBox.isFocused() && searchBox.keyPressed(event);
    }

    public static boolean onCharTyped(Screen screen, CharacterEvent event) {
        return screen == activeScreen
            && searchBox != null
            && searchBox.isFocused()
            && searchBox.charTyped(event);
    }

    public static boolean onMouseClicked(Screen screen, MouseButtonEvent event, boolean doubleClick) {
        if (screen != activeScreen || searchBox == null || layout == null) {
            return false;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (layout.previousButton().contains(event.x(), event.y())) {
                ItemCatalogController.get().previousPage();
                return true;
            }
            if (layout.nextButton().contains(event.x(), event.y())) {
                ItemCatalogController.get().nextPage();
                return true;
            }
        }
        if (searchBox.mouseClicked(event, doubleClick)) {
            return true;
        }
        searchBox.setFocused(false);
        return false;
    }

    public static boolean onMouseScrolled(Screen screen, double mouseX, double mouseY, double verticalAmount) {
        if (screen != activeScreen || layout == null || !layout.contains(mouseX, mouseY) || verticalAmount == 0.0D) {
            return false;
        }
        if (verticalAmount > 0.0D) {
            ItemCatalogController.get().previousPage();
        } else {
            ItemCatalogController.get().nextPage();
        }
        return true;
    }

    private static void ensureSearchBox(InventoryScreen screen, PanelLayout currentLayout) {
        if (activeScreen != screen || searchBox == null) {
            activeScreen = screen;
            searchBox = new EditBox(
                Minecraft.getInstance().font,
                currentLayout.left() + 2,
                currentLayout.searchTop(),
                currentLayout.width() - 4,
                SEARCH_HEIGHT - 2,
                Component.literal("Search item catalog")
            );
            searchBox.setMaxLength(64);
            searchBox.setHint(Component.literal("Search..."));
            searchBox.setTextColor(TEXT);
            searchBox.setValue(ItemCatalogController.get().query());
            searchBox.setResponder(ItemCatalogController.get()::setQuery);
        } else {
            searchBox.setRectangle(
                currentLayout.width() - 4,
                SEARCH_HEIGHT - 2,
                currentLayout.left() + 2,
                currentLayout.searchTop()
            );
        }
    }

    private static void renderPageControls(
        GuiGraphicsExtractor graphics,
        Font font,
        PanelLayout currentLayout,
        ItemCatalogController controller
    ) {
        ButtonBounds previous = currentLayout.previousButton();
        ButtonBounds next = currentLayout.nextButton();
        graphics.fill(previous.left(), previous.top(), previous.right(), previous.bottom(), BORDER_DARK);
        graphics.fill(next.left(), next.top(), next.right(), next.bottom(), BORDER_DARK);
        graphics.text(font, "<", previous.left() + 5, previous.top() + 3, TEXT, false);
        graphics.text(font, ">", next.left() + 5, next.top() + 3, TEXT, false);

        String pageText = (controller.page() + 1) + "/" + controller.pages();
        graphics.text(
            font,
            pageText,
            (currentLayout.left() + currentLayout.right() - font.width(pageText)) / 2,
            currentLayout.top() + 5,
            TEXT,
            false
        );
    }

    private static void renderItems(
        GuiGraphicsExtractor graphics,
        Font font,
        PanelLayout currentLayout,
        ItemCatalogController controller,
        int mouseX,
        int mouseY
    ) {
        if (controller.displayItems().isEmpty()) {
            String message = controller.total() == 0 ? "No items found" : "Loading...";
            graphics.text(
                font,
                message,
                (currentLayout.left() + currentLayout.right() - font.width(message)) / 2,
                (currentLayout.gridTop() + currentLayout.gridBottom() - font.lineHeight) / 2,
                TEXT_MUTED,
                false
            );
            return;
        }

        int count = Math.min(currentLayout.capacity(), controller.displayItems().size());
        for (int index = 0; index < count; index++) {
            int column = index % currentLayout.columns();
            int row = index / currentLayout.columns();
            int x = currentLayout.gridLeft() + column * SLOT_SIZE;
            int y = currentLayout.gridTop() + row * SLOT_SIZE;
            CatalogDisplayItem item = controller.displayItems().get(index);
            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? SLOT_HOVERED : SLOT);
            graphics.fill(x, y, x + SLOT_SIZE, y + 1, hovered ? TEXT : BORDER_DARK);
            graphics.fill(x, y, x + 1, y + SLOT_SIZE, hovered ? TEXT : BORDER_DARK);
            graphics.item(item.stack(), x + 1, y + 1);
            if (hovered) {
                graphics.setTooltipForNextFrame(font, item.stack(), mouseX, mouseY);
            }
        }
    }

    private static PanelLayout calculateLayout(
        GuiGraphicsExtractor graphics,
        int screenLeft,
        int screenTop,
        int imageWidth,
        int imageHeight
    ) {
        int screenRight = screenLeft + imageWidth;
        int right = graphics.guiWidth() - 4;
        int left = Math.min(screenRight + 4, right - 54);
        int top = Math.max(4, screenTop);
        int bottom = Math.min(graphics.guiHeight() - 4, screenTop + imageHeight);
        int width = Math.max(54, right - left);
        int columns = Math.max(1, (width - 4) / SLOT_SIZE);
        int gridWidth = columns * SLOT_SIZE;
        int gridLeft = left + (width - gridWidth) / 2;
        int gridTop = top + HEADER_HEIGHT;
        int searchTop = bottom - SEARCH_HEIGHT;
        int rows = Math.max(1, (searchTop - gridTop) / SLOT_SIZE);
        int capacity = Math.min(48, columns * rows);
        return new PanelLayout(left, top, right, bottom, gridLeft, gridTop, searchTop, columns, rows, capacity);
    }

    private static void clearIfScreenChanged(Screen screen) {
        if (activeScreen != null && activeScreen != screen) {
            activeScreen = null;
            searchBox = null;
            layout = null;
        }
    }

    private record PanelLayout(
        int left,
        int top,
        int right,
        int bottom,
        int gridLeft,
        int gridTop,
        int searchTop,
        int columns,
        int rows,
        int capacity
    ) {
        int width() {
            return right - left;
        }

        int gridBottom() {
            return gridTop + rows * SLOT_SIZE;
        }

        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }

        ButtonBounds previousButton() {
            return new ButtonBounds(left + 2, top + 2, left + 16, top + 17);
        }

        ButtonBounds nextButton() {
            return new ButtonBounds(right - 16, top + 2, right - 2, top + 17);
        }
    }

    private record ButtonBounds(int left, int top, int right, int bottom) {
        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }
}
