package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public record S2CPlayerPayload(float collisionSize, float renderSize, boolean flag, float restitution,
                               Quaternionf quaternion, Vec3 currentPosition) implements CustomPacketPayload {
    public static final Type<S2CPlayerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "player_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayerPayload> STREAM_CODEC = StreamCodec.of((buf, payload) -> {
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
        return TYPE;
    }

    public S2CPlayerPacket toPacket() {
        return new S2CPlayerPacket(collisionSize, renderSize, flag, restitution, quaternion, currentPosition);
    }

    public static S2CPlayerPayload fromPacket(S2CPlayerPacket packet) {
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
