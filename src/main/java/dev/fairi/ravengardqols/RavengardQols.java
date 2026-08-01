package dev.fairi.ravengardqols;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(RavengardQols.MOD_ID)
public final class RavengardQols {
    public static final String MOD_ID = "ravengardqols";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RavengardQols(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modEventBus.addListener(RavengardQolsClient::registerKeyMappings);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::onClientTick);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::onScreenRender);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::onRenderGuiLayer);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::onScreenKeyPressed);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::onScreenCharacterTyped);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::onScreenMousePressed);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::onScreenMouseScrolled);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::registerClientCommands);
            NeoForge.EVENT_BUS.addListener(RavengardQolsClient::onClientChatReceived);
        }
    }
}
