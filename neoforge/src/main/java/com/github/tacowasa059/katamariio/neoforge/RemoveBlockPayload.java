package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record RemoveBlockPayload(BlockPos pos, Vec3 playerPos) implements CustomPacketPayload {
    public static final Type<RemoveBlockPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "remove_block"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveBlockPayload> STREAM_CODEC = StreamCodec.of((buf, payload) -> {
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
        return TYPE;
    }

    public RemoveBlockPacket toPacket() {
        return new RemoveBlockPacket(this.pos, this.playerPos);
    }

    public static RemoveBlockPayload fromPacket(RemoveBlockPacket packet) {
        return new RemoveBlockPayload(packet.pos(), packet.playerPos());
    }
}
