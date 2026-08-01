package dev.fairi.ravengardqols.client.gui;

import dev.fairi.ravengardqols.client.feature.inspector.ItemInspection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class ItemInspectorScreen extends Screen {
    private static final int FRAME_BLACK = 0xFF080C12;
    private static final int FRAME_DARK = 0xFF111A24;
    private static final int FRAME_LIGHT = 0xFF40556D;
    private static final int SURFACE = 0xFF1A2633;
    private static final int ACCENT = 0xFF2E86DE;
    private static final int ACCENT_LIGHT = 0xFF66B7FF;
    private static final int TEXT = 0xFFEAF2FA;
    private static final int TEXT_MUTED = 0xFF91A4B8;

    private final ItemStack stack;
    private final Screen parent;
    private ItemInspection inspection;
    private List<FormattedCharSequence> wrappedLines = List.of();
    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    private int contentTop;
    private int contentBottom;
    private int visibleLineCount;
    private int scrollOffset;
    private RavengardButton copyButton;

    public ItemInspectorScreen(ItemStack stack) {
        this(null, stack);
    }

    public ItemInspectorScreen(Screen parent, ItemStack stack) {
        super(Component.literal("Item Component Inspector"));
        this.parent = parent;
        this.stack = stack.copy();
    }

    @Override
    public void onClose() {
        if (parent != null) {
            minecraft.gui.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    protected void init() {
        inspection = ItemInspection.inspect(minecraft, stack);
        int panelWidth = Math.min(width - 24, 520);
        int panelHeight = Math.min(height - 20, 360);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        panelRight = panelLeft + panelWidth;
        panelBottom = panelTop + panelHeight;
        contentTop = panelTop + 44;
        contentBottom = panelBottom - 34;
        visibleLineCount = Math.max(1, (contentBottom - contentTop) / font.lineHeight);
        wrappedLines = buildWrappedLines(panelWidth - 34);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());

        copyButton = addRenderableWidget(
            new RavengardButton(panelRight - 150, panelBottom - 27, 68, 18, Component.literal("COPY ALL"), this::copyAll)
        );
        addRenderableWidget(
            new RavengardButton(panelRight - 76, panelBottom - 27, 62, 18, Component.literal("BACK"), this::onClose)
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelLeft + 8, panelTop + 9, panelRight + 8, panelBottom + 9, 0x78000000);
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, FRAME_BLACK);
        graphics.fill(panelLeft + 2, panelTop + 2, panelRight - 2, panelBottom - 2, FRAME_LIGHT);
        graphics.fill(panelLeft + 3, panelTop + 3, panelRight - 3, panelBottom - 3, FRAME_DARK);
        graphics.fill(panelLeft + 7, panelTop + 7, panelRight - 7, panelBottom - 7, SURFACE);

        graphics.fill(panelLeft + 7, panelTop + 7, panelRight - 7, panelTop + 38, FRAME_DARK);
        graphics.fill(panelLeft + 7, panelTop + 37, panelRight - 7, panelTop + 40, ACCENT);
        graphics.fill(panelLeft + 7, panelTop + 37, panelRight - 74, panelTop + 38, ACCENT_LIGHT);
        graphics.text(font, title, panelLeft + 15, panelTop + 17, TEXT, false);
        graphics.text(
            font,
            inspection.components().size() + " COMPONENTS",
            panelRight - 15 - font.width(inspection.components().size() + " COMPONENTS"),
            panelTop + 17,
            TEXT_MUTED,
            false
        );

        graphics.enableScissor(panelLeft + 12, contentTop, panelRight - 12, contentBottom);
        int lastLine = Math.min(wrappedLines.size(), scrollOffset + visibleLineCount);
        for (int line = scrollOffset; line < lastLine; line++) {
            int y = contentTop + (line - scrollOffset) * font.lineHeight;
            graphics.text(font, wrappedLines.get(line), panelLeft + 15, y, TEXT, false);
        }
        graphics.disableScissor();
        renderScrollbar(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelLeft && mouseX < panelRight && mouseY >= contentTop && mouseY < contentBottom) {
            int direction = (int) Math.signum(scrollY);
            scrollOffset = Mth.clamp(scrollOffset - direction * 3, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private List<FormattedCharSequence> buildWrappedLines(int lineWidth) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("ITEM: ").withColor(ACCENT_LIGHT).append(Component.literal(inspection.itemName()).withColor(TEXT)));
        lines.add(Component.literal("ID: ").withColor(ACCENT_LIGHT).append(Component.literal(inspection.itemId()).withColor(TEXT_MUTED)));
        lines.add(Component.literal("COUNT: ").withColor(ACCENT_LIGHT).append(Component.literal(String.valueOf(inspection.count())).withColor(TEXT_MUTED)));
        lines.add(
            Component.literal("DETECTED RARITY: ").withColor(ACCENT_LIGHT)
                .append(Component.literal(inspection.detectedRarity()).withColor(TEXT))
        );
        lines.add(
            Component.literal("DETECTION SOURCE: ").withColor(ACCENT_LIGHT)
                .append(Component.literal(inspection.detectionSource()).withColor(TEXT_MUTED))
        );
        lines.add(
            Component.literal("SELL PRICE: ").withColor(ACCENT_LIGHT)
                .append(Component.literal(inspection.sellPrice()).withColor(TEXT_MUTED))
        );
        lines.add(Component.literal(" "));

        for (ItemInspection.ComponentEntry component : inspection.components()) {
            lines.add(
                Component.literal(component.id() + " = ").withColor(ACCENT_LIGHT)
                    .append(Component.literal(component.value()).withColor(TEXT_MUTED))
            );
        }

        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(font.split(line, lineWidth));
        }
        return List.copyOf(wrapped);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        if (wrappedLines.size() <= visibleLineCount) {
            return;
        }
        int trackTop = contentTop;
        int trackBottom = contentBottom;
        int trackHeight = trackBottom - trackTop;
        int thumbHeight = Math.max(12, trackHeight * visibleLineCount / wrappedLines.size());
        int travel = trackHeight - thumbHeight;
        int thumbTop = trackTop + travel * scrollOffset / maxScroll();
        graphics.fill(panelRight - 10, trackTop, panelRight - 7, trackBottom, FRAME_BLACK);
        graphics.fill(panelRight - 10, thumbTop, panelRight - 7, thumbTop + thumbHeight, ACCENT_LIGHT);
    }

    private int maxScroll() {
        return Math.max(0, wrappedLines.size() - visibleLineCount);
    }

    private void copyAll() {
        minecraft.keyboardHandler.setClipboard(inspection.clipboardText());
        copyButton.setMessage(Component.literal("COPIED"));
    }
}
