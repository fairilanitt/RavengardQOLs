package dev.fairi.ravengardqols.client.feature.catalog;

import java.util.List;

public record CatalogPage(
    List<CatalogItemEntry> items,
    int page,
    int pageSize,
    int total,
    int pages
) {
    public static CatalogPage empty(int pageSize) {
        return new CatalogPage(List.of(), 0, pageSize, 0, 1);
    }
}
