package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket;
import com.github.tacowasa059.katamariio.common.networks.KatamariPayloads;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CKatamariDeltaPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Quaternionf;

public class NeoForgeModNetwork implements ModNetwork.NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public void register() {
    }

    public void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(KatamariPayloads.RemoveBlock.TYPE, KatamariPayloads.RemoveBlock.CODEC,
                (payload, context) -> payload.packet().handle((ServerPlayer) context.player()));
        registrar.playToServer(KatamariPayloads.Rotation.TYPE, KatamariPayloads.Rotation.CODEC,
                (payload, context) -> payload.packet().handle((ServerPlayer) context.player()));
        registrar.playToClient(KatamariPayloads.PlayerSync.TYPE, KatamariPayloads.PlayerSync.CODEC,
                (payload, context) -> NeoForgeClientStateSync.receive(payload.packet()));
        registrar.playToClient(KatamariPayloads.Delta.TYPE, KatamariPayloads.Delta.CODEC,
                (payload, context) -> NeoForgeClientStateSync.receiveDelta(payload.packet()));
        registrar.playToClient(KatamariPayloads.ConfigSync.TYPE, KatamariPayloads.ConfigSync.CODEC,
                (payload, context) -> NeoForgeClientStateSync.receiveConfig(payload.packet()));
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        ClientPacketDistributor.sendToServer(new KatamariPayloads.RemoveBlock(packet));
    }

    @Override
    public void sendRotationToServer(Quaternionf quaternion) {
        ClientPacketDistributor.sendToServer(new KatamariPayloads.Rotation(new C2SPlayerRotationPacket(quaternion)));
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        PacketDistributor.sendToPlayer(player, new KatamariPayloads.PlayerSync(packet));
    }

    @Override
    public void sendBlocksDelta(ServerPlayer owner, S2CKatamariDeltaPacket packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(owner, new KatamariPayloads.Delta(packet));
    }

    @Override
    public void sendFullSnapshot(ServerPlayer viewer, ServerPlayer owner) {
        if (owner == null) return;
        ICustomPlayerData data = (ICustomPlayerData) owner;
        KatamariPayloads.Delta payload = new KatamariPayloads.Delta(new S2CKatamariDeltaPacket(
                owner.getId(), new KatamariBlocks(data.katamariIO$getBlocks()), true));
        if (viewer != null) {
            PacketDistributor.sendToPlayer(viewer, payload);
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(owner, payload);
        }
    }

    @Override
    public void sendConfigSync(ServerPlayer player) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new KatamariPayloads.ConfigSync(new S2CConfigSyncPacket(KatamariConfig.toJson())));
    }

    @Override
    public void broadcastConfigSync(MinecraftServer server) {
        if (server == null) return;
        PacketDistributor.sendToAllPlayers(new KatamariPayloads.ConfigSync(new S2CConfigSyncPacket(KatamariConfig.toJson())));
    }
}
