package dev.fairi.ravengardqols.client.feature.rarity;

import java.util.Optional;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public final class RarityScanner {
    private RarityScanner() {
    }

    public static Optional<ItemRarity> detect(ItemStack stack) {
        return detectDetailed(stack).map(RarityDetection::rarity);
    }

    public static Optional<RarityDetection> detectDetailed(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (int index = 0; index < lore.lines().size(); index++) {
                Component line = lore.lines().get(index);
                Optional<ItemRarity> rarity = ItemRarity.fromText(line.getString());
                if (rarity.isPresent()) {
                    return Optional.of(new RarityDetection(rarity.get(), "minecraft:lore[" + index + "]"));
                }
            }
        }

        Optional<ItemRarity> nameRarity = ItemRarity.fromText(stack.getHoverName().getString());
        if (nameRarity.isPresent()) {
            return Optional.of(new RarityDetection(nameRarity.get(), "minecraft:custom_name"));
        }

        for (TypedDataComponent<?> component : stack.getComponents()) {
            Optional<ItemRarity> rarity = ItemRarity.fromExplicitMetadata(component.value().toString());
            if (rarity.isPresent()) {
                String componentId = String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type()));
                return Optional.of(new RarityDetection(rarity.get(), componentId));
            }
        }

        return Optional.empty();
    }
}
