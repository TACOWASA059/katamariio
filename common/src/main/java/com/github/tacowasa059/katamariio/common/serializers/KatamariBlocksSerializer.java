package com.github.tacowasa059.katamariio.common.serializers;

import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class KatamariBlocksSerializer implements EntityDataSerializer<KatamariBlocks> {
    @Override
    public void write(FriendlyByteBuf buf, KatamariBlocks data) {
        int size = data.size();
        buf.writeVarInt(size);
        if (size == 0) {
            return;
        }

        // Positions: 16-bit relative to one shared scale (max magnitude). Rotations: 8-bit per
        // component. Blocks: session registry id (varint). ~12 bytes per block on the wire.
        float maxAbs = 0.0f;
        for (int i = 0; i < size; i++) {
            maxAbs = Math.max(maxAbs,
                    Math.max(Math.abs(data.posX(i)), Math.max(Math.abs(data.posY(i)), Math.abs(data.posZ(i)))));
        }
        buf.writeFloat(maxAbs);

        float inv = maxAbs > 0.0f ? 32767.0f / maxAbs : 0.0f;
        for (int i = 0; i < size; i++) {
            buf.writeShort(Math.round(data.posX(i) * inv));
            buf.writeShort(Math.round(data.posY(i) * inv));
            buf.writeShort(Math.round(data.posZ(i) * inv));
            buf.writeByte(quantize(data.quatX(i)));
            buf.writeByte(quantize(data.quatY(i)));
            buf.writeByte(quantize(data.quatZ(i)));
            buf.writeByte(quantize(data.quatW(i)));
            buf.writeId(BuiltInRegistries.BLOCK, data.block(i));
        }
    }

    @Override
    public KatamariBlocks read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        KatamariBlocks data = new KatamariBlocks(Math.max(1, size));
        if (size == 0) {
            return data;
        }

        float scale = buf.readFloat() / 32767.0f;
        for (int i = 0; i < size; i++) {
            float x = buf.readShort() * scale;
            float y = buf.readShort() * scale;
            float z = buf.readShort() * scale;
            float qx = buf.readByte() / 127.0f;
            float qy = buf.readByte() / 127.0f;
            float qz = buf.readByte() / 127.0f;
            float qw = buf.readByte() / 127.0f;
            float len2 = qx * qx + qy * qy + qz * qz + qw * qw;
            if (len2 > 1.0E-6f) {
                float invLen = 1.0f / (float) Math.sqrt(len2);
                qx *= invLen;
                qy *= invLen;
                qz *= invLen;
                qw *= invLen;
            } else {
                qx = 0.0f;
                qy = 0.0f;
                qz = 0.0f;
                qw = 1.0f;
            }
            Block block = buf.readById(BuiltInRegistries.BLOCK);
            data.addRaw(block != null ? block : Blocks.AIR, qx, qy, qz, qw, x, y, z);
        }
        return data;
    }

    @Override
    public KatamariBlocks copy(KatamariBlocks value) {
        return new KatamariBlocks(value);
    }

    private static byte quantize(float value) {
        float clamped = Math.max(-1.0f, Math.min(1.0f, value));
        return (byte) Math.round(clamped * 127.0f);
    }
}
