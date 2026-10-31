package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @org.spongepowered.asm.mixin.Shadow
    protected boolean jumping;

    @org.spongepowered.asm.mixin.Shadow
    protected float zza;

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    public void modifyKnockback(double strength, double ratioX, double ratioZ, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player)) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) entity;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        strength = strength * (1.0f - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (strength <= 0.0F) {
            return;
        }

        entity.hasImpulse = true;
        Vec3 currentMotion = entity.getDeltaMovement();
        Vec3 knockbackVector = (new Vec3(ratioX, 0.0, ratioZ)).normalize().scale(strength);
        entity.setDeltaMovement(currentMotion.x / 2.0 - knockbackVector.x,
                currentMotion.y / 2.0 + strength / 2.0 + 0.1D, currentMotion.z / 2.0 - knockbackVector.z);
        ci.cancel();
    }

    @Inject(method = "getFrictionInfluencedSpeed", at = @At("HEAD"), cancellable = true)
    public void modifyFrictionInfluencedSpeed(float friction, CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player)) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) entity;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        if (!entity.onGround()) {
            cir.setReturnValue(entity.getSpeed() * (0.21600002F / (friction * friction * friction)));
        }
    }

    @Inject(method = "getJumpPower", at = @At("HEAD"), cancellable = true)
    protected void getJumpPower(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player)) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) entity;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        float jumpPower = 0.42F * this.katamariIO$getJumpFactor();
        jumpPower *= 1.75f;
        cir.setReturnValue(jumpPower);
        cir.cancel();
    }

    @Unique
    protected float katamariIO$getJumpFactor() {
        LivingEntity entity = (LivingEntity) (Object) this;
        float blockJumpFactor = entity.level().getBlockState(entity.blockPosition()).getBlock().getJumpFactor();
        BlockPos pos = this.getOnPos(0.500001F);
        float onPosJumpFactor = entity.level().getBlockState(pos).getBlock().getJumpFactor();
        return (double) blockJumpFactor == 1.0 ? onPosJumpFactor : blockJumpFactor;
    }

    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    public void isInWall(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "isAffectedByFluids", at = @At("HEAD"), cancellable = true)
    public void katamariIO$isAffectedByFluids(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            boolean submerged = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                    || player.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA);
            boolean wantsToDive = player.isShiftKeyDown() || player.isCrouching()
                    || (player.getXRot() > 50.0F && this.zza > 0.0F);

            // If grounded or partially touching fluid (not actively submerged/diving), ignore fluid hindrance
            if (player.onGround() || (!submerged && !wantsToDive)) {
                cir.setReturnValue(false);
            }
        }
    }

    // Only allow standing on fluids when not submerged and not trying to dive (sneaking or looking down sharply),
    // so players can roll across the water surface easily, while still being able to dive into the water naturally.
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    public void katamariIO$canStandOnFluid(FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            boolean submerged = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                    || player.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA);
            boolean wantsToDive = player.isShiftKeyDown() || player.isCrouching()
                    || (player.getXRot() > 50.0F && this.zza > 0.0F);

            if (submerged || wantsToDive) {
                cir.setReturnValue(false);
            } else {
                cir.setReturnValue(true);
            }
        }
    }

    // Provide responsive upward buoyancy and ascent when pressing jump in fluid,
    // responsive descent when sneaking or looking down to dive,
    // smooth water surface hovering support in deep water,
    // and seamless shore climbing assistance to prevent snagging and stalling.
    @Inject(method = "travel", at = @At("HEAD"))
    public void katamariIO$travelInFluid(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            if (player.getAbilities().flying) {
                return;
            }
            if (player.isInWater() || player.isInLava()) {
                Vec3 motion = player.getDeltaMovement();
                boolean diving = player.isShiftKeyDown() || player.isCrouching()
                        || (player.getXRot() > 50.0F && (this.zza > 0.0F || travelVector.z > 0.0D));
                boolean submerged = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                        || player.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA);

                boolean deepWater = player.level().getFluidState(player.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER)
                        || player.level().getFluidState(player.blockPosition().below()).is(net.minecraft.tags.FluidTags.LAVA);

                if (this.jumping) {
                    player.setDeltaMovement(motion.x, Math.max(motion.y + 0.08D, 0.32D), motion.z);
                } else if (diving) {
                    double diveSpeed = player.getXRot() > 50.0F ? -0.22D : -0.28D;
                    player.setDeltaMovement(motion.x, Math.min(motion.y - 0.08D, diveSpeed), motion.z);
                } else if (!submerged && deepWater && !player.onGround()) {
                    // Surface hovering support: only smoothly lift player when actually floating in deep water.
                    // Keep normal ground contact when onGround or in shallow water/shores.
                    player.setDeltaMovement(motion.x, Math.max(motion.y, 0.04D), motion.z);
                } else if (!player.onGround() && motion.y < -0.15D && deepWater) {
                    player.setDeltaMovement(motion.x, -0.15D, motion.z);
                }

                // If moving towards an obstacle/shore while in water, assist climbing out onto land
                if (player.horizontalCollision && (this.zza > 0.0F || travelVector.z > 0.0D || motion.horizontalDistanceSqr() > 1.0E-4)) {
                    player.setDeltaMovement(motion.x, Math.max(motion.y, 0.28D), motion.z);
                }
            }
        }
    }

    @Inject(method = "travel", at = @At("TAIL"))
    public void katamariIO$travelTail(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            if (player.getAbilities().flying) {
                return;
            }
            if (player.isInWater() || player.isInLava()) {
                boolean submerged = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                        || player.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA);
                boolean diving = player.isShiftKeyDown() || player.isCrouching()
                        || (player.getXRot() > 50.0F && (this.zza > 0.0F || travelVector.z > 0.0D));

                boolean deepWater = player.level().getFluidState(player.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER)
                        || player.level().getFluidState(player.blockPosition().below()).is(net.minecraft.tags.FluidTags.LAVA);

                // Only clamp downward motion if floating on deep water surface.
                // Never clamp downward motion when grounded or in shallow water, allowing normal landing and step-up.
                if (!submerged && !diving && !this.jumping && deepWater && !player.onGround()) {
                    Vec3 motion = player.getDeltaMovement();
                    if (motion.y < 0.0D) {
                        player.setDeltaMovement(motion.x, 0.0D, motion.z);
                    }
                }
            }
        }
    }

    @Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
    protected void katamariIO$doPush(Entity entity, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            ci.cancel();
        } else if (entity instanceof Player otherPlayer && ((ICustomPlayerData) otherPlayer).katamariIO$getFlag()) {
            ci.cancel();
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void katamariIO$isPushable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "rideTick", at = @At("HEAD"), cancellable = true)
    public void katamariIO$rideTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getVehicle() instanceof Player parentPlayer && ((ICustomPlayerData) parentPlayer).katamariIO$getFlag()) {
            super.rideTick();
            ci.cancel();
        }
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            float size = ((ICustomPlayerData) player).katamariIO$getSize();
            cir.setReturnValue(
                    EntityDimensions.scalable(size, size)
                            .withEyeHeight(0.85F * size)
                            .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, new Vec3(0.0, 0.15, 0.0)))
            );
        }
    }
}