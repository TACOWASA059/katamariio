package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricClientNetworkSender {
    private FabricClientNetworkSender() {
    }

    public static void sendToServer(RemoveBlockPacket packet) {
        ClientPlayNetworking.send(FabricModNetwork.RemoveBlockPayload.fromPacket(packet));
    }
}
