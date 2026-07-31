package dev.fairi.ravengardqols.client.feature.rarity;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public final class RarityScanner {
    private RarityScanner() {
    }

    public static Optional<ItemRarity> detect(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                Optional<ItemRarity> rarity = ItemRarity.fromText(line.getString());
                if (rarity.isPresent()) {
                    return rarity;
                }
            }
        }

        return ItemRarity.fromText(stack.getHoverName().getString());
    }
}
