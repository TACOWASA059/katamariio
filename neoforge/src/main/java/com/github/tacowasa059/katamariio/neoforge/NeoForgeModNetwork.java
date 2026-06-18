package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgeModNetwork implements ModNetwork.NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public void register() {
    }

    public void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(RemoveBlockPayload.TYPE, RemoveBlockPayload.STREAM_CODEC,
                (payload, context) -> payload.toPacket().handle((ServerPlayer) context.player()));
        registrar.playToClient(S2CPlayerPayload.TYPE, S2CPlayerPayload.STREAM_CODEC,
                (payload, context) -> NeoForgeClientStateSync.receive(payload.toPacket()));
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        ClientPacketDistributor.sendToServer(RemoveBlockPayload.fromPacket(packet));
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        PacketDistributor.sendToPlayer(player, S2CPlayerPayload.fromPacket(packet));
    }
}
