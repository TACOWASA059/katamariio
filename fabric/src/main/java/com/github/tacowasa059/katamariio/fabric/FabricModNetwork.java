package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class FabricModNetwork implements ModNetwork.NetworkHandler {
    static final CustomPacketPayload.Type<RemoveBlockPayload> REMOVE_BLOCK_ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "remove_block"));
    static final CustomPacketPayload.Type<S2CPlayerPayload> S2C_PLAYER_ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "player_sync"));

    @Override
    public void register() {
        PayloadTypeRegistry.playC2S().register(REMOVE_BLOCK_ID, RemoveBlockPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_PLAYER_ID, S2CPlayerPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(REMOVE_BLOCK_ID, (payload, context) ->
                payload.toPacket().handle(context.player()));
    }

    @Override
    public void sendToServer(RemoveBlockPacket packet) {
        FabricClientNetworkSender.sendToServer(packet);
    }

    @Override
    public void sendInitialData(ServerPlayer player, S2CPlayerPacket packet) {
        ServerPlayNetworking.send(player, S2CPlayerPayload.fromPacket(packet));
    }

    record RemoveBlockPayload(net.minecraft.core.BlockPos pos, Vec3 playerPos) implements CustomPacketPayload {
        static final StreamCodec<RegistryFriendlyByteBuf, RemoveBlockPayload> CODEC = StreamCodec.of((buf, payload) -> {
            buf.writeBlockPos(payload.pos);
            buf.writeDouble(payload.playerPos.x);
            buf.writeDouble(payload.playerPos.y);
            buf.writeDouble(payload.playerPos.z);
        }, buf -> new RemoveBlockPayload(
                buf.readBlockPos(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
        ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return REMOVE_BLOCK_ID;
        }

        RemoveBlockPacket toPacket() {
            return new RemoveBlockPacket(this.pos, this.playerPos);
        }

        static RemoveBlockPayload fromPacket(RemoveBlockPacket packet) {
            return new RemoveBlockPayload(packet.pos(), packet.playerPos());
        }
    }

    record S2CPlayerPayload(float collisionSize, float renderSize, boolean flag, float restitution,
                            Quaternionf quaternion, Vec3 currentPosition) implements CustomPacketPayload {
        static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayerPayload> CODEC = StreamCodec.of((buf, payload) -> {
            buf.writeFloat(payload.collisionSize);
            buf.writeFloat(payload.renderSize);
            buf.writeBoolean(payload.flag);
            buf.writeFloat(payload.restitution);
            buf.writeFloat(payload.quaternion.x);
            buf.writeFloat(payload.quaternion.y);
            buf.writeFloat(payload.quaternion.z);
            buf.writeFloat(payload.quaternion.w);
            buf.writeDouble(payload.currentPosition.x);
            buf.writeDouble(payload.currentPosition.y);
            buf.writeDouble(payload.currentPosition.z);
        }, buf -> new S2CPlayerPayload(
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readFloat(),
                new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
        ));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return S2C_PLAYER_ID;
        }

        S2CPlayerPacket toPacket() {
            return new S2CPlayerPacket(collisionSize, renderSize, flag, restitution, quaternion, currentPosition);
        }

        static S2CPlayerPayload fromPacket(S2CPlayerPacket packet) {
            return new S2CPlayerPayload(
                    packet.collisionSize(),
                    packet.renderSize(),
                    packet.flag(),
                    packet.restitution(),
                    new Quaternionf(packet.quaternion()),
                    packet.currentPosition()
            );
        }
    }
}
