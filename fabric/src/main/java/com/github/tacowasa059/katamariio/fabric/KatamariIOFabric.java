package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.commands.KatamariCommands;
import com.github.tacowasa059.katamariio.common.event.PlayerRespawnEventListener;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry;
import net.minecraft.resources.Identifier;

public class KatamariIOFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModNetwork.bootstrap(new FabricModNetwork());
        // Fabric forbids registering custom tracked data handlers through the vanilla
        // EntityDataSerializers registry (it can desync), so register them via the Fabric API instead.
        FabricTrackedDataRegistry.register(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "katamari_blocks"), ModDataSerializers.KATAMARI_BLOCKS);
        FabricTrackedDataRegistry.register(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "compound_tag"), ModDataSerializers.COMPOUND_TAG);
        KatamariIO.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                KatamariCommands.register(dispatcher));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                PlayerRespawnEventListener.onPlayerClone(oldPlayer, newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PlayerRespawnEventListener.onPlayerLogin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PlayerRespawnEventListener.onPlayerLogout(handler.getPlayer()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                PlayerRespawnEventListener.onPlayerChangeDimension(player));
    }
}
