package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class QuaternionfListSerializer implements EntityDataSerializer<List<Quaternionf>> {
    @Override
    public void write(FriendlyByteBuf buf, List<Quaternionf> list) {
        buf.writeVarInt(list.size());
        for (Quaternionf q : list) {
            buf.writeFloat(q.x());
            buf.writeFloat(q.y());
            buf.writeFloat(q.z());
            buf.writeFloat(q.w());
        }
    }

    @Override
    public List<Quaternionf> read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Quaternionf> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
        }
        return list;
    }

    @Override
    public List<Quaternionf> copy(List<Quaternionf> list) {
        List<Quaternionf> copy = new ArrayList<>(list.size());
        for (Quaternionf q : list) {
            copy.add(new Quaternionf(q));
        }
        return copy;
    }
}
