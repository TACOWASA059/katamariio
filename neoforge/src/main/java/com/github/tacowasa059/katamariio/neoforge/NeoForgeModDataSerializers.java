package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeModDataSerializers {
    public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, KatamariIO.MODID);

    static {
        ENTITY_DATA_SERIALIZERS.register("vec3_list", () -> ModDataSerializers.VEC3_LIST);
        ENTITY_DATA_SERIALIZERS.register("quaternion_list", () -> ModDataSerializers.QUATERNION_LIST);
        ENTITY_DATA_SERIALIZERS.register("block_list", () -> ModDataSerializers.BLOCK_LIST);
    }

    private NeoForgeModDataSerializers() {
    }
}
