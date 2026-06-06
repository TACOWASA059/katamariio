package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockListSerializer implements EntityDataSerializer<List<Block>> {
    @Override
    public void write(FriendlyByteBuf buf, List<Block> blocks) {
        buf.writeVarInt(blocks.size());
        for (Block block : blocks) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            buf.writeResourceLocation(id);
        }
    }

    @Override
    public List<Block> read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Block> blocks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            blocks.add(BuiltInRegistries.BLOCK.get(buf.readResourceLocation()));
        }
        return blocks;
    }

    @Override
    public List<Block> copy(List<Block> blocks) {
        return new ArrayList<>(blocks);
    }
}
