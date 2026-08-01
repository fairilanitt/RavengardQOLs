package dev.fairi.ravengardqols.client.feature.party;

import dev.fairi.ravengardqols.RavengardQolsCommon;
import dev.fairi.ravengardqols.client.feature.playerlist.PlayerLevelParser;
import dev.fairi.ravengardqols.client.gui.PartyFinderScreen;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

public final class PartyFinderController {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern IDENTIFIER = Pattern.compile("[0-9a-fA-F-]{32,36}");
    private static final int PARTY_LIMIT = 3;
    private static final PartyFinderController INSTANCE = new PartyFinderController();

    private final PartyChatParser chatParser = new PartyChatParser();
    private final Set<String> announcedRequests = new HashSet<>();
    private volatile PartyRelayClient relay;
    private volatile List<PartyListing> parties = List.of();
    private volatile List<PartyJoinRequest> requests = List.of();
    private volatile PartyRoster roster = PartyRoster.empty();
    private volatile String status = "Ready";
    private boolean relayStarted;
    private boolean advertised;
    private int advertisedMinimum;
    private int advertisedMaximum = 9999;
    private long nextPollAt;
    private long nextHeartbeatAt;

    private PartyFinderController() {
    }

    public static PartyFinderController get() {
        return INSTANCE;
    }

    public void tick() {
        PartyRoster expired = chatParser.finishIfExpired();
        if (expired != null) {
            setRoster(expired);
        }
        if (!relayStarted || Minecraft.getInstance().player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= nextPollAt) {
            nextPollAt = now + 3_000L;
            refreshParties();
            refreshRequests();
        }
        if (advertised && now >= nextHeartbeatAt) {
            nextHeartbeatAt = now + 25_000L;
            publishParty(advertisedMinimum, advertisedMaximum, false);
        }
    }

    public void onSystemChat(Component message) {
        PartyRoster parsed = chatParser.accept(message.getString());
        if (parsed != null) {
            setRoster(parsed);
        }
    }

    public List<PartyListing> parties() {
        return parties;
    }

    public PartyRoster roster() {
        return roster;
    }

    public String status() {
        return status;
    }

    public boolean isAdvertised() {
        return advertised;
    }

    public void refreshRoster() {
        chatParser.beginCapture();
        sendCommand("party list");
        setStatus("Reading /party list...");
    }

    public void refreshParties() {
        startRelay();
        relay.listParties().whenComplete((result, failure) -> onMinecraftThread(() -> {
            if (failure != null) {
                setFailure("Party list", failure);
                return;
            }
            boolean changed = !parties.equals(result);
            parties = result;
            setStatus("Found " + result.size() + " parties");
            if (changed) {
                notifyScreen();
            }
        }));
    }

    public void invite(String player) {
        if (!validUsername(player)) {
            setStatus("Enter a valid Minecraft name");
            return;
        }
        if (roster.members().size() >= PARTY_LIMIT) {
            setStatus("Party is already full");
            return;
        }
        sendCommand("party invite " + player);
        setStatus("Invited " + player);
    }

    public void kick(String player) {
        if (validUsername(player)) {
            sendCommand("party kick " + player);
            setStatus("Kicked " + player);
            scheduleRosterRefresh();
        }
    }

    public void transfer(String player) {
        if (validUsername(player)) {
            sendCommand("party transfer " + player);
            setStatus("Transferred party to " + player);
            advertised = false;
            startRelay();
            relay.removeParty();
            scheduleRosterRefresh();
        }
    }

    public void publishParty(int minimumLevel, int maximumLevel) {
        publishParty(minimumLevel, maximumLevel, true);
    }

    private void publishParty(int minimumLevel, int maximumLevel, boolean reportStatus) {
        if (minimumLevel < 0 || maximumLevel < minimumLevel || maximumLevel > 9999) {
            setStatus("Level range must be 0-9999");
            return;
        }
        int localLevel = localLevel();
        List<String> members = roster.members().isEmpty()
            ? List.of(Minecraft.getInstance().getUser().getName())
            : roster.members().subList(0, Math.min(PARTY_LIMIT, roster.members().size()));

        startRelay();
        relay.publishParty(localLevel, minimumLevel, maximumLevel, members).whenComplete((ignored, failure) -> onMinecraftThread(() -> {
            if (failure != null) {
                setFailure("Publish", failure);
                return;
            }
            advertised = true;
            advertisedMinimum = minimumLevel;
            advertisedMaximum = maximumLevel;
            nextHeartbeatAt = System.currentTimeMillis() + 25_000L;
            if (reportStatus) {
                setStatus("Party published globally");
            }
            notifyScreen();
            refreshParties();
        }));
    }

    public void removePublishedParty() {
        startRelay();
        relay.removeParty().whenComplete((ignored, failure) -> onMinecraftThread(() -> {
            if (failure != null) {
                setFailure("Remove", failure);
                return;
            }
            advertised = false;
            setStatus("Party removed from finder");
            notifyScreen();
            refreshParties();
        }));
    }

    public void requestJoin(PartyListing listing) {
        if (listing.isFull()) {
            setStatus("That party is full");
            return;
        }
        int level = localLevel();
        if (level < listing.minimumLevel() || level > listing.maximumLevel()) {
            setStatus("Your level is outside that party's range");
            return;
        }
        startRelay();
        relay.requestJoin(listing.leaderUuid(), level).whenComplete((ignored, failure) -> onMinecraftThread(() -> {
            if (failure != null) {
                setFailure("Join request", failure);
            } else {
                setStatus("Join request sent to " + listing.leaderName());
            }
        }));
    }

    public void acceptRequest(String id) {
        PartyJoinRequest request = findRequest(id);
        if (request == null || !validUsername(request.requesterName())) {
            setStatus("Join request is no longer available");
            return;
        }
        if (roster.members().size() >= PARTY_LIMIT) {
            setStatus("Party is full; request not accepted");
            return;
        }
        invite(request.requesterName());
        decideRequest(request, true);
    }

    public void declineRequest(String id) {
        PartyJoinRequest request = findRequest(id);
        if (request != null) {
            decideRequest(request, false);
        }
    }

    private void decideRequest(PartyJoinRequest request, boolean accepted) {
        startRelay();
        relay.decideRequest(request.id(), accepted).whenComplete((ignored, failure) -> onMinecraftThread(() -> {
            if (failure != null) {
                setFailure("Request decision", failure);
                return;
            }
            List<PartyJoinRequest> updated = new ArrayList<>(requests);
            updated.removeIf(candidate -> candidate.id().equals(request.id()));
            requests = List.copyOf(updated);
            setStatus((accepted ? "Accepted " : "Declined ") + request.requesterName());
            notifyScreen();
        }));
    }

    private void refreshRequests() {
        relay.getRequests().whenComplete((result, failure) -> onMinecraftThread(() -> {
            if (failure != null) {
                return;
            }
            boolean changed = !requests.equals(result);
            requests = result;
            for (PartyJoinRequest request : result) {
                if (announcedRequests.add(request.id())) {
                    announceRequest(request);
                }
            }
            announcedRequests.retainAll(result.stream().map(PartyJoinRequest::id).toList());
            if (changed) {
                notifyScreen();
            }
        }));
    }

    private void announceRequest(PartyJoinRequest request) {
        Component message = Component.literal("[Party Finder] ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(request.requesterName() + " (Lv " + request.requesterLevel() + ") wants to join. ")
                .withStyle(ChatFormatting.GOLD))
            .append(Component.literal("[ACCEPT]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent.RunCommand("/pf accept " + request.id()))))
            .append(Component.literal(" "))
            .append(Component.literal("[DECLINE]").withStyle(style -> style
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent.RunCommand("/pf decline " + request.id()))));
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
    }

    public void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        startRelay();
        minecraft.gui.setScreen(new PartyFinderScreen(minecraft.gui.screen(), this));
        refreshParties();
        refreshRoster();
    }

    private void startRelay() {
        if (relay != null) {
            relayStarted = true;
            return;
        }
        try {
            relay = new PartyRelayClient(PartyFinderConfig.relayUri());
            relayStarted = true;
        } catch (RuntimeException exception) {
            setStatus("Invalid relay config: " + safeMessage(exception));
            throw exception;
        }
    }

    private void setRoster(PartyRoster updated) {
        boolean changed = !roster.equals(updated);
        roster = updated;
        setStatus(updated.inParty() ? "Party updated: " + updated.members().size() + "/3" : "Not currently in a party");
        if (advertised) {
            publishParty(advertisedMinimum, advertisedMaximum, false);
        }
        if (changed) {
            notifyScreen();
        }
    }

    private int localLevel() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }
        return PlayerLevelParser.parse(minecraft.player.getDisplayName().getString()).level().orElse(0);
    }

    private PartyJoinRequest findRequest(String id) {
        if (id == null || !IDENTIFIER.matcher(id).matches()) {
            return null;
        }
        return requests.stream().filter(request -> request.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    private static boolean validUsername(String player) {
        return player != null && USERNAME.matcher(player).matches();
    }

    private static void sendCommand(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand(command);
        }
    }

    private void scheduleRosterRefresh() {
        CompletableFuture.delayedExecutor(700L, java.util.concurrent.TimeUnit.MILLISECONDS)
            .execute(() -> onMinecraftThread(this::refreshRoster));
    }

    private void setFailure(String operation, Throwable failure) {
        Throwable cause = unwrap(failure);
        RavengardQolsCommon.LOGGER.warn("Party Finder {} failed: {}", operation, cause.toString());
        setStatus(operation + " failed: " + safeMessage(cause));
    }

    private void setStatus(String value) {
        status = value.length() > 96 ? value.substring(0, 96) : value;
    }

    private void notifyScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof PartyFinderScreen screen) {
            screen.partyDataChanged();
        }
    }

    private static void onMinecraftThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message.replaceAll("[\\r\\n\\t]", " ");
    }
}
