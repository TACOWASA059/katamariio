package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class S2CPlayerPacket {
    private final float collisionSize;
    private final float renderSize;
    private final boolean flag;
    private final float restitution;
    private final Quaternionf quaternion;
    private final Vec3 currentPosition;

    public S2CPlayerPacket(float collisionSize, float renderSize, boolean flag, float restitution, @Nonnull Quaternionf quaternion, @Nonnull Vec3 currentPosition) {
        this.collisionSize = collisionSize;
        this.renderSize = renderSize;
        this.flag = flag;
        this.restitution = restitution;
        this.quaternion = quaternion;
        this.currentPosition = currentPosition;
    }

    public static void encode(S2CPlayerPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.collisionSize);
        buf.writeFloat(packet.renderSize);
        buf.writeBoolean(packet.flag);
        buf.writeFloat(packet.restitution);
        buf.writeFloat(packet.quaternion.x);
        buf.writeFloat(packet.quaternion.y);
        buf.writeFloat(packet.quaternion.z);
        buf.writeFloat(packet.quaternion.w);
        buf.writeDouble(packet.currentPosition.x);
        buf.writeDouble(packet.currentPosition.y);
        buf.writeDouble(packet.currentPosition.z);
    }

    public static S2CPlayerPacket decode(FriendlyByteBuf buf) {
        float collisionSize = buf.readFloat();
        float renderSize = buf.readFloat();
        boolean flag = buf.readBoolean();
        float restitution = buf.readFloat();
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float w = buf.readFloat();
        Quaternionf quaternion = new Quaternionf(x, y, z, w);
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new S2CPlayerPacket(collisionSize, renderSize, flag, restitution, quaternion, pos);
    }

    public static void handle(S2CPlayerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> applyToClientPlayer(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyToClientPlayer(S2CPlayerPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player instanceof ICustomPlayerData data) {
            data.katamariIO$setSize(packet.collisionSize);
            data.katamariIO$setRenderSize(packet.renderSize);
            data.katamariIO$setFlag(packet.flag);
            data.katamariIO$setRestitutionCoefficient(packet.restitution);

            data.katamariIO$setQuaternion(packet.quaternion);
            data.katamariIO$setCurrentPosition(packet.currentPosition);

            player.setPos(player.position());
        }
    }
}
