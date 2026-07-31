package dev.fairi.ravengardqols;

import com.mojang.blaze3d.platform.InputConstants;
import dev.fairi.ravengardqols.client.feature.rarity.RaritySlotHighlighter;
import dev.fairi.ravengardqols.client.gui.RavengardMainScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

final class RavengardQolsClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
        Identifier.fromNamespaceAndPath(RavengardQols.MOD_ID, "main")
    );
    private static final KeyMapping OPEN_MAIN_SCREEN = new KeyMapping(
        "key.ravengardqols.open_main_screen",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );

    private RavengardQolsClient() {
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_MAIN_SCREEN);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_MAIN_SCREEN.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.setScreen(new RavengardMainScreen(minecraft.gui.screen()));
        }
    }

    static void onScreenRender(ScreenEvent.Render.Foreground event) {
        RaritySlotHighlighter.highlightContainerSlots(event);
    }

    static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        RaritySlotHighlighter.highlightHotbarSlots(event);
    }
}
