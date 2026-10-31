package com.github.tacowasa059.katamariio.forge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.github.tacowasa059.katamariio.common.networks.C2SPlayerRotationPacket;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CKatamariDeltaPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import org.joml.Quaternionf;

@SuppressWarnings("removal")
public class ForgeModNetwork implements ModNetwork.NetworkHandler {
    private static final int PROTOCOL_VERSION = 1;
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(KatamariIO.MODID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    private int nextId;

    @Override
    public void register() {
        CHANNEL.messageBuilder(RemoveBlockPacket.class, nextId++)
                .encoder(RemoveBlockPacket::encode)
                .decoder(RemoveBlockPacket::decode)
                .consumerMainThread((packet, context) -> packet.handle(context.getSender()))
                .add();
        CHANNEL.messageBuilder(S2CPlayerPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPlayerPacket::encode)
                .decoder(S2CPlayerPacket::decode)
                .consumerMainThread((packet, context) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientStateSync.receive(packet)))
                .add();
        CHANNEL.messageBuilder(S2CKatamariDeltaPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CKatamariDeltaPacket::encode)
                .decoder(S2CKatamariDeltaPacket::decode)
                .consumerMainThread((packet, context) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientStateSync.receiveDelta(packet)))
                .add();
        CHANNEL.messageBuilder(S2CConfigSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CConfigSyncPacket::encode)
                .decoder(S2CConfigSyncPacket::decode)
                .consumerMainThread((packet, context) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientStateSync.receiveConfig(packet)))
                .add();
        CHANNEL.messageBuilder(C2SPlayerRotationPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SPlayerRotationPacket::encode)
                .decoder(C2SPlayerRotationPacket::decode)
                .consumerMainThread((packet, context) -> packet.handle(context.getSender()))
                .add();
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendRotationToServer(Quaternionf quaternion) {
        CHANNEL.send(new C2SPlayerRotationPacket(quaternion), PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    @Override
    public void sendBlocksDelta(ServerPlayer owner, S2CKatamariDeltaPacket packet) {
        CHANNEL.send(packet, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(owner));
    }

    @Override
    public void sendFullSnapshot(ServerPlayer viewer, ServerPlayer owner) {
        if (owner == null) return;
        ICustomPlayerData data = (ICustomPlayerData) owner;
        S2CKatamariDeltaPacket packet = new S2CKatamariDeltaPacket(
                owner.getId(), new KatamariBlocks(data.katamariIO$getBlocks()), true);
        if (viewer != null) {
            CHANNEL.send(packet, PacketDistributor.PLAYER.with(viewer));
        } else {
            CHANNEL.send(packet, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(owner));
        }
    }

    @Override
    public void sendConfigSync(ServerPlayer player) {
        if (player == null) return;
        S2CConfigSyncPacket packet = new S2CConfigSyncPacket(KatamariConfig.toJson());
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    @Override
    public void broadcastConfigSync(MinecraftServer server) {
        if (server == null) return;
        S2CConfigSyncPacket packet = new S2CConfigSyncPacket(KatamariConfig.toJson());
        CHANNEL.send(packet, PacketDistributor.ALL.noArg());
    }
}
