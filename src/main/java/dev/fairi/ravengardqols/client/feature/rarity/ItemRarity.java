package dev.fairi.ravengardqols.client.feature.rarity;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public enum ItemRarity {
    COMMON("COMMON", 0xFFD0D0D0, 0x34D0D0D0),
    UNCOMMON("UNCOMMON", 0xFF168A35, 0x3A168A35),
    RARE("RARE", 0xFF3B82F6, 0x403B82F6);

    private final int borderColor;
    private final int fillColor;
    private final Pattern labelPattern;

    ItemRarity(String label, int borderColor, int fillColor) {
        this.borderColor = borderColor;
        this.fillColor = fillColor;
        this.labelPattern = Pattern.compile("^\\s*" + label + "(?:$|\\W)");
    }

    public int borderColor() {
        return borderColor;
    }

    public int fillColor() {
        return fillColor;
    }

    public static Optional<ItemRarity> fromText(String text) {
        String normalized = text.toUpperCase(Locale.ROOT);
        for (ItemRarity rarity : values()) {
            if (rarity.labelPattern.matcher(normalized).find()) {
                return Optional.of(rarity);
            }
        }
        return Optional.empty();
    }
}
