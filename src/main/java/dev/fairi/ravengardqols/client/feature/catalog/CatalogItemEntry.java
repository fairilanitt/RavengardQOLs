package dev.fairi.ravengardqols.client.feature.catalog;

import com.google.gson.JsonElement;
import java.util.List;

public record CatalogItemEntry(
    String name,
    String itemId,
    JsonElement stack,
    List<CatalogComponent> components,
    String rarity,
    Long sellPrice,
    long firstSeenAt
) {
}
