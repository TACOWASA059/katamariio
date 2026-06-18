package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.commands.KatamariCommands;
import com.github.tacowasa059.katamariio.common.event.PlayerRespawnEventListener;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
        FabricTrackedDataRegistry.register(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "vec3_list"), ModDataSerializers.VEC3_LIST);
        FabricTrackedDataRegistry.register(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "quaternion_list"), ModDataSerializers.QUATERNION_LIST);
        FabricTrackedDataRegistry.register(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "block_list"), ModDataSerializers.BLOCK_LIST);
        KatamariIO.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                KatamariCommands.register(dispatcher));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                PlayerRespawnEventListener.onPlayerClone(oldPlayer, newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PlayerRespawnEventListener.onPlayerLogin(handler.getPlayer()));
    }
}
