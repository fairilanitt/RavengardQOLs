package dev.fairi.ravengardqols.client.gui;

import dev.fairi.ravengardqols.client.feature.party.PartyFinderController;
import dev.fairi.ravengardqols.client.feature.party.PartyListing;
import dev.fairi.ravengardqols.client.feature.party.PartyRoster;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PartyFinderScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 264;
    private static final int SIDEBAR_WIDTH = 126;
    private static final int ROW_HEIGHT = 43;
    private static final int PAGE_SIZE = 3;

    private static final int SHADOW = 0x96000000;
    private static final int FRAME = 0xFF090E0B;
    private static final int BRASS_DARK = 0xFF604A29;
    private static final int BRASS = 0xFFA78243;
    private static final int BRASS_LIGHT = 0xFFE0C475;
    private static final int WOOD_DARK = 0xFF1F160F;
    private static final int WOOD = 0xFF382719;
    private static final int WOOD_LIGHT = 0xFF563D25;
    private static final int GREEN_DARK = 0xFF173229;
    private static final int GREEN = 0xFF295546;
    private static final int TEAL = 0xFF3C806F;
    private static final int TEAL_LIGHT = 0xFF69C0A7;
    private static final int PARCHMENT = 0xFFE9DDBB;
    private static final int MUTED = 0xFFB6AA89;

    private final Screen parent;
    private final PartyFinderController controller;
    private Tab tab = Tab.FIND;
    private int page;
    private String minimumLevel = "0";
    private String maximumLevel = "9999";
    private String inviteName = "";

    public PartyFinderScreen(Screen parent, PartyFinderController controller) {
        super(Component.literal("Party Finder"));
        this.parent = parent;
        this.controller = controller;
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int contentLeft = left + SIDEBAR_WIDTH + 13;
        int contentRight = left + PANEL_WIDTH - 12;

        addRenderableWidget(new PartyFinderButton(left + 8, top + 53, SIDEBAR_WIDTH - 8, 25, Component.literal("Find Party"), () -> select(Tab.FIND)));
        addRenderableWidget(new PartyFinderButton(left + 8, top + 78, SIDEBAR_WIDTH - 8, 25, Component.literal("My Party"), () -> select(Tab.MANAGE)));

        if (tab == Tab.FIND) {
            addRenderableWidget(new PartyFinderButton(contentRight - 68, top + 51, 62, 20, Component.literal("Refresh"), controller::refreshParties));
            List<PartyListing> listings = visibleListings();
            for (int index = 0; index < listings.size(); index++) {
                PartyListing listing = listings.get(index);
                int y = top + 82 + index * ROW_HEIGHT;
                PartyFinderButton join = new PartyFinderButton(contentRight - 62, y + 11, 52, 20, Component.literal(listing.isFull() ? "Full" : "Join"), () -> controller.requestJoin(listing));
                join.active = !listing.isFull();
                addRenderableWidget(join);
            }
            addRenderableWidget(new PartyFinderButton(contentLeft, top + PANEL_HEIGHT - 39, 54, 19, Component.literal("< Prev"), () -> changePage(-1)));
            addRenderableWidget(new PartyFinderButton(contentRight - 54, top + PANEL_HEIGHT - 39, 54, 19, Component.literal("Next >"), () -> changePage(1)));
        } else {
            EditBox minimum = createNumberBox(contentLeft + 43, top + 83, 43, "Minimum level", minimumLevel, value -> minimumLevel = value);
            EditBox maximum = createNumberBox(contentLeft + 132, top + 83, 43, "Maximum level", maximumLevel, value -> maximumLevel = value);
            addRenderableWidget(minimum);
            addRenderableWidget(maximum);
            addRenderableWidget(new PartyFinderButton(contentRight - 82, top + 80, 72, 21, Component.literal(controller.isAdvertised() ? "Update" : "Publish"), this::publish));
            addRenderableWidget(new PartyFinderButton(contentRight - 82, top + 105, 72, 19, Component.literal("Remove"), controller::removePublishedParty));

            EditBox invite = new EditBox(font, contentLeft + 12, top + 139, 128, 20, Component.literal("Player name"));
            invite.setMaxLength(16);
            invite.setValue(inviteName);
            invite.setHint(Component.literal("Player name"));
            invite.setResponder(value -> inviteName = value);
            invite.setTextColor(PARCHMENT);
            addRenderableWidget(invite);
            addRenderableWidget(new PartyFinderButton(contentLeft + 146, top + 139, 55, 20, Component.literal("Invite"), () -> controller.invite(inviteName)));
            addRenderableWidget(new PartyFinderButton(contentRight - 82, top + 139, 72, 20, Component.literal("/p list"), controller::refreshRoster));

            PartyRoster roster = controller.roster();
            String self = minecraft.getUser().getName();
            int row = 0;
            for (String member : roster.members()) {
                if (row >= 3) {
                    break;
                }
                int y = top + 174 + row * 27;
                if (!member.equalsIgnoreCase(self)) {
                    addRenderableWidget(new PartyFinderButton(contentRight - 113, y + 4, 46, 19, Component.literal("Kick"), () -> controller.kick(member)));
                    addRenderableWidget(new PartyFinderButton(contentRight - 62, y + 4, 52, 19, Component.literal("Leader"), () -> controller.transfer(member)));
                }
                row++;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;
        int contentLeft = left + SIDEBAR_WIDTH + 13;
        int contentRight = right - 12;

        graphics.fill(left + 9, top + 11, right + 9, bottom + 11, SHADOW);
        graphics.fill(left, top, right, bottom, FRAME);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, BRASS_DARK);
        graphics.fill(left + 4, top + 4, right - 4, bottom - 4, WOOD_DARK);
        graphics.fill(left + 7, top + 7, right - 7, bottom - 7, WOOD);

        graphics.fill(left + 7, top + 7, right - 7, top + 45, GREEN_DARK);
        graphics.fill(left + 8, top + 8, right - 8, top + 11, TEAL);
        graphics.fill(left + 7, top + 43, right - 7, top + 45, BRASS);
        graphics.text(font, title, left + 22, top + 22, PARCHMENT, false);
        graphics.text(font, "Ravengard party board", right - 147, top + 22, TEAL_LIGHT, false);

        graphics.fill(left + 7, top + 45, left + SIDEBAR_WIDTH + 7, bottom - 7, WOOD_DARK);
        graphics.fill(left + SIDEBAR_WIDTH + 7, top + 45, left + SIDEBAR_WIDTH + 9, bottom - 7, BRASS_DARK);
        graphics.fill(left + 8, top + (tab == Tab.FIND ? 53 : 78), left + 11, top + (tab == Tab.FIND ? 78 : 103), TEAL_LIGHT);

        graphics.text(font, tab == Tab.FIND ? "Available Parties" : "Party Management", contentLeft, top + 57, PARCHMENT, false);
        graphics.fill(contentLeft, top + 72, contentRight, top + 73, TEAL);

        if (tab == Tab.FIND) {
            renderPartyList(graphics, contentLeft, contentRight, top);
        } else {
            renderManagement(graphics, contentLeft, contentRight, top);
        }

        String status = font.plainSubstrByWidth(controller.status(), SIDEBAR_WIDTH - 22);
        graphics.text(font, "Status", left + 18, bottom - 43, BRASS_LIGHT, false);
        graphics.text(font, status, left + 18, bottom - 28, MUTED, false);
        rivet(graphics, left + 7, top + 7);
        rivet(graphics, right - 8, top + 7);
        rivet(graphics, left + 7, bottom - 8);
        rivet(graphics, right - 8, bottom - 8);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    public void partyDataChanged() {
        rebuildWidgets();
    }

    private void renderPartyList(GuiGraphicsExtractor graphics, int left, int right, int top) {
        List<PartyListing> listings = visibleListings();
        if (listings.isEmpty()) {
            graphics.text(font, "No advertised parties", left + 12, top + 102, MUTED, false);
        }
        for (int index = 0; index < listings.size(); index++) {
            PartyListing listing = listings.get(index);
            int y = top + 82 + index * ROW_HEIGHT;
            graphics.fill(left, y, right, y + 36, FRAME);
            graphics.fill(left + 1, y + 1, right - 1, y + 35, index % 2 == 0 ? GREEN_DARK : WOOD_DARK);
            graphics.fill(left + 2, y + 2, left + 5, y + 34, listing.isFull() ? BRASS_DARK : TEAL);
            graphics.fill(left + 5, y + 2, right - 2, y + 3, WOOD_LIGHT);
            graphics.text(font, listing.leaderName(), left + 12, y + 8, PARCHMENT, false);
            graphics.text(font, "Lv " + listing.leaderLevel(), left + 12, y + 21, TEAL_LIGHT, false);
            graphics.text(font, "Range " + listing.minimumLevel() + "-" + listing.maximumLevel(), left + 91, y + 8, MUTED, false);
            graphics.text(font, listing.size() + "/3 players", left + 91, y + 21, BRASS_LIGHT, false);
        }
        int pages = Math.max(1, (controller.parties().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        String pageText = "Page " + (page + 1) + "/" + pages;
        graphics.text(font, pageText, (left + right - font.width(pageText)) / 2, top + PANEL_HEIGHT - 34, MUTED, false);
    }

    private void renderManagement(GuiGraphicsExtractor graphics, int left, int right, int top) {
        graphics.fill(left, top + 78, right, top + 128, FRAME);
        graphics.fill(left + 1, top + 79, right - 1, top + 127, GREEN_DARK);
        graphics.fill(left + 2, top + 80, left + 5, top + 126, TEAL);
        graphics.text(font, "Levels", left + 10, top + 89, PARCHMENT, false);
        graphics.text(font, "to", left + 93, top + 89, MUTED, false);
        graphics.text(font, controller.isAdvertised() ? "Listed globally" : "Not listed", left + 10, top + 110, controller.isAdvertised() ? TEAL_LIGHT : MUTED, false);

        graphics.fill(left, top + 134, right, top + 164, FRAME);
        graphics.fill(left + 1, top + 135, right - 1, top + 163, WOOD_DARK);
        graphics.fill(left + 2, top + 136, left + 5, top + 162, BRASS);

        PartyRoster roster = controller.roster();
        for (int index = 0; index < Math.min(3, roster.members().size()); index++) {
            int y = top + 174 + index * 27;
            String member = roster.members().get(index);
            graphics.fill(left, y, right, y + 26, FRAME);
            graphics.fill(left + 1, y + 1, right - 1, y + 25, index == 0 ? GREEN_DARK : WOOD_DARK);
            graphics.fill(left + 2, y + 2, left + 5, y + 24, index == 0 ? TEAL : BRASS_DARK);
            graphics.text(font, member, left + 12, y + 9, PARCHMENT, false);
            if (member.equalsIgnoreCase(roster.leader())) {
                graphics.text(font, "Leader", left + 113, y + 9, BRASS_LIGHT, false);
            }
        }
        if (roster.members().isEmpty()) {
            graphics.text(font, "Use /party list to load your current party", left + 9, top + 186, MUTED, false);
        }
    }

    private EditBox createNumberBox(int x, int y, int width, String narration, String initial, java.util.function.Consumer<String> responder) {
        EditBox box = new EditBox(font, x, y, width, 18, Component.literal(narration));
        box.setMaxLength(4);
        box.setValue(initial);
        box.setFilter(value -> value.isEmpty() || value.matches("[0-9]{1,4}"));
        box.setResponder(responder);
        box.setTextColor(PARCHMENT);
        return box;
    }

    private void publish() {
        try {
            int minimum = minimumLevel.isEmpty() ? 0 : Integer.parseInt(minimumLevel);
            int maximum = maximumLevel.isEmpty() ? 9999 : Integer.parseInt(maximumLevel);
            controller.publishParty(minimum, maximum);
        } catch (NumberFormatException ignored) {
        }
    }

    private List<PartyListing> visibleListings() {
        List<PartyListing> all = controller.parties();
        int maximumPage = Math.max(0, (all.size() - 1) / PAGE_SIZE);
        page = Math.min(page, maximumPage);
        int from = Math.min(all.size(), page * PAGE_SIZE);
        int to = Math.min(all.size(), from + PAGE_SIZE);
        return all.subList(from, to);
    }

    private void select(Tab selected) {
        if (tab != selected) {
            tab = selected;
            rebuildWidgets();
        }
    }

    private void changePage(int direction) {
        int maximumPage = Math.max(0, (controller.parties().size() - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(maximumPage, page + direction));
        rebuildWidgets();
    }

    private static void rivet(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 3, y + 3, FRAME);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, BRASS_LIGHT);
    }

    private enum Tab {
        FIND,
        MANAGE
    }
}
