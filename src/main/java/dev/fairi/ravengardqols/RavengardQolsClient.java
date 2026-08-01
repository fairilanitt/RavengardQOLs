package dev.fairi.ravengardqols;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.fairi.ravengardqols.client.feature.catalog.ItemCatalogController;
import dev.fairi.ravengardqols.client.feature.catalog.ItemCatalogPanel;
import dev.fairi.ravengardqols.client.feature.rarity.RaritySlotHighlighter;
import dev.fairi.ravengardqols.client.feature.inventory.InventoryLedgerPanel;
import dev.fairi.ravengardqols.client.feature.playerlist.NearbyPlayerList;
import dev.fairi.ravengardqols.client.feature.party.PartyFinderController;
import dev.fairi.ravengardqols.client.gui.ItemInspectorScreen;
import dev.fairi.ravengardqols.client.gui.RavengardMainScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
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
    private static final KeyMapping INSPECT_ITEM = new KeyMapping(
        "key.ravengardqols.inspect_item",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F8,
        CATEGORY
    );

    private RavengardQolsClient() {
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_MAIN_SCREEN);
        event.register(INSPECT_ITEM);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        PartyFinderController.get().tick();
        ItemCatalogController.get().tick();
        while (OPEN_MAIN_SCREEN.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.setScreen(new RavengardMainScreen(minecraft.gui.screen()));
        }

        while (INSPECT_ITEM.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() == null && minecraft.player != null) {
                openInspector(minecraft.player.getMainHandItem());
            }
        }
    }

    static void onScreenRender(ScreenEvent.Render.Foreground event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            RaritySlotHighlighter.highlightContainerSlots(
                containerScreen,
                event.getGuiGraphics(),
                containerScreen.getLeftPos(),
                containerScreen.getTopPos()
            );
            InventoryLedgerPanel.render(
                containerScreen,
                event.getGuiGraphics(),
                containerScreen.getLeftPos(),
                containerScreen.getTopPos(),
                containerScreen.getImageWidth(),
                containerScreen.getImageHeight()
            );
            ItemCatalogPanel.render(
                containerScreen,
                event.getGuiGraphics(),
                containerScreen.getLeftPos(),
                containerScreen.getTopPos(),
                containerScreen.getImageWidth(),
                containerScreen.getImageHeight(),
                event.getMouseX(),
                event.getMouseY(),
                event.getPartialTick()
            );
        }
    }

    static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            NearbyPlayerList.render(event.getGuiGraphics());
        }
    }

    static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (ItemCatalogPanel.onKeyPressed(event.getScreen(), event.getKeyEvent())) {
            event.setCanceled(true);
            return;
        }
        if (!INSPECT_ITEM.matches(event.getKeyEvent())
            || !(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        Slot hoveredSlot = containerScreen.getHoveredSlot();
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            openInspector(hoveredSlot.getItem());
            event.setCanceled(true);
        }
    }

    static void onScreenCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (ItemCatalogPanel.onCharTyped(event.getScreen(), event.getCharacterEvent())) {
            event.setCanceled(true);
        }
    }

    static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (ItemCatalogPanel.onMouseClicked(event.getScreen(), event.getMouseButtonEvent(), event.isDoubleClick())) {
            event.setCanceled(true);
        }
    }

    static void onScreenMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (ItemCatalogPanel.onMouseScrolled(
            event.getScreen(),
            event.getMouseX(),
            event.getMouseY(),
            event.getScrollDeltaY()
        ) || InventoryLedgerPanel.onMouseScrolled(
            event.getScreen(),
            event.getMouseX(),
            event.getMouseY(),
            event.getScrollDeltaY()
        )) {
            event.setCanceled(true);
        }
    }

    static void registerClientCommands(RegisterClientCommandsEvent event) {
        PartyFinderController controller = PartyFinderController.get();
        event.getDispatcher().register(
            Commands.literal("pf")
                .executes(context -> {
                    Minecraft.getInstance().execute(controller::openScreen);
                    return 1;
                })
                .then(Commands.literal("accept").then(Commands.argument("request", StringArgumentType.word()).executes(context -> {
                    controller.acceptRequest(StringArgumentType.getString(context, "request"));
                    return 1;
                })))
                .then(Commands.literal("decline").then(Commands.argument("request", StringArgumentType.word()).executes(context -> {
                    controller.declineRequest(StringArgumentType.getString(context, "request"));
                    return 1;
                })))
        );
    }

    static void onClientChatReceived(ClientChatReceivedEvent event) {
        if (event.isSystem()) {
            PartyFinderController.get().onSystemChat(event.getMessage());
        }
    }

    private static void openInspector(ItemStack stack) {
        if (!stack.isEmpty()) {
            Minecraft.getInstance().gui.pushScreenLayer(new ItemInspectorScreen(stack));
        }
    }
}
