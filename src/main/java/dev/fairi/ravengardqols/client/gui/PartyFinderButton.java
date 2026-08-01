package dev.fairi.ravengardqols.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class PartyFinderButton extends AbstractButton {
    private static final long PRESS_NANOS = 110_000_000L;
    private static final int SHADOW = 0x85000000;
    private static final int FRAME = 0xFF0A100C;
    private static final int EDGE = 0xFF3C806F;
    private static final int EDGE_HOVER = 0xFF69C0A7;
    private static final int FACE = 0xFF294D3D;
    private static final int FACE_HOVER = 0xFF356653;
    private static final int FACE_DOWN = 0xFF172C24;
    private static final int TEXT = 0xFFE9DDBB;

    private final Runnable action;
    private boolean held;
    private long pressedUntil;

    PartyFinderButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        held = true;
        pressedUntil = System.nanoTime() + PRESS_NANOS;
        super.onClick(event, doubleClick);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        held = false;
        super.onRelease(event);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean pressed = held || System.nanoTime() < pressedUntil;
        int offset = pressed ? 2 : 0;
        int x = getX();
        int y = getY();
        int right = x + getWidth();
        int bottom = y + getHeight();
        graphics.fill(x + 2, y + 3, right + 2, bottom + 3, SHADOW);
        graphics.fill(x, y + offset, right, bottom + offset, FRAME);
        graphics.fill(x + 1, y + 1 + offset, right - 1, bottom - 1 + offset, isHoveredOrFocused() ? EDGE_HOVER : EDGE);
        graphics.fill(x + 2, y + 2 + offset, right - 2, bottom - 2 + offset, pressed ? FACE_DOWN : isHoveredOrFocused() ? FACE_HOVER : FACE);
        if (!pressed) {
            graphics.fill(x + 3, y + 3, right - 3, y + 4, 0x36FFFFFF);
        }

        Font font = Minecraft.getInstance().font;
        int textX = x + (getWidth() - font.width(getMessage())) / 2;
        int textY = y + (getHeight() - font.lineHeight) / 2 + offset;
        graphics.text(font, getMessage(), textX, textY, TEXT, false);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
