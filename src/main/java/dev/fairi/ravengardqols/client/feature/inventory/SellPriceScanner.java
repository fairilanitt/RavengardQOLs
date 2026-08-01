package dev.fairi.ravengardqols.client.feature.inventory;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public final class SellPriceScanner {
    private static final Pattern CROWNS_PATTERN = Pattern.compile("([0-9][0-9,]*)\\s+crowns", Pattern.CASE_INSENSITIVE);

    private SellPriceScanner() {
    }

    public static OptionalLong detect(ItemStack stack) {
        if (stack.isEmpty()) {
            return OptionalLong.empty();
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return OptionalLong.empty();
        }

        for (Component line : lore.lines()) {
            OptionalLong price = detectText(line.getString());
            if (price.isPresent()) {
                return price;
            }
        }
        return OptionalLong.empty();
    }

    public static OptionalLong detectText(String text) {
        Matcher matcher = CROWNS_PATTERN.matcher(text);
        if (!matcher.find()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(matcher.group(1).replace(",", "")));
        } catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }
}
