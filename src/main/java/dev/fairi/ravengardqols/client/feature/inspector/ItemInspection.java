package dev.fairi.ravengardqols.client.feature.inspector;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.fairi.ravengardqols.client.feature.rarity.RarityDetection;
import dev.fairi.ravengardqols.client.feature.rarity.RarityScanner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public record ItemInspection(
    String itemName,
    String itemId,
    int count,
    String detectedRarity,
    String detectionSource,
    List<ComponentEntry> components,
    String clipboardText
) {
    public static ItemInspection inspect(Minecraft minecraft, ItemStack stack) {
        DynamicOps<JsonElement> ops = minecraft.level == null
            ? JsonOps.INSTANCE
            : RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());

        List<ComponentEntry> components = new ArrayList<>();
        for (TypedDataComponent<?> component : stack.getComponents()) {
            String id = String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type()));
            String value = component.encodeValue(ops)
                .result()
                .map(JsonElement::toString)
                .orElseGet(() -> component.value().toString());
            components.add(new ComponentEntry(id, escapeNonAscii(value)));
        }
        components.sort(Comparator.comparing(ComponentEntry::id));

        RarityDetection detection = RarityScanner.detectDetailed(stack).orElse(null);
        String rarity = detection == null ? "NONE" : detection.rarity().name();
        String source = detection == null ? "NONE" : detection.source();
        String itemName = escapeNonAscii(stack.getHoverName().getString());
        String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        String clipboard = buildClipboardText(itemName, itemId, stack.getCount(), rarity, source, components);

        return new ItemInspection(itemName, itemId, stack.getCount(), rarity, source, List.copyOf(components), clipboard);
    }

    private static String buildClipboardText(
        String itemName,
        String itemId,
        int count,
        String rarity,
        String source,
        List<ComponentEntry> components
    ) {
        StringBuilder output = new StringBuilder();
        output.append("Item: ").append(itemName).append('\n');
        output.append("ID: ").append(itemId).append('\n');
        output.append("Count: ").append(count).append('\n');
        output.append("Detected rarity: ").append(rarity).append('\n');
        output.append("Detection source: ").append(source).append('\n');
        output.append("Components: ").append(components.size()).append('\n');
        for (ComponentEntry component : components) {
            output.append('\n').append(component.id()).append(" = ").append(component.value());
        }
        return output.toString();
    }

    private static String escapeNonAscii(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 0x20 || character > 0x7E) {
                escaped.append(String.format("\\u%04X", (int) character));
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    public record ComponentEntry(String id, String value) {
    }
}
