package dev.fairi.ravengardqols.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class RavengardSectionButton extends AbstractButton {
    private static final long MINIMUM_PRESS_NANOS = 110_000_000L;
    private static final int FRAME = 0xFF080C12;
    private static final int EDGE = 0xFF15508A;
    private static final int EDGE_HOVERED = 0xFF66B7FF;
    private static final int FACE = 0xFF30475E;
    private static final int FACE_HOVERED = 0xFF3A5873;
    private static final int FACE_SELECTED = 0xFF17222E;
    private static final int TEXT = 0xFFEAF2FA;
    private static final int TEXT_SELECTED = 0xFF66B7FF;

    private final boolean selected;
    private final Runnable action;
    private boolean mouseHeld;
    private long pressedUntil;

    RavengardSectionButton(
        int x,
        int y,
        int width,
        int height,
        Component message,
        boolean selected,
        Runnable action
    ) {
        super(x, y, width, height, message);
        this.selected = selected;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        mouseHeld = true;
        pressedUntil = System.nanoTime() + MINIMUM_PRESS_NANOS;
        super.onClick(event, doubleClick);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        mouseHeld = false;
        super.onRelease(event);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean momentarilyPressed = mouseHeld || System.nanoTime() < pressedUntil;
        boolean recessed = selected || momentarilyPressed;
        int faceOffset = !selected && momentarilyPressed ? 1 : 0;
        int x = getX();
        int y = getY();
        int right = x + getWidth();
        int bottom = y + getHeight();

        fillLeftRounded(graphics, x, y, right, bottom, FRAME);
        fillLeftRounded(
            graphics,
            x + 1,
            y + 1,
            right - 1,
            bottom - 1,
            isHoveredOrFocused() ? EDGE_HOVERED : EDGE
        );
        fillLeftRounded(
            graphics,
            x + 2,
            y + 2 + faceOffset,
            right - 2,
            bottom - 2,
            recessed ? FACE_SELECTED : isHoveredOrFocused() ? FACE_HOVERED : FACE
        );

        if (!recessed) {
            graphics.fill(x + 7, y + 3, right - 3, y + 4, 0x5CFFFFFF);
        }

        Font font = Minecraft.getInstance().font;
        int textX = x + (getWidth() - font.width(getMessage())) / 2;
        int textY = y + (getHeight() - font.lineHeight) / 2 + faceOffset;
        graphics.text(font, getMessage(), textX, textY, selected ? TEXT_SELECTED : TEXT, false);
    }

    private static void fillLeftRounded(
        GuiGraphicsExtractor graphics,
        int left,
        int top,
        int right,
        int bottom,
        int color
    ) {
        graphics.fill(left + 5, top, right, bottom, color);
        graphics.fill(left + 3, top + 1, right, bottom - 1, color);
        graphics.fill(left + 1, top + 3, right, bottom - 3, color);
        graphics.fill(left, top + 5, right, bottom - 5, color);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
