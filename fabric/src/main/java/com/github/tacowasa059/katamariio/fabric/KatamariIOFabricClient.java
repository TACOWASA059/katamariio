package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.networks.S2CKatamariDeltaPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class KatamariIOFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> FabricClientStateSync.flushPending());
        ClientPlayNetworking.registerGlobalReceiver(FabricModNetwork.S2C_PLAYER_ID,
                (client, handler, buf, responseSender) -> {
                    S2CPlayerPacket packet = S2CPlayerPacket.decode(buf);
                    client.execute(() -> FabricClientStateSync.receive(packet));
                });
        ClientPlayNetworking.registerGlobalReceiver(FabricModNetwork.S2C_DELTA_ID,
                (client, handler, buf, responseSender) -> {
                    S2CKatamariDeltaPacket packet = S2CKatamariDeltaPacket.decode(buf);
                    client.execute(() -> packet.apply(client.level));
                });
        ClientPlayNetworking.registerGlobalReceiver(FabricModNetwork.S2C_CONFIG_SYNC_ID,
                (client, handler, buf, responseSender) -> {
                    com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket packet =
                            com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket.decode(buf);
                    client.execute(packet::apply);
                });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> com.github.tacowasa059.katamariio.common.config.KatamariConfig.load());
    }
}
