package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Vec3ListSerializer implements EntityDataSerializer<List<Vec3>> {
    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, List<Vec3>> codec() {
        return StreamCodec.of((buf, list) -> {
            buf.writeVarInt(list.size());
            for (Vec3 v : list) {
                buf.writeDouble(v.x);
                buf.writeDouble(v.y);
                buf.writeDouble(v.z);
            }
        }, buf -> {
            int size = buf.readVarInt();
            List<Vec3> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
            }
            return list;
        });
    }

    @Override
    public List<Vec3> copy(List<Vec3> list) {
        return new ArrayList<>(list);
    }
}
