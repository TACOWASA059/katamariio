package com.github.tacowasa059.katamariio.forge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CKatamariDeltaPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@SuppressWarnings("removal")
public class ForgeModNetwork implements ModNetwork.NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KatamariIO.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private int nextId;

    @Override
    public void register() {
        CHANNEL.registerMessage(nextId++, RemoveBlockPacket.class,
                RemoveBlockPacket::encode,
                RemoveBlockPacket::decode,
                (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> packet.handle(context.getSender()));
                    context.setPacketHandled(true);
                });
        CHANNEL.registerMessage(nextId++, S2CPlayerPacket.class,
                S2CPlayerPacket::encode,
                S2CPlayerPacket::decode,
                (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() ->
                            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientStateSync.receive(packet)));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, S2CKatamariDeltaPacket.class,
                S2CKatamariDeltaPacket::encode,
                S2CKatamariDeltaPacket::decode,
                (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() ->
                            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientStateSync.receiveDelta(packet)));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket.class,
                com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket::encode,
                com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket::decode,
                (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() ->
                            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientStateSync.receiveConfig(packet)));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket.class,
                com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket::encode,
                com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket::decode,
                (packet, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> packet.handle(context.getSender()));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    @Override
    public void sendRotationToServer(org.joml.Quaternionf quaternion) {
        CHANNEL.sendToServer(new com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket(quaternion));
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public void sendBlocksDelta(ServerPlayer owner, S2CKatamariDeltaPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner), packet);
    }

    @Override
    public void sendFullSnapshot(ServerPlayer viewer, ServerPlayer owner) {
        if (owner == null) return;
        com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData data =
                (com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData) owner;
        S2CKatamariDeltaPacket packet = new S2CKatamariDeltaPacket(
                owner.getId(), new com.github.tacowasa059.katamariio.common.data.KatamariBlocks(data.katamariIO$getBlocks()), true);
        if (viewer != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet);
        } else {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner), packet);
        }
    }

    @Override
    public void sendConfigSync(ServerPlayer player) {
        if (player == null) return;
        com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket packet =
                new com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket(
                        com.github.tacowasa059.katamariio.common.config.KatamariConfig.toJson());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public void broadcastConfigSync(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket packet =
                new com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket(
                        com.github.tacowasa059.katamariio.common.config.KatamariConfig.toJson());
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
