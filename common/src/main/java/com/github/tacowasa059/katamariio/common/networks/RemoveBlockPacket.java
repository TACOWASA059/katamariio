package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RemoveBlockPacket {
    private final BlockPos pos;
    private final Vec3 playerPos;

    public RemoveBlockPacket(BlockPos pos, Vec3 loc) {
        this.pos = pos;
        this.playerPos = loc;
    }

    public static RemoveBlockPacket decode(FriendlyByteBuf buf) {
        return new RemoveBlockPacket(buf.readBlockPos(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeDouble(playerPos.x);
        buf.writeDouble(playerPos.y);
        buf.writeDouble(playerPos.z);
    }

    public void handle(ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            return;
        }

        Level level = serverPlayer.level();
        Block block = level.getBlockState(pos).getBlock();
        if (block.equals(Blocks.AIR) || block.equals(Blocks.CAVE_AIR) || block.equals(Blocks.VOID_AIR)) {
            return;
        }

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.removeBlockEntity(pos);

        ICustomPlayerData playerData = (ICustomPlayerData) serverPlayer;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        float radius = playerData.katamariIO$getSize() / 2.0f;
        Vec3 center = playerPos.add(0, radius, 0);
        Vec3 relativePosWorld = new Vec3(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f).subtract(center);
        Quaternionf inverseBallRotation = new Quaternionf(playerData.katamariIO$getQuaternion()).invert();
        Vector3f relativePosLocal = new Vector3f((float) relativePosWorld.x, (float) relativePosWorld.y, (float) relativePosWorld.z);
        relativePosLocal.rotate(inverseBallRotation);

        playerData.katamariIO$addBlock(block, inverseBallRotation,
                new Vec3(relativePosLocal.x, relativePosLocal.y, relativePosLocal.z));
    }
}
