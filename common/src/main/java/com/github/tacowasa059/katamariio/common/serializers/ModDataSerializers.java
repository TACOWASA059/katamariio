package com.github.tacowasa059.katamariio.common.serializers;

import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

public final class ModDataSerializers {
    public static final EntityDataSerializer<KatamariBlocks> KATAMARI_BLOCKS = new KatamariBlocksSerializer();
    public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG = new EntityDataSerializer<>() {
        private static final StreamCodec<RegistryFriendlyByteBuf, CompoundTag> CODEC =
                StreamCodec.of((buf, tag) -> buf.writeNbt(tag), buf -> {
                    CompoundTag tag = buf.readNbt();
                    return tag != null ? tag : new CompoundTag();
                });

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, CompoundTag> codec() {
            return CODEC;
        }

        @Override
        public CompoundTag copy(CompoundTag value) {
            return value.copy();
        }
    };

    private ModDataSerializers() {
    }

    public static void registerVanilla() {
        EntityDataSerializers.registerSerializer(KATAMARI_BLOCKS);
        EntityDataSerializers.registerSerializer(COMPOUND_TAG);
    }
}
