package com.github.tacowasa059.katamariio.forge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

@SuppressWarnings("removal")
public class ForgeModNetwork implements ModNetwork.NetworkHandler {
    private static final int PROTOCOL_VERSION = 1;
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "main"))
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
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }
}
