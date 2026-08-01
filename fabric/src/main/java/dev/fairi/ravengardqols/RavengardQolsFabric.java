package dev.fairi.ravengardqols;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.fairi.ravengardqols.client.feature.catalog.ItemCatalogController;
import dev.fairi.ravengardqols.client.feature.catalog.ItemCatalogPanel;
import dev.fairi.ravengardqols.client.feature.inventory.InventoryLedgerPanel;
import dev.fairi.ravengardqols.client.feature.party.PartyFinderController;
import dev.fairi.ravengardqols.client.feature.playerlist.NearbyPlayerList;
import dev.fairi.ravengardqols.client.feature.rarity.RaritySlotHighlighter;
import dev.fairi.ravengardqols.client.gui.ItemInspectorScreen;
import dev.fairi.ravengardqols.client.gui.RavengardMainScreen;
import dev.fairi.ravengardqols.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class RavengardQolsFabric implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
        Identifier.fromNamespaceAndPath(RavengardQolsCommon.MOD_ID, "main")
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

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OPEN_MAIN_SCREEN);
        KeyMappingHelper.registerKeyMapping(INSPECT_ITEM);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        registerScreenHooks();
        registerHud();
        registerCommands();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                PartyFinderController.get().onSystemChat(message);
            }
        });
    }

    private void onClientTick(Minecraft minecraft) {
        PartyFinderController.get().tick();
        ItemCatalogController.get().tick();
        while (OPEN_MAIN_SCREEN.consumeClick()) {
            minecraft.gui.setScreen(new RavengardMainScreen(minecraft.gui.screen()));
        }
        while (INSPECT_ITEM.consumeClick()) {
            if (minecraft.gui.screen() == null && minecraft.player != null) {
                openInspector(minecraft.player.getMainHandItem());
            }
        }
    }

    private static void registerScreenHooks() {
        ScreenEvents.AFTER_INIT.register((minecraft, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, partialTick) -> {
                if (current instanceof AbstractContainerScreen<?> containerScreen) {
                    AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) containerScreen;
                    RaritySlotHighlighter.highlightContainerSlots(
                        containerScreen,
                        graphics,
                        accessor.ravengardqols$getLeftPos(),
                        accessor.ravengardqols$getTopPos()
                    );
                    InventoryLedgerPanel.render(
                        containerScreen,
                        graphics,
                        accessor.ravengardqols$getLeftPos(),
                        accessor.ravengardqols$getTopPos(),
                        accessor.ravengardqols$getImageWidth(),
                        accessor.ravengardqols$getImageHeight()
                    );
                    ItemCatalogPanel.render(
                        containerScreen,
                        graphics,
                        accessor.ravengardqols$getLeftPos(),
                        accessor.ravengardqols$getTopPos(),
                        accessor.ravengardqols$getImageWidth(),
                        accessor.ravengardqols$getImageHeight(),
                        mouseX,
                        mouseY,
                        partialTick
                    );
                }
            });
            ScreenKeyboardEvents.allowKeyPress(screen).register((current, keyEvent) -> {
                if (ItemCatalogPanel.onKeyPressed(current, keyEvent)) {
                    return false;
                }
                if (!INSPECT_ITEM.matches(keyEvent) || !(current instanceof AbstractContainerScreen<?> containerScreen)) {
                    return true;
                }
                Slot hoveredSlot = ((AbstractContainerScreenAccessor) containerScreen).ravengardqols$getHoveredSlot();
                if (hoveredSlot == null || !hoveredSlot.hasItem()) {
                    return true;
                }
                openInspector(hoveredSlot.getItem());
                return false;
            });
            ScreenKeyboardEvents.allowCharType(screen).register((current, characterEvent) ->
                !ItemCatalogPanel.onCharTyped(current, characterEvent)
            );
            ScreenMouseEvents.allowMouseClick(screen).register((current, mouseButtonEvent) ->
                !ItemCatalogPanel.onMouseClicked(current, mouseButtonEvent, false)
            );
            ScreenMouseEvents.allowMouseScroll(screen).register((current, mouseX, mouseY, horizontalAmount, verticalAmount) ->
                !ItemCatalogPanel.onMouseScrolled(current, mouseX, mouseY, verticalAmount)
                    && !InventoryLedgerPanel.onMouseScrolled(current, mouseX, mouseY, verticalAmount)
            );
        });
    }

    private static void registerHud() {
        Identifier layer = Identifier.fromNamespaceAndPath(RavengardQolsCommon.MOD_ID, "nearby_players");
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.HOTBAR,
            layer,
            (graphics, deltaTracker) -> NearbyPlayerList.render(graphics)
        );
    }

    private static void registerCommands() {
        PartyFinderController controller = PartyFinderController.get();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
            ClientCommands.literal("pf")
                .executes(context -> {
                    Minecraft.getInstance().execute(controller::openScreen);
                    return 1;
                })
                .then(ClientCommands.literal("accept").then(ClientCommands.argument("request", StringArgumentType.word()).executes(context -> {
                    controller.acceptRequest(StringArgumentType.getString(context, "request"));
                    return 1;
                })))
                .then(ClientCommands.literal("decline").then(ClientCommands.argument("request", StringArgumentType.word()).executes(context -> {
                    controller.declineRequest(StringArgumentType.getString(context, "request"));
                    return 1;
                })))
        ));
    }

    private static void openInspector(ItemStack stack) {
        if (!stack.isEmpty()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.setScreen(new ItemInspectorScreen(minecraft.gui.screen(), stack));
        }
    }
}
