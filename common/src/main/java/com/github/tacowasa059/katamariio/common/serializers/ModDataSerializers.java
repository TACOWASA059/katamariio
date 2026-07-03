package com.github.tacowasa059.katamariio.common.serializers;

import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

public class ModDataSerializers {
    public static final EntityDataSerializer<KatamariBlocks> KATAMARI_BLOCKS = new KatamariBlocksSerializer();

    public static void register() {
        EntityDataSerializers.registerSerializer(KATAMARI_BLOCKS);
    }
}
