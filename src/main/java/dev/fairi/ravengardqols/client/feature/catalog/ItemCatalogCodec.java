package dev.fairi.ravengardqols.client.feature.catalog;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.fairi.ravengardqols.client.feature.inventory.SellPriceScanner;
import dev.fairi.ravengardqols.client.feature.rarity.RarityScanner;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemCatalogCodec {
    private static final int MAXIMUM_SUBMISSION_BYTES = 60_000;
    private static final int MAXIMUM_COMPONENTS = 64;
    private static final int MAXIMUM_COMPONENT_VALUE = 16_384;
    private static final Gson GSON = new Gson();

    private ItemCatalogCodec() {
    }

    public static Optional<CatalogItemSubmission> capture(Minecraft minecraft, ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.NETHER_STAR)) {
            return Optional.empty();
        }

        String name = stack.getHoverName().getString().trim();
        if (name.isEmpty() || name.length() > 128 || containsControlCharacter(name)) {
            return Optional.empty();
        }

        DynamicOps<JsonElement> ops = registryOps(minecraft);
        Optional<JsonElement> encoded = ItemStack.CODEC.encodeStart(ops, stack.copyWithCount(1)).result();
        if (encoded.isEmpty()) {
            return Optional.empty();
        }

        List<CatalogComponent> components = new ArrayList<>();
        for (TypedDataComponent<?> component : stack.getComponents()) {
            if (components.size() >= MAXIMUM_COMPONENTS) {
                return Optional.empty();
            }
            String id = String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type()));
            String value = component.encodeValue(ops)
                .result()
                .map(JsonElement::toString)
                .orElseGet(() -> component.value().toString());
            if (value.length() > MAXIMUM_COMPONENT_VALUE) {
                return Optional.empty();
            }
            components.add(new CatalogComponent(id, value));
        }

        String rarity = RarityScanner.detect(stack).map(Enum::name).orElse("NONE");
        OptionalLong detectedSellPrice = SellPriceScanner.detect(stack);
        Long sellPrice = detectedSellPrice.isPresent() ? detectedSellPrice.getAsLong() : null;
        CatalogItemSubmission submission = new CatalogItemSubmission(
            name,
            String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())),
            encoded.get(),
            List.copyOf(components),
            rarity,
            sellPrice
        );
        if (GSON.toJson(submission).getBytes(StandardCharsets.UTF_8).length > MAXIMUM_SUBMISSION_BYTES) {
            return Optional.empty();
        }
        return Optional.of(submission);
    }

    public static ItemStack decode(Minecraft minecraft, CatalogItemEntry entry) {
        Optional<ItemStack> decoded = ItemStack.CODEC.parse(registryOps(minecraft), entry.stack()).result();
        if (decoded.isPresent() && !decoded.get().isEmpty()) {
            ItemStack stack = decoded.get();
            stack.setCount(1);
            return stack;
        }

        Identifier id = Identifier.tryParse(entry.itemId());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        ItemStack fallback = new ItemStack(BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR));
        fallback.setCount(1);
        return fallback;
    }

    private static DynamicOps<JsonElement> registryOps(Minecraft minecraft) {
        return minecraft.level == null
            ? JsonOps.INSTANCE
            : RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());
    }

    private static boolean containsControlCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 0x20 || character == 0x7F) {
                return true;
            }
        }
        return false;
    }
}
