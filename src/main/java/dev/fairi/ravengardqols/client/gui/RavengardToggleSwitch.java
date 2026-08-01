package dev.fairi.ravengardqols.client.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class RavengardToggleSwitch extends AbstractButton {
    private static final int WIDTH = 40;
    private static final int HEIGHT = 16;
    private static final long MINIMUM_PRESS_NANOS = 110_000_000L;
    private static final int FRAME = 0xFF080C12;
    private static final int TRACK_OFF = 0xFF7B302C;
    private static final int TRACK_OFF_LIGHT = 0xFFB9564B;
    private static final int TRACK_ON = 0xFF35683A;
    private static final int TRACK_ON_LIGHT = 0xFF67A65F;
    private static final int KNOB_DARK = 0xFF8C8778;
    private static final int KNOB = 0xFFE2DCC8;
    private static final int KNOB_LIGHT = 0xFFFFFFFF;

    private final Component label;
    private final BooleanSupplier value;
    private final Runnable toggle;
    private boolean mouseHeld;
    private long pressedUntil;

    RavengardToggleSwitch(int x, int y, Component label, BooleanSupplier value, Runnable toggle) {
        super(x, y, WIDTH, HEIGHT, stateMessage(label, value.getAsBoolean()));
        this.label = label;
        this.value = value;
        this.toggle = toggle;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        toggle.run();
        setMessage(stateMessage(label, value.getAsBoolean()));
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
        boolean enabled = value.getAsBoolean();
        boolean pressed = mouseHeld || System.nanoTime() < pressedUntil;
        int x = getX();
        int y = getY() + (pressed ? 1 : 0);
        int right = x + getWidth();
        int bottom = y + getHeight();
        int track = enabled ? TRACK_ON : TRACK_OFF;
        int trackLight = enabled ? TRACK_ON_LIGHT : TRACK_OFF_LIGHT;

        fillChamfered(graphics, x, y, right, bottom, FRAME);
        fillChamfered(graphics, x + 2, y + 2, right - 2, bottom - 2, track);
        graphics.fill(x + 5, y + 3, right - 5, y + 4, trackLight);

        int knobLeft = enabled ? right - 14 : x + 4;
        graphics.fill(knobLeft + 1, y + 4, knobLeft + 11, bottom - 2, KNOB_DARK);
        graphics.fill(knobLeft, y + 3, knobLeft + 10, bottom - 3, KNOB);
        graphics.fill(knobLeft + 1, y + 4, knobLeft + 9, y + 5, KNOB_LIGHT);
    }

    private static void fillChamfered(
        GuiGraphicsExtractor graphics,
        int left,
        int top,
        int right,
        int bottom,
        int color
    ) {
        graphics.fill(left + 2, top, right - 2, bottom, color);
        graphics.fill(left, top + 2, right, bottom - 2, color);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, color);
    }

    private static Component stateMessage(Component label, boolean enabled) {
        return Component.literal(label.getString() + ": " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
