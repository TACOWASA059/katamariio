package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.networks.KatamariPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class KatamariIOFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> FabricClientStateSync.flushPending());
        // Since Fabric API for 1.20.5+, payload handlers already run on the client thread.
        ClientPlayNetworking.registerGlobalReceiver(KatamariPayloads.PlayerSync.TYPE,
                (payload, context) -> FabricClientStateSync.receive(payload.packet()));
        ClientPlayNetworking.registerGlobalReceiver(KatamariPayloads.Delta.TYPE,
                (payload, context) -> payload.packet().apply(context.client().level));
        ClientPlayNetworking.registerGlobalReceiver(KatamariPayloads.ConfigSync.TYPE,
                (payload, context) -> payload.packet().apply());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KatamariConfig.load());
    }
}
