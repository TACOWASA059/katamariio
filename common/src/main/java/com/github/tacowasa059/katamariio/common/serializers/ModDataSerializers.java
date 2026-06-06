package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;

public class ModDataSerializers {
    public static final EntityDataSerializer<List<Vec3>> VEC3_LIST = new Vec3ListSerializer();
    public static final EntityDataSerializer<List<Quaternionf>> QUATERNION_LIST = new QuaternionfListSerializer();
    public static final EntityDataSerializer<List<Block>> BLOCK_LIST = new BlockListSerializer();

    public static void register() {
        // EntityDataSerializer の登録
        EntityDataSerializers.registerSerializer(VEC3_LIST);
        EntityDataSerializers.registerSerializer(QUATERNION_LIST);
        EntityDataSerializers.registerSerializer(BLOCK_LIST);
    }
}
