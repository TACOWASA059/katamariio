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
    static final ResourceLocation ROTATION_ID = new ResourceLocation(KatamariIO.MODID, "player_rotation");
    static final ResourceLocation S2C_PLAYER_ID = new ResourceLocation(KatamariIO.MODID, "player_sync");
    static final ResourceLocation S2C_DELTA_ID = new ResourceLocation(KatamariIO.MODID, "katamari_delta");
    public static final ResourceLocation S2C_CONFIG_SYNC_ID = new ResourceLocation(KatamariIO.MODID, "config_sync");

    @Override
    public void register() {
        ServerPlayNetworking.registerGlobalReceiver(REMOVE_BLOCK_ID, (server, player, handler, buf, responseSender) -> {
            RemoveBlockPacket packet = RemoveBlockPacket.decode(buf);
            server.execute(() -> packet.handle(player));
        });
        ServerPlayNetworking.registerGlobalReceiver(ROTATION_ID, (server, player, handler, buf, responseSender) -> {
            com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket packet =
                    com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket.decode(buf);
            server.execute(() -> packet.handle(player));
        });
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        FabricClientNetworkSender.sendToServer(packet);
    }

    @Override
    public void sendRotationToServer(org.joml.Quaternionf quaternion) {
        FabricClientNetworkSender.sendRotationToServer(quaternion);
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

    @Override
    public void sendFullSnapshot(ServerPlayer viewer, ServerPlayer owner) {
        if (owner == null) return;
        com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData data =
                (com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData) owner;
        S2CKatamariDeltaPacket packet = new S2CKatamariDeltaPacket(
                owner.getId(), new com.github.tacowasa059.katamariio.common.data.KatamariBlocks(data.katamariIO$getBlocks()), true);
        if (viewer != null) {
            sendDelta(viewer, packet);
        } else {
            sendDelta(owner, packet);
            for (ServerPlayer v : PlayerLookup.tracking(owner)) {
                sendDelta(v, packet);
            }
        }
    }

    private static void sendDelta(ServerPlayer target, S2CKatamariDeltaPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        S2CKatamariDeltaPacket.encode(packet, buf);
        ServerPlayNetworking.send(target, S2C_DELTA_ID, buf);
    }

    @Override
    public void sendConfigSync(ServerPlayer player) {
        if (player == null) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket packet =
                new com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket(
                        com.github.tacowasa059.katamariio.common.config.KatamariConfig.toJson());
        com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket.encode(packet, buf);
        ServerPlayNetworking.send(player, S2C_CONFIG_SYNC_ID, buf);
    }

    @Override
    public void broadcastConfigSync(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket packet =
                new com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket(
                        com.github.tacowasa059.katamariio.common.config.KatamariConfig.toJson());
        com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket.encode(packet, buf);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, S2C_CONFIG_SYNC_ID, new FriendlyByteBuf(buf.copy()));
        }
    }
}
