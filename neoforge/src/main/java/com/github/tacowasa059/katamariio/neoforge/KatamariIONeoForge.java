package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.commands.KatamariCommands;
import com.github.tacowasa059.katamariio.common.event.PlayerRespawnEventListener;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(KatamariIO.MODID)
public class KatamariIONeoForge {
    private final NeoForgeModNetwork network;

    public KatamariIONeoForge(IEventBus eventBus) {
        this.network = new NeoForgeModNetwork();
        ModNetwork.bootstrap(network);
        NeoForgeModDataSerializers.ENTITY_DATA_SERIALIZERS.register(eventBus);
        KatamariIO.init();
        eventBus.addListener(this::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangeDimension);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        network.register(event);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        KatamariCommands.register(event.getDispatcher());
    }

    private void onPlayerClone(PlayerEvent.Clone event) {
        PlayerRespawnEventListener.onPlayerClone(event.getOriginal(), event.getEntity());
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRespawnEventListener.onPlayerLogin(player);
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRespawnEventListener.onPlayerLogout(player);
        }
    }

    private void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRespawnEventListener.onPlayerChangeDimension(player);
        }
    }
}
