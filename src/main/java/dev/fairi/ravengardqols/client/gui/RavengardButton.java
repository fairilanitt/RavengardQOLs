package dev.fairi.ravengardqols.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class RavengardButton extends AbstractButton {
    private static final long MINIMUM_PRESS_NANOS = 110_000_000L;
    private static final int SHADOW = 0xA0000000;
    private static final int FRAME = 0xFF080C12;
    private static final int EDGE = 0xFF15508A;
    private static final int EDGE_HOVERED = 0xFF66B7FF;
    private static final int FACE = 0xFF243447;
    private static final int FACE_HOVERED = 0xFF30475E;
    private static final int FACE_PRESSED = 0xFF17222E;
    private static final int TEXT = 0xFFEAF2FA;
    private static final int TEXT_HOVERED = 0xFFFFFFFF;

    private final Runnable action;
    private boolean mouseHeld;
    private long pressedUntil;

    public RavengardButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
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
        boolean pressed = mouseHeld || System.nanoTime() < pressedUntil;
        int offset = pressed ? 2 : 0;
        int x = getX();
        int y = getY();
        int right = x + getWidth();
        int bottom = y + getHeight();

        graphics.fill(x + 2, y + 3, right + 2, bottom + 3, SHADOW);
        graphics.fill(x, y + offset, right, bottom + offset, FRAME);
        graphics.fill(x + 1, y + 1 + offset, right - 1, bottom - 1 + offset, isHoveredOrFocused() ? EDGE_HOVERED : EDGE);
        graphics.fill(
            x + 2,
            y + 2 + offset,
            right - 2,
            bottom - 2 + offset,
            pressed ? FACE_PRESSED : isHoveredOrFocused() ? FACE_HOVERED : FACE
        );
        if (!pressed) {
            graphics.fill(x + 3, y + 3, right - 3, y + 4, 0x5CFFFFFF);
        }

        Font font = Minecraft.getInstance().font;
        int textX = x + (getWidth() - font.width(getMessage())) / 2;
        int textY = y + (getHeight() - font.lineHeight) / 2 + offset;
        graphics.text(font, getMessage(), textX, textY, isHoveredOrFocused() ? TEXT_HOVERED : TEXT, false);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
