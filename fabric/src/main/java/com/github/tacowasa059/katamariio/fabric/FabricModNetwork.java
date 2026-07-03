package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CKatamariDeltaPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class FabricModNetwork implements ModNetwork.NetworkHandler {
    static final ResourceLocation REMOVE_BLOCK_ID = new ResourceLocation(KatamariIO.MODID, "remove_block");
    static final ResourceLocation S2C_PLAYER_ID = new ResourceLocation(KatamariIO.MODID, "player_sync");
    static final ResourceLocation S2C_DELTA_ID = new ResourceLocation(KatamariIO.MODID, "katamari_delta");

    @Override
    public void register() {
        ServerPlayNetworking.registerGlobalReceiver(REMOVE_BLOCK_ID, (server, player, handler, buf, responseSender) -> {
            RemoveBlockPacket packet = RemoveBlockPacket.decode(buf);
            server.execute(() -> packet.handle(player));
        });
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        FabricClientNetworkSender.sendToServer(packet);
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        S2CPlayerPacket.encode(packet, buf);
        ServerPlayNetworking.send(player, S2C_PLAYER_ID, buf);
    }

    @Override
    public void sendBlocksDelta(ServerPlayer owner, S2CKatamariDeltaPacket packet) {
        // PlayerLookup.tracking(owner) is the other players who can see the owner; the owner itself
        // is not included, so it is sent to separately. A fresh buffer is encoded per recipient.
        sendDelta(owner, packet);
        for (ServerPlayer viewer : PlayerLookup.tracking(owner)) {
            sendDelta(viewer, packet);
        }
    }

    private static void sendDelta(ServerPlayer target, S2CKatamariDeltaPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        S2CKatamariDeltaPacket.encode(packet, buf);
        ServerPlayNetworking.send(target, S2C_DELTA_ID, buf);
    }
}
