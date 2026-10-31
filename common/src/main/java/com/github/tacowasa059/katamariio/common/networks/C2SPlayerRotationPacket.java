package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Quaternionf;

public class C2SPlayerRotationPacket {
    private final Quaternionf quaternion;

    public C2SPlayerRotationPacket(Quaternionf quaternion) {
        this.quaternion = quaternion;
    }

    public static void encode(C2SPlayerRotationPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.quaternion.x);
        buf.writeFloat(packet.quaternion.y);
        buf.writeFloat(packet.quaternion.z);
        buf.writeFloat(packet.quaternion.w);
    }

    public static C2SPlayerRotationPacket decode(FriendlyByteBuf buf) {
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float w = buf.readFloat();
        return new C2SPlayerRotationPacket(new Quaternionf(x, y, z, w));
    }

    public void handle(ServerPlayer player) {
        if (player == null) return;
        if (player instanceof ICustomPlayerData data) {
            data.katamariIO$setQuaternion(quaternion);
        }
    }

    public Quaternionf getQuaternion() {
        return quaternion;
    }
}
