package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockListSerializer implements EntityDataSerializer<List<Block>> {
    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, List<Block>> codec() {
        return StreamCodec.of((buf, blocks) -> {
            buf.writeVarInt(blocks.size());
            for (Block block : blocks) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                buf.writeIdentifier(id);
            }
        }, buf -> {
            int size = buf.readVarInt();
            List<Block> blocks = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Block block = BuiltInRegistries.BLOCK.getValue(buf.readIdentifier());
                if (block != null) {
                    blocks.add(block);
                }
            }
            return blocks;
        });
    }

    @Override
    public List<Block> copy(List<Block> blocks) {
        return new ArrayList<>(blocks);
    }
}
