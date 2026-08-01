package dev.fairi.ravengardqols.client.feature.playerlist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;

public final class NearbyPlayerList {
    private static final int PANEL_WIDTH = 136;
    private static final int HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 15;
    private static final int SCREEN_MARGIN = 8;

    private static final int SHADOW = 0x8A000000;
    private static final int FRAME_BLACK = 0xFF0C100A;
    private static final int BRASS_DARK = 0xFF614A26;
    private static final int BRASS = 0xFFB18A47;
    private static final int WOOD_DARK = 0xFF24170F;
    private static final int WOOD = 0xFF3B2818;
    private static final int WOOD_LIGHT = 0xFF5A3F24;
    private static final int GREEN_DARK = 0xFF29412B;
    private static final int GREEN = 0xFF4F7748;
    private static final int GREEN_LIGHT = 0xFF8BB478;
    private static final int PARCHMENT = 0xFFE4D5AD;
    private static final int PARCHMENT_MUTED = 0xFFB8AA87;

    private NearbyPlayerList() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (
            minecraft.level == null
                || minecraft.player == null
                || minecraft.gui.screen() != null
                || !minecraft.options.keyPlayerList.isDown()
        ) {
            return;
        }

        List<PlayerEntry> players = collectNearbyPlayers(minecraft);
        if (players.isEmpty()) {
            return;
        }

        Font font = minecraft.font;
        int availableRows = Math.max(1, (graphics.guiHeight() - 48 - HEADER_HEIGHT) / ROW_HEIGHT);
        int visibleRows = Math.min(players.size(), availableRows);
        int panelHeight = HEADER_HEIGHT + visibleRows * ROW_HEIGHT + 5;
        int left = Math.max(SCREEN_MARGIN, graphics.guiWidth() / 16);
        int right = left + PANEL_WIDTH;
        int top = Math.max(SCREEN_MARGIN, (graphics.guiHeight() - panelHeight) / 2);
        int bottom = top + panelHeight;

        renderFrame(graphics, font, left, top, right, bottom);
        for (int index = 0; index < visibleRows; index++) {
            renderPlayerRow(graphics, font, left, right, top + HEADER_HEIGHT + index * ROW_HEIGHT, players.get(index), index);
        }
    }

    private static List<PlayerEntry> collectNearbyPlayers(Minecraft minecraft) {
        double renderRadius = minecraft.options.renderDistance().get() * 16.0;
        double maximumDistanceSquared = renderRadius * renderRadius;
        List<PlayerEntry> entries = new ArrayList<>();

        for (AbstractClientPlayer player : minecraft.level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (player.distanceToSqr(minecraft.player) > maximumDistanceSquared) {
                continue;
            }
            entries.add(parsePlayer(player.getDisplayName().getString(), player == minecraft.player));
        }

        entries.sort(
            Comparator.comparing(PlayerEntry::local).reversed()
                .thenComparing(PlayerEntry::name, String.CASE_INSENSITIVE_ORDER)
        );
        return List.copyOf(entries);
    }

    private static PlayerEntry parsePlayer(String displayName, boolean local) {
        PlayerLevelParser.ParsedPlayer parsed = PlayerLevelParser.parse(displayName);
        String level = parsed.level().isPresent() ? String.valueOf(parsed.level().getAsInt()) : "?";
        return new PlayerEntry(parsed.name(), level, local);
    }

    private static void renderFrame(
        GuiGraphicsExtractor graphics,
        Font font,
        int left,
        int top,
        int right,
        int bottom
    ) {
        graphics.fill(left + 4, top + 5, right + 4, bottom + 5, SHADOW);
        graphics.fill(left, top, right, bottom, FRAME_BLACK);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, BRASS_DARK);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, WOOD_DARK);
        graphics.fill(left + 4, top + 4, right - 4, bottom - 4, WOOD);

        graphics.fill(left + 4, top + 4, right - 4, top + HEADER_HEIGHT - 2, GREEN_DARK);
        graphics.fill(left + 5, top + 5, right - 5, top + 7, GREEN);
        graphics.fill(left + 4, top + HEADER_HEIGHT - 3, right - 4, top + HEADER_HEIGHT - 2, BRASS);

        String title = "Nearby Players";
        graphics.text(font, title, (left + right - font.width(title)) / 2, top + 10, PARCHMENT, false);

        rivet(graphics, left + 4, top + 4);
        rivet(graphics, right - 5, top + 4);
        rivet(graphics, left + 4, bottom - 5);
        rivet(graphics, right - 5, bottom - 5);
    }

    private static void renderPlayerRow(
        GuiGraphicsExtractor graphics,
        Font font,
        int left,
        int right,
        int top,
        PlayerEntry player,
        int index
    ) {
        int rowLeft = left + 5;
        int rowRight = right - 5;
        int rowBottom = top + ROW_HEIGHT;
        int background = player.local() ? GREEN_DARK : index % 2 == 0 ? WOOD_DARK : WOOD;

        graphics.fill(rowLeft, top, rowRight, rowBottom, background);
        graphics.fill(rowLeft + 1, top + 1, rowLeft + 3, rowBottom - 1, GREEN);
        graphics.fill(rowLeft + 4, top + 1, rowRight - 1, top + 2, WOOD_LIGHT);

        String level = player.level();
        int levelWidth = font.width(level);
        int levelX = rowRight - levelWidth - 4;
        int nameWidth = Math.max(12, levelX - rowLeft - 12);
        String name = font.plainSubstrByWidth(player.name(), nameWidth);

        graphics.text(font, name, rowLeft + 7, top + 4, player.local() ? PARCHMENT : PARCHMENT_MUTED, false);
        graphics.text(font, level, levelX, top + 4, GREEN_LIGHT, false);
    }

    private static void rivet(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 2, y + 2, FRAME_BLACK);
        graphics.fill(x, y, x + 1, y + 1, BRASS);
    }

    private record PlayerEntry(String name, String level, boolean local) {
    }
}
