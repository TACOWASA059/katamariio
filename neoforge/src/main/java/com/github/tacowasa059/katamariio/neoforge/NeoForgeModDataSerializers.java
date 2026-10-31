package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeModDataSerializers {
    public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, KatamariIO.MODID);

    static {
        ENTITY_DATA_SERIALIZERS.register("katamari_blocks", () -> ModDataSerializers.KATAMARI_BLOCKS);
        ENTITY_DATA_SERIALIZERS.register("compound_tag", () -> ModDataSerializers.COMPOUND_TAG);
    }

    private NeoForgeModDataSerializers() {
    }
}
