package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket;
import com.github.tacowasa059.katamariio.common.networks.KatamariPayloads;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.joml.Quaternionf;

public final class FabricClientNetworkSender {
    private FabricClientNetworkSender() {
    }

    public static void sendToServer(RemoveBlockPacket packet) {
        ClientPlayNetworking.send(new KatamariPayloads.RemoveBlock(packet));
    }

    public static void sendRotationToServer(Quaternionf quaternion) {
        ClientPlayNetworking.send(new KatamariPayloads.Rotation(new C2SPlayerRotationPacket(quaternion)));
    }
}
