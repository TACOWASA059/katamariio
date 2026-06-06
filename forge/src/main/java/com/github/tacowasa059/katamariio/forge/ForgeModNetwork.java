package com.github.tacowasa059.katamariio.forge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
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
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
