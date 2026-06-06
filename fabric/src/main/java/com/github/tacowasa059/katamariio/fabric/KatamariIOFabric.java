package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.commands.KatamariCommands;
import com.github.tacowasa059.katamariio.common.event.PlayerRespawnEventListener;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class KatamariIOFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModNetwork.bootstrap(new FabricModNetwork());
        KatamariIO.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                KatamariCommands.register(dispatcher));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                PlayerRespawnEventListener.onPlayerClone(oldPlayer, newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PlayerRespawnEventListener.onPlayerLogin(handler.getPlayer()));
    }
}
