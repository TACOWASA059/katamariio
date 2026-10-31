package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
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

    public BlockPos pos() {
        return this.pos;
    }

    public Vec3 playerPos() {
        return this.playerPos;
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
        if (serverPlayer == null || serverPlayer.isSpectator() || !KatamariConfig.get().attachBlocks) {
            return;
        }

        Level level = serverPlayer.level();
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (!KatamariConfig.get().isBlockAllowed(state)) {
            return;
        }

        Block block = state.getBlock();

        // Compute actual bounding box and center offset before removing the block
        net.minecraft.world.phys.AABB bounds = com.github.tacowasa059.katamariio.common.utils.KatamariBlockUtils.getBounds(state, level, pos);
        double targetCenterX = pos.getX() + (bounds.minX + bounds.maxX) * 0.5;
        double targetCenterY = pos.getY() + (bounds.minY + bounds.maxY) * 0.5;
        double targetCenterZ = pos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5;
        float extent = (float) Math.max(0.0625, Math.min(0.5, (bounds.maxY - bounds.minY) * 0.5));

        // Check if block has a container and drop its contents before removing
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof net.minecraft.world.Container container) {
            net.minecraft.world.Containers.dropContents(level, pos, container);
        }

        // Full neighbour updates so eaten blocks stay lively (snow/sand fall, water/lava flow into
        // the holes). The per-tick eat rate limit keeps the neighbour-update cost bounded.
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.removeBlockEntity(pos);

        ICustomPlayerData playerData = (ICustomPlayerData) serverPlayer;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        // Validate reach distance on server to prevent faraway block pickup
        float outerRadius = playerData.katamariIO$getOuterRadius();
        Vec3 serverPos = serverPlayer.position();
        double distSq = serverPos.distanceToSqr(targetCenterX, targetCenterY, targetCenterZ);
        double maxReach = outerRadius + 3.5;
        if (distSq > maxReach * maxReach) {
            return;
        }

        float radius = playerData.katamariIO$getSize() / 2.0f;
        Vec3 center = serverPos.add(0, radius, 0);
        Vec3 relativePosWorld = new Vec3(targetCenterX, targetCenterY, targetCenterZ).subtract(center);
        Quaternionf inverseBallRotation = new Quaternionf(playerData.katamariIO$getQuaternion()).invert();
        Vector3f relativePosLocal = new Vector3f((float) relativePosWorld.x, (float) relativePosWorld.y, (float) relativePosWorld.z);
        relativePosLocal.rotate(inverseBallRotation);

        // Snap attached block distance directly to current AABB collision radius R
        // Embed slightly by extent * 0.2f so the block sits flush on the collision shell surface
        float dist = relativePosLocal.length();
        if (dist > 1.0E-6f) {
            float nx = relativePosLocal.x / dist;
            float ny = relativePosLocal.y / dist;
            float nz = relativePosLocal.z / dist;
            float targetDist = Math.max(0.5f, radius - extent * 0.2f);
            relativePosLocal.set(nx * targetDist, ny * targetDist, nz * targetDist);
        }

        playerData.katamariIO$addBlock(block, inverseBallRotation,
                new Vec3(relativePosLocal.x, relativePosLocal.y, relativePosLocal.z), extent);
    }
}
