package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

public final class FabricClientNetworkSender {
    private FabricClientNetworkSender() {
    }

    public static void sendToServer(RemoveBlockPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ClientPlayNetworking.send(FabricModNetwork.REMOVE_BLOCK_ID, buf);
    }
}
