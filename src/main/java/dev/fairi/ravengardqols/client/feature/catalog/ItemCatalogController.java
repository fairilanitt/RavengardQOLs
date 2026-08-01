package dev.fairi.ravengardqols.client.feature.catalog;

import dev.fairi.ravengardqols.client.feature.party.PartyFinderConfig;
import dev.fairi.ravengardqols.client.feature.party.PartyRelayClient;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemCatalogController {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int MAXIMUM_PENDING_ITEMS = 128;
    private static final long RETRY_DELAY_MILLIS = 10_000L;
    private static final long REFRESH_INTERVAL_MILLIS = 15_000L;
    private static final ItemCatalogController INSTANCE = new ItemCatalogController();

    private final Map<String, CatalogItemSubmission> pending = new LinkedHashMap<>();
    private final Set<String> acceptedNames = new HashSet<>();
    private volatile List<CatalogDisplayItem> displayItems = List.of();
    private volatile CatalogPage catalogPage = CatalogPage.empty(36);
    private volatile boolean connected;
    private PartyRelayClient relay;
    private boolean discoveryInFlight;
    private boolean pageInFlight;
    private int scanTicks;
    private int requestedPage;
    private int pageSize = 36;
    private String query = "";
    private long queryRefreshAt;
    private long nextRefreshAt;
    private long retryAt;
    private ClientLevel observedLevel;
    private boolean ravengardSession;

    private ItemCatalogController() {
    }

    public static ItemCatalogController get() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        boolean canDiscoverItems = isRavengardSession(minecraft);
        if (++scanTicks >= SCAN_INTERVAL_TICKS) {
            scanTicks = 0;
            if (canDiscoverItems) {
                scanInventory(minecraft);
            }
        }

        long now = System.currentTimeMillis();
        if (canDiscoverItems && !discoveryInFlight && !pending.isEmpty() && now >= retryAt) {
            submitNext();
        }
        if (minecraft.gui.screen() instanceof InventoryScreen
            && !pageInFlight
            && now >= retryAt
            && (now >= queryRefreshAt || now >= nextRefreshAt)) {
            refreshPage();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public List<CatalogDisplayItem> displayItems() {
        return displayItems;
    }

    public int page() {
        return catalogPage.page();
    }

    public int pages() {
        return Math.max(1, catalogPage.pages());
    }

    public int total() {
        return Math.max(0, catalogPage.total());
    }

    public String query() {
        return query;
    }

    public void setQuery(String value) {
        String updated = value == null ? "" : value;
        if (updated.length() > 64) {
            updated = updated.substring(0, 64);
        }
        if (!query.equals(updated)) {
            query = updated;
            requestedPage = 0;
            queryRefreshAt = System.currentTimeMillis() + 250L;
        }
    }

    public void setPageSize(int value) {
        int updated = Math.max(1, Math.min(48, value));
        if (pageSize != updated) {
            pageSize = updated;
            requestedPage = 0;
            queryRefreshAt = 0L;
        }
    }

    public void previousPage() {
        if (requestedPage > 0) {
            requestedPage--;
            queryRefreshAt = 0L;
        }
    }

    public void nextPage() {
        if (requestedPage + 1 < pages()) {
            requestedPage++;
            queryRefreshAt = 0L;
        }
    }

    private void scanInventory(Minecraft minecraft) {
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && pending.size() < MAXIMUM_PENDING_ITEMS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.is(Items.NETHER_STAR)) {
                continue;
            }
            String key = catalogKey(stack.getHoverName().getString());
            if (key.isEmpty() || acceptedNames.contains(key) || pending.containsKey(key)) {
                continue;
            }
            try {
                ItemCatalogCodec.capture(minecraft, stack).ifPresentOrElse(
                    item -> pending.put(key, item),
                    () -> acceptedNames.add(key)
                );
            } catch (RuntimeException ignored) {
                acceptedNames.add(key);
            }
        }
    }

    private boolean isRavengardSession(Minecraft minecraft) {
        if (observedLevel != minecraft.level) {
            observedLevel = minecraft.level;
            ravengardSession = false;
        }
        if (!isHypixel(minecraft)) {
            return false;
        }
        if (ravengardSession) {
            return true;
        }

        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            Identifier tooltipStyle = inventory.getItem(slot).get(DataComponents.TOOLTIP_STYLE);
            if (tooltipStyle != null && "hypixel_ravengard".equals(tooltipStyle.getNamespace())) {
                ravengardSession = true;
                return true;
            }
        }
        return false;
    }

    private static boolean isHypixel(Minecraft minecraft) {
        if (minecraft.getCurrentServer() == null || minecraft.getCurrentServer().ip == null) {
            return false;
        }
        String address = minecraft.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);
        int portSeparator = address.lastIndexOf(':');
        if (portSeparator > 0 && address.indexOf(':') == portSeparator) {
            address = address.substring(0, portSeparator);
        }
        return address.equals("hypixel.net") || address.endsWith(".hypixel.net");
    }

    private void submitNext() {
        PartyRelayClient client = backendClient();
        if (client == null || pending.isEmpty()) {
            return;
        }
        Map.Entry<String, CatalogItemSubmission> queued = pending.entrySet().iterator().next();
        discoveryInFlight = true;
        client.discoverItem(queued.getValue()).whenComplete((created, failure) -> onMinecraftThread(() -> {
            discoveryInFlight = false;
            if (failure != null) {
                markDisconnected();
                return;
            }
            pending.remove(queued.getKey());
            acceptedNames.add(queued.getKey());
            markConnected();
            if (Boolean.TRUE.equals(created)) {
                nextRefreshAt = 0L;
            }
        }));
    }

    private void refreshPage() {
        PartyRelayClient client = backendClient();
        if (client == null) {
            return;
        }
        String requestedQuery = query;
        int page = requestedPage;
        int size = pageSize;
        pageInFlight = true;
        queryRefreshAt = Long.MAX_VALUE;
        nextRefreshAt = System.currentTimeMillis() + REFRESH_INTERVAL_MILLIS;
        client.listItems(requestedQuery, page, size).whenComplete((result, failure) -> onMinecraftThread(() -> {
            pageInFlight = false;
            if (failure != null || result == null || result.items() == null) {
                markDisconnected();
                return;
            }
            markConnected();
            if (!query.equals(requestedQuery) || requestedPage != page || pageSize != size) {
                queryRefreshAt = 0L;
                return;
            }

            List<CatalogDisplayItem> decoded = result.items().stream()
                .filter(ItemCatalogController::isSafeEntry)
                .map(ItemCatalogController::decodeSafely)
                .filter(java.util.Objects::nonNull)
                .filter(item -> !item.stack().isEmpty() && !item.stack().is(Items.NETHER_STAR))
                .toList();
            displayItems = List.copyOf(decoded);
            catalogPage = new CatalogPage(
                result.items(),
                Math.max(0, result.page()),
                Math.max(1, result.pageSize()),
                Math.max(0, result.total()),
                Math.max(1, result.pages())
            );
            requestedPage = catalogPage.page();
            for (CatalogDisplayItem item : displayItems) {
                acceptedNames.add(catalogKey(item.entry().name()));
            }
        }));
    }

    private PartyRelayClient backendClient() {
        if (relay != null) {
            return relay;
        }
        try {
            relay = new PartyRelayClient(PartyFinderConfig.relayUri());
            return relay;
        } catch (RuntimeException exception) {
            markDisconnected();
            return null;
        }
    }

    private void markConnected() {
        connected = true;
        retryAt = 0L;
    }

    private void markDisconnected() {
        connected = false;
        retryAt = System.currentTimeMillis() + RETRY_DELAY_MILLIS;
        nextRefreshAt = retryAt;
    }

    private static boolean isSafeEntry(CatalogItemEntry entry) {
        return entry != null
            && entry.name() != null
            && !entry.name().isBlank()
            && entry.name().length() <= 128
            && entry.itemId() != null
            && entry.itemId().length() <= 160
            && Identifier.tryParse(entry.itemId()) != null
            && entry.stack() != null
            && entry.stack().toString().getBytes(StandardCharsets.UTF_8).length <= 60_000
            && entry.components() != null
            && entry.components().size() <= 64
            && entry.components().stream().allMatch(component ->
                component != null
                    && component.id() != null
                    && component.id().length() <= 160
                    && Identifier.tryParse(component.id()) != null
                    && component.value() != null
                    && component.value().length() <= 16_384
            );
    }

    private static CatalogDisplayItem decodeSafely(CatalogItemEntry entry) {
        try {
            return new CatalogDisplayItem(entry, ItemCatalogCodec.decode(Minecraft.getInstance(), entry));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String catalogKey(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private static void onMinecraftThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}
