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
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
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

    @Mutable
    @Final
    @Shadow
    private AABB box;
    @Final
    @Shadow
    private CollisionContext context;
    @Mutable
    @Final
    @Shadow
    private Cursor3D cursor;
    @Final
    @Shadow
    private BlockPos.MutableBlockPos pos;
    @Mutable
    @Final
    @Shadow
    private VoxelShape entityShape;
    @Final
    @Shadow
    private CollisionGetter collisionGetter;
    @Final
    @Shadow
    private boolean onlySuffocatingBlocks;
    @Final
    @Shadow
    private BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider;

    @Shadow
    protected abstract BlockGetter getChunk(int p_186412_, int p_186413_);

    @Inject(method = "<init>", at=@At("TAIL"))
    private void onInit(CollisionGetter p_286817_, Entity p_286246_, AABB p_286624_, boolean p_286354_,
                        BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> p_286303_, CallbackInfo ci){

        katamariIO$entity = p_286246_;

        if (katamariIO$entity!=null && katamariIO$entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            p_286624_ = p_286624_.expandTowards(1, 0, 0).expandTowards(-1, 0, 0)
                    .expandTowards(0, 0, 1).expandTowards(0, 0, -1);

            int $$5 = Mth.floor(p_286624_.minX - 1.0E-7) - 1;
            int $$6 = Mth.floor(p_286624_.maxX + 1.0E-7) + 1;
            int $$7 = Mth.floor(p_286624_.minY - 1.0E-7) - 1;
            int $$8 = Mth.floor(p_286624_.maxY + 1.0E-7) + 1;
            int $$9 = Mth.floor(p_286624_.minZ - 1.0E-7) - 1;
            int $$10 = Mth.floor(p_286624_.maxZ + 1.0E-7) + 1;
            this.cursor = new Cursor3D($$5, $$7, $$9, $$6, $$8, $$10);
        }

    }

    @Inject(method = "computeNext", at=@At("HEAD"), cancellable = true)
    public void computeNext(CallbackInfoReturnable<T> cir){
        if (katamariIO$entity!=null && katamariIO$entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()){
            while(true) {
                if (this.cursor.advance()) {
                    int $$0 = this.cursor.nextX();
                    int $$1 = this.cursor.nextY();
                    int $$2 = this.cursor.nextZ();
                    int $$3 = this.cursor.getNextType();
                    if ($$3 == 3) {
                        continue;
                    }

                    BlockGetter $$4 = this.getChunk($$0, $$2);
                    if ($$4 == null) {
                        continue;
                    }

                    this.pos.set($$0, $$1, $$2);
                    BlockState $$5 = $$4.getBlockState(this.pos);
                    if (this.onlySuffocatingBlocks && !$$5.isSuffocating($$4, this.pos) || $$3 == 1 && !$$5.hasLargeCollisionShape() || $$3 == 2 && !$$5.is(Blocks.MOVING_PISTON)) {
                        continue;
                    }


                    VoxelShape voxelShape = $$5.getCollisionShape(this.collisionGetter, this.pos, this.context);

                    if (voxelShape == Shapes.block()) {
                        if (!this.box.intersects($$0, $$1, $$2,
                                (double)$$0 + (double)1.0F, (double)$$1 + (double)1.0F, (double)$$2 + (double)1.0F)) {
                            continue;
                        } //デフォルトで弾くもの
                        voxelShape = voxelShape.move($$0, $$1, $$2);


                        AABB aabb = voxelShape.bounds();
                        float radius = player.getDimensions(Pose.STANDING).height/2.0f;
                        Vec3 center = player.position().add(0, radius,0);
                        Optional<Vec3> closest = voxelShape.closestPointTo(center);

                        T t = this.resultProvider.apply(this.pos, voxelShape);

                        if(aabb.minY < player.position().y){
                            cir.setReturnValue(t);
                            return;
                        }
                        else if(closest.isPresent() && closest.get().distanceTo(center) <= radius){
                            if(player.level().isClientSide) NeighboringBlockUtils.processNeighboring(player, new BlockPos($$0, $$1, $$2));
                        }
                        continue;
                    }

                    VoxelShape $$7 = voxelShape.move($$0, $$1, $$2);
                    if ($$7.isEmpty() || !Shapes.joinIsNotEmpty($$7, this.entityShape, BooleanOp.AND)) {
                        continue;
                    }//デフォルトで弾くもの

                    AABB aabb = $$7.bounds();
                    float radius = player.getDimensions(Pose.STANDING).height/2.0f;
                    Vec3 center = player.position().add(0, radius,0);
                    Optional<Vec3> closest = $$7.closestPointTo(center);

                    T t = this.resultProvider.apply(this.pos, voxelShape);

                    if(aabb.minY < player.position().y){
                        cir.setReturnValue(t);
                        return;
                    }
                    else if(closest.isPresent() && closest.get().distanceTo(center) <= radius){
                        if(player.level().isClientSide) NeighboringBlockUtils.processNeighboring(player, new BlockPos($$0, $$1, $$2));
                    }
                    continue;
                }

                cir.setReturnValue(this.endOfData());
                return;
            }
        }else{
            while(true) {
                if (this.cursor.advance()) {
                    int $$0 = this.cursor.nextX();
                    int $$1 = this.cursor.nextY();
                    int $$2 = this.cursor.nextZ();
                    int $$3 = this.cursor.getNextType();
                    if ($$3 == 3) {
                        continue;
                    }

                    BlockGetter $$4 = this.getChunk($$0, $$2);
                    if ($$4 == null) {
                        continue;
                    }

                    this.pos.set($$0, $$1, $$2);
                    BlockState $$5 = $$4.getBlockState(this.pos);
                    if (this.onlySuffocatingBlocks && !$$5.isSuffocating($$4, this.pos) || $$3 == 1 && !$$5.hasLargeCollisionShape() || $$3 == 2 && !$$5.is(Blocks.MOVING_PISTON)) {
                        continue;
                    }

                    VoxelShape $$6 = $$5.getCollisionShape(this.collisionGetter, this.pos, this.context);
                    if ($$6 == Shapes.block()) {
                        if (!this.box.intersects($$0, $$1, $$2, (double)$$0 + (double)1.0F, (double)$$1 + (double)1.0F, (double)$$2 + (double)1.0F)) {
                            continue;
                        }

                        cir.setReturnValue(this.resultProvider.apply(this.pos, $$6.move($$0, $$1, $$2)));
                        return;
                    }

                    VoxelShape $$7 = $$6.move($$0, $$1, $$2);
                    if ($$7.isEmpty() || !Shapes.joinIsNotEmpty($$7, this.entityShape, BooleanOp.AND)) {
                        continue;
                    }

                    cir.setReturnValue(this.resultProvider.apply(this.pos, $$7));
                    return;
                }

                cir.setReturnValue(this.endOfData());
                return;
            }
        }

    }


    protected @Nullable T computeNext() {
        return null;
    }
}

