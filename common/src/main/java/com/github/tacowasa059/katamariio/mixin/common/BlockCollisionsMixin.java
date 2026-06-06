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

import java.util.Optional;
import java.util.function.BiFunction;

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

        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            AABB expanded = box.expandTowards(1, 0, 0).expandTowards(-1, 0, 0)
                    .expandTowards(0, 0, 1).expandTowards(0, 0, -1);
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
        if (katamariIO$entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
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

                VoxelShape voxelShape = state.getCollisionShape(this.collisionGetter, this.pos, this.context);
                if (voxelShape == Shapes.block()) {
                    if (!this.box.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0)) {
                        continue;
                    }
                    voxelShape = voxelShape.move(x, y, z);
                    T result = this.resultProvider.apply(this.pos, voxelShape);
                    if (katamariIO$handleCollision(player, voxelShape.bounds(), voxelShape, x, y, z)) {
                        cir.setReturnValue(result);
                        return;
                    }
                    continue;
                }

                VoxelShape movedShape = voxelShape.move(x, y, z);
                if (movedShape.isEmpty() || !Shapes.joinIsNotEmpty(movedShape, this.entityShape, BooleanOp.AND)) {
                    continue;
                }

                T result = this.resultProvider.apply(this.pos, voxelShape);
                if (katamariIO$handleCollision(player, movedShape.bounds(), movedShape, x, y, z)) {
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
    private boolean katamariIO$handleCollision(Player player, AABB aabb, VoxelShape shape, int x, int y, int z) {
        float radius = player.getDimensions(Pose.STANDING).height / 2.0f;
        Vec3 center = player.position().add(0, radius, 0);
        Optional<Vec3> closest = shape.closestPointTo(center);

        if (aabb.minY < player.position().y) {
            return true;
        }
        if (closest.isPresent() && closest.get().distanceTo(center) <= radius && player.level().isClientSide) {
            NeighboringBlockUtils.processNeighboring(player, new BlockPos(x, y, z));
        }
        return false;
    }

    public T computeNext() {
        return null;
    }
}
