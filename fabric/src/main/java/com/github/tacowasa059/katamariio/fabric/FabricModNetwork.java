package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.github.tacowasa059.katamariio.common.networks.KatamariPayloads;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CKatamariDeltaPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Quaternionf;

public class FabricModNetwork implements ModNetwork.NetworkHandler {
    @Override
    public void register() {
        PayloadTypeRegistry.playC2S().register(KatamariPayloads.RemoveBlock.TYPE, KatamariPayloads.RemoveBlock.CODEC);
        PayloadTypeRegistry.playC2S().register(KatamariPayloads.Rotation.TYPE, KatamariPayloads.Rotation.CODEC);
        PayloadTypeRegistry.playS2C().register(KatamariPayloads.PlayerSync.TYPE, KatamariPayloads.PlayerSync.CODEC);
        PayloadTypeRegistry.playS2C().register(KatamariPayloads.Delta.TYPE, KatamariPayloads.Delta.CODEC);
        PayloadTypeRegistry.playS2C().register(KatamariPayloads.ConfigSync.TYPE, KatamariPayloads.ConfigSync.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(KatamariPayloads.RemoveBlock.TYPE,
                (payload, context) -> payload.packet().handle(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(KatamariPayloads.Rotation.TYPE,
                (payload, context) -> payload.packet().handle(context.player()));
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        FabricClientNetworkSender.sendToServer(packet);
    }

    @Override
    public void sendRotationToServer(Quaternionf quaternion) {
        FabricClientNetworkSender.sendRotationToServer(quaternion);
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        ServerPlayNetworking.send(player, new KatamariPayloads.PlayerSync(packet));
    }

    @Override
    public void sendBlocksDelta(ServerPlayer owner, S2CKatamariDeltaPacket packet) {
        sendDelta(owner, packet);
        for (ServerPlayer viewer : PlayerLookup.tracking(owner)) {
            sendDelta(viewer, packet);
        }
    }

    @Override
    public void sendFullSnapshot(ServerPlayer viewer, ServerPlayer owner) {
        if (owner == null) return;
        ICustomPlayerData data = (ICustomPlayerData) owner;
        S2CKatamariDeltaPacket packet = new S2CKatamariDeltaPacket(
                owner.getId(), new KatamariBlocks(data.katamariIO$getBlocks()), true);
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
        ServerPlayNetworking.send(target, new KatamariPayloads.Delta(packet));
    }

    @Override
    public void sendConfigSync(ServerPlayer player) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new KatamariPayloads.ConfigSync(new S2CConfigSyncPacket(KatamariConfig.toJson())));
    }

    @Override
    public void broadcastConfigSync(MinecraftServer server) {
        if (server == null) return;
        KatamariPayloads.ConfigSync payload = new KatamariPayloads.ConfigSync(new S2CConfigSyncPacket(KatamariConfig.toJson()));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
