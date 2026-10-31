package com.github.tacowasa059.katamariio.common.serializers;

import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

public final class ModDataSerializers {
    public static final EntityDataSerializer<KatamariBlocks> KATAMARI_BLOCKS = new KatamariBlocksSerializer();

    private ModDataSerializers() {
    }

    public static void registerVanilla() {
        EntityDataSerializers.registerSerializer(KATAMARI_BLOCKS);
    }
}