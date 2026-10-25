package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.client.NeighboringBlockUtils;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.google.common.collect.AbstractIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin<T> extends AbstractIterator<T> {
    @Unique
    private Entity katamariIO$entity;

    @Mutable @Final @Shadow private AABB box;
    @Final @Shadow private CollisionContext context;
    @Mutable @Final @Shadow private Cursor3D cursor;
    @Final @Shadow private BlockPos.MutableBlockPos pos;
    @Mutable @Final @Shadow private VoxelShape entityShape;
    @Final @Shadow private CollisionGetter collisionGetter;
    @Final @Shadow private boolean onlySuffocatingBlocks;
    @Final @Shadow private BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider;

    @Shadow
    protected abstract BlockGetter getChunk(int x, int z);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CollisionGetter collisionGetter, Entity entity, AABB box, boolean onlySuffocatingBlocks,
                        BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider, CallbackInfo ci) {
        katamariIO$entity = entity;

        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag() && !player.isSpectator()) {
            float outerRadius = ((ICustomPlayerData) player).katamariIO$getOuterRadius();
            double inflation = Math.max(1.0, outerRadius - (box.getXsize() / 2.0) + 0.5);
            inflation = Math.min(16.0, inflation);
            AABB expanded = box.inflate(inflation);
            int minX = Mth.floor(expanded.minX - 1.0E-7) - 1;
            int maxX = Mth.floor(expanded.maxX + 1.0E-7) + 1;
            int minY = Mth.floor(expanded.minY - 1.0E-7) - 1;
            int maxY = Mth.floor(expanded.maxY + 1.0E-7) + 1;
            int minZ = Mth.floor(expanded.minZ - 1.0E-7) - 1;
            int maxZ = Mth.floor(expanded.maxZ + 1.0E-7) + 1;
            this.cursor = new Cursor3D(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    @Inject(method = "computeNext", at = @At("HEAD"), cancellable = true)
    public void computeNext(CallbackInfoReturnable<T> cir) {
        if (katamariIO$entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag() && !player.isSpectator()) {
            float ballRadius = player.getDimensions(Pose.STANDING).height / 2.0f;
            float outerRadius = ((ICustomPlayerData) player).katamariIO$getOuterRadius();
            Vec3 ballCenter = player.position().add(0.0, ballRadius, 0.0);
            double feetY = player.position().y;
            double maxEatRadius = Math.max(ballRadius, outerRadius) + 0.35;
            double maxRadiusSq = maxEatRadius * maxEatRadius;
            while (true) {
                if (!this.cursor.advance()) {
                    cir.setReturnValue(this.endOfData());
                    return;
                }

                int x = this.cursor.nextX();
                int y = this.cursor.nextY();
                int z = this.cursor.nextZ();
                int type = this.cursor.getNextType();
                if (type == 3) {
                    continue;
                }

                // Cells above the ball's feet that lie entirely outside the pickup sphere can be
                // neither a floor collision nor a pickup, so skip the block-state and collision-shape
                // lookups for them. Cells at or below feet are kept for floor support regardless.
                if (y >= feetY && katamariIO$isCellOutsideBall(x, y, z, ballCenter, maxRadiusSq)) {
                    continue;
                }

                BlockGetter chunk = this.getChunk(x, z);
                if (chunk == null) {
                    continue;
                }

                this.pos.set(x, y, z);
                BlockState state = chunk.getBlockState(this.pos);
                if (this.onlySuffocatingBlocks && !state.isSuffocating(chunk, this.pos) || type == 1 && !state.hasLargeCollisionShape() || type == 2 && !state.is(Blocks.MOVING_PISTON)) {
                    continue;
                }

                VoxelShape voxelShape = state.getCollisionShape(this.collisionGetter, this.pos, this.context);
                if (voxelShape == Shapes.block()) {
                    if (!this.box.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0)) {
                        continue;
                    }
                    voxelShape = voxelShape.move(x, y, z);
                    T result = this.resultProvider.apply(this.pos, voxelShape);
                    if (katamariIO$handleCollision(player, state, voxelShape.bounds(), ballRadius, ballCenter, feetY, x, y, z)) {
                        cir.setReturnValue(result);
                        return;
                    }
                    continue;
                }

                VoxelShape movedShape = voxelShape.move(x, y, z);
                if (movedShape.isEmpty()) {
                    // No collision shape (plants, torches, redstone, etc.): the katamari still eats
                    // them if within the pickup radius, even though they never produce a physics
                    // collision. Air and fluids are left alone.
                    if (player.level().isClientSide && katamariIO$canAttachBlocks(player)) {
                        katamariIO$tryEatShapeless(player, ballRadius, ballCenter, state, x, y, z);
                    }
                    continue;
                }
                if (!Shapes.joinIsNotEmpty(movedShape, this.entityShape, BooleanOp.AND)) {
                    continue;
                }

                AABB shapeBounds = movedShape.bounds();
                // Special-shaped blocks (fences, walls, iron bars, panes, chains, ...) are either taller
                // than a full block or do not fill the cell horizontally. Colliding with them snags the
                // rolling ball, so the katamari eats them and rolls through instead. Full-width shapes no
                // taller than a block (slabs, soul sand, farmland, snow, ...) stay solid so the ball can
                // still rest on them.
                if (!katamariIO$isFloorLikeShape(shapeBounds)) {
                    if (!com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().isBlockAllowed(state)
                            || !katamariIO$canAttachBlocks(player)) {
                        T result = this.resultProvider.apply(this.pos, movedShape);
                        cir.setReturnValue(result);
                        return;
                    }
                    if (player.level().isClientSide) {
                        katamariIO$tryEat(player, state, ballRadius, ballCenter, shapeBounds, x, y, z);
                    }
                    continue;
                }

                T result = this.resultProvider.apply(this.pos, movedShape);
                if (katamariIO$handleCollision(player, state, shapeBounds, ballRadius, ballCenter, feetY, x, y, z)) {
                    cir.setReturnValue(result);
                    return;
                }
            }
        }

        while (true) {
            if (!this.cursor.advance()) {
                cir.setReturnValue(this.endOfData());
                return;
            }

            int x = this.cursor.nextX();
            int y = this.cursor.nextY();
            int z = this.cursor.nextZ();
            int type = this.cursor.getNextType();
            if (type == 3) {
                continue;
            }

            BlockGetter chunk = this.getChunk(x, z);
            if (chunk == null) {
                continue;
            }

            this.pos.set(x, y, z);
            BlockState state = chunk.getBlockState(this.pos);
            if (this.onlySuffocatingBlocks && !state.isSuffocating(chunk, this.pos) || type == 1 && !state.hasLargeCollisionShape() || type == 2 && !state.is(Blocks.MOVING_PISTON)) {
                continue;
            }

            VoxelShape shape = state.getCollisionShape(this.collisionGetter, this.pos, this.context);
            if (shape == Shapes.block()) {
                if (!this.box.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0)) {
                    continue;
                }
                cir.setReturnValue(this.resultProvider.apply(this.pos, shape.move(x, y, z)));
                return;
            }

            VoxelShape movedShape = shape.move(x, y, z);
            if (movedShape.isEmpty() || !Shapes.joinIsNotEmpty(movedShape, this.entityShape, BooleanOp.AND)) {
                continue;
            }

            cir.setReturnValue(this.resultProvider.apply(this.pos, movedShape));
            return;
        }
    }

    @Unique
    private static boolean katamariIO$canAttachBlocks(Player player) {
        return com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().attachBlocks
                && (!player.isPassenger() || com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().attachWhileRiding);
    }

    @Unique
    private boolean katamariIO$handleCollision(Player player, BlockState state, AABB aabb, double radius, Vec3 center, double feetY, int x, int y, int z) {
        // Blocks strictly below feet are treated as floor support: keep them solid and never consume them.
        if (aabb.minY < feetY) {
            return true;
        }

        if (!com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().isBlockAllowed(state)
                || !katamariIO$canAttachBlocks(player)) {
            return true;
        }

        // Closest point of the block bounds to the ball centre via a cheap AABB clamp
        double cx = Math.max(aabb.minX, Math.min(center.x, aabb.maxX));
        double cy = Math.max(aabb.minY, Math.min(center.y, aabb.maxY));
        double cz = Math.max(aabb.minZ, Math.min(center.z, aabb.maxZ));
        double dx = cx - center.x;
        double dy = cy - center.y;
        double dz = cz - center.z;
        double eatRadius = katamariIO$getEatRadius(player, radius, dx, dy, dz);
        boolean insideBall = (dx * dx + dy * dy + dz * dz <= eatRadius * eatRadius);

        if (insideBall && player.level().isClientSide) {
            NeighboringBlockUtils.processNeighboring(player, new BlockPos(x, y, z));
        }

        // If the block is contacted by the ball sphere, do not treat it as a solid collision,
        // preventing the vanilla physics engine from pushing the player upwards (suffocation/step-up).
        if (insideBall) {
            return false;
        }

        return false;
    }

    @Unique
    private static double katamariIO$getEatRadius(Player player, double fallbackRadius, double dx, double dy, double dz) {
        if (player instanceof ICustomPlayerData data) {
            double lenSq = dx * dx + dy * dy + dz * dz;
            if (lenSq > 1.0E-6) {
                double len = Math.sqrt(lenSq);
                Quaternionf invRot = new Quaternionf(data.katamariIO$getQuaternion()).invert();
                Vector3f localDir = new Vector3f((float) (dx / len), (float) (dy / len), (float) (dz / len)).rotate(invRot);
                float surfaceRadius = data.katamariIO$getRadiusInDirection(localDir.x, localDir.y, localDir.z);
                return Math.max(fallbackRadius, surfaceRadius) + 0.35;
            }
        }
        return fallbackRadius + 0.35;
    }

    @Unique
    private static final double KATAMARI_FLOOR_SHAPE_EPSILON = 1.0E-3;

    @Unique
    private static boolean katamariIO$isFloorLikeShape(AABB bounds) {
        // Solid enough to stand on: no taller than a full block and filling the whole cell footprint.
        // Anything else (taller than a block, or narrower than the cell) is a special obstacle shape.
        return bounds.maxY - bounds.minY <= 1.0 + KATAMARI_FLOOR_SHAPE_EPSILON
                && bounds.maxX - bounds.minX >= 1.0 - KATAMARI_FLOOR_SHAPE_EPSILON
                && bounds.maxZ - bounds.minZ >= 1.0 - KATAMARI_FLOOR_SHAPE_EPSILON;
    }

    @Unique
    private void katamariIO$tryEat(Player player, BlockState state, double radius, Vec3 center, AABB aabb, int x, int y, int z) {
        if (aabb.minY < player.position().y) {
            return;
        }
        if (!com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().isBlockAllowed(state)
                || !katamariIO$canAttachBlocks(player)) {
            return;
        }
        double cx = Math.max(aabb.minX, Math.min(center.x, aabb.maxX));
        double cy = Math.max(aabb.minY, Math.min(center.y, aabb.maxY));
        double cz = Math.max(aabb.minZ, Math.min(center.z, aabb.maxZ));
        double dx = cx - center.x;
        double dy = cy - center.y;
        double dz = cz - center.z;
        double eatRadius = katamariIO$getEatRadius(player, radius, dx, dy, dz);
        if (dx * dx + dy * dy + dz * dz <= eatRadius * eatRadius) {
            NeighboringBlockUtils.processNeighboring(player, new BlockPos(x, y, z));
        }
    }

    @Unique
    private void katamariIO$tryEatShapeless(Player player, double radius, Vec3 center, BlockState state, int x, int y, int z) {
        if (y < (int) Math.floor(player.position().y)) {
            return;
        }
        if (!com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().isBlockAllowed(state)
                || !katamariIO$canAttachBlocks(player)) {
            return;
        }
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return;
        }
        double cx = Math.max(x, Math.min(center.x, x + 1.0));
        double cy = Math.max(y, Math.min(center.y, y + 1.0));
        double cz = Math.max(z, Math.min(center.z, z + 1.0));
        double dx = cx - center.x;
        double dy = cy - center.y;
        double dz = cz - center.z;
        double eatRadius = katamariIO$getEatRadius(player, radius, dx, dy, dz);
        if (dx * dx + dy * dy + dz * dz <= eatRadius * eatRadius) {
            NeighboringBlockUtils.processNeighboring(player, new BlockPos(x, y, z));
        }
    }

    @Unique
    private static boolean katamariIO$isCellOutsideBall(int x, int y, int z, Vec3 center, double radiusSq) {
        double cx = Math.max(x, Math.min(center.x, x + 1.0));
        double cy = Math.max(y, Math.min(center.y, y + 1.0));
        double cz = Math.max(z, Math.min(center.z, z + 1.0));
        double dx = cx - center.x;
        double dy = cy - center.y;
        double dz = cz - center.z;
        return dx * dx + dy * dy + dz * dz > radiusSq;
    }

    public T computeNext() {
        return null;
    }
}
