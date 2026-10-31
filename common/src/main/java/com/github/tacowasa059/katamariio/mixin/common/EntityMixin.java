package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private Vec3 katamariIO$previousPosition;

    @Inject(method = "baseTick", at = @At("HEAD"))
    public void baseTick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player) {
            katamariIO$updateQuaternion(player);
            katamariIO$updateStepHeight(player);
        }
    }

    @Unique
    private void katamariIO$updateStepHeight(Player player) {
        ICustomPlayerData data = (ICustomPlayerData) player;
        if (data.katamariIO$getFlag()) {
            float size = data.katamariIO$getSize();
            float step = Math.max(1.25F, Math.min(3.0F, size * 0.5F));
            katamariIO$setStepHeight(player, step);
        } else if (katamariIO$getStepHeight(player) > 0.6F) {
            katamariIO$setStepHeight(player, 0.6F);
        }
    }

    // 1.20.5+ removed Entity.maxUpStep; step height is the STEP_HEIGHT attribute base value.
    @Unique
    private static double katamariIO$getStepHeight(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.STEP_HEIGHT);
        return attribute != null ? attribute.getBaseValue() : 0.6;
    }

    @Unique
    private static void katamariIO$setStepHeight(Player player, double step) {
        AttributeInstance attribute = player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute != null && attribute.getBaseValue() != step) {
            attribute.setBaseValue(step);
        }
    }

    @Unique
    private void katamariIO$updateQuaternion(Player player) {
        Level world = player.level();
        if (!world.isClientSide()) {
            ICustomPlayerData playerData = (ICustomPlayerData) player;
            if (!playerData.katamariIO$getFlag()) {
                return;
            }
            if (player.getVehicle() != null) {
                // When riding any vehicle (boat, horse, or katamari), no separate rolling rotation is performed
                return;
            }

            Vec3 currentPosition = player.position();
            Quaternionf rotation = QuaternionUtils.getUpdatedQuaternion(currentPosition, katamariIO$previousPosition, playerData);
            katamariIO$previousPosition = currentPosition;
            playerData.katamariIO$setCurrentPosition(player.position());
            if (rotation != null) {
                playerData.katamariIO$setQuaternion(rotation);
            }
        }
    }

    @Inject(method = "checkInsideBlocks(Ljava/util/List;Lnet/minecraft/world/entity/InsideBlockEffectApplier$StepBasedCollector;)V", at = @At("HEAD"), cancellable = true)
    public void checkInsideBlocks(List<Object> movements, InsideBlockEffectApplier.StepBasedCollector stepBasedCollector, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            ci.cancel();
        } else if (entity.getVehicle() instanceof Player vehiclePlayer && ((ICustomPlayerData) vehiclePlayer).katamariIO$getFlag()) {
            ci.cancel();
        }
    }

    @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;)F", at = @At("HEAD"), cancellable = true)
    public void getEyeHeight(Pose pose, CallbackInfoReturnable<Float> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            cir.setReturnValue(0.85F * ((ICustomPlayerData) player).katamariIO$getSize());
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    public void katamariIO$onPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            ci.cancel();
        } else if (other instanceof Player otherPlayer && ((ICustomPlayerData) otherPlayer).katamariIO$getFlag()) {
            ci.cancel();
        }
    }

    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
    protected void katamariIO$canAddPassenger(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            cir.setReturnValue(KatamariEntityAttacher.hasCapacity(self.getPassengers().size()));
        }
    }

    @Inject(method = "couldAcceptPassenger", at = @At("HEAD"), cancellable = true)
    protected void katamariIO$couldAcceptPassenger(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            cir.setReturnValue(KatamariEntityAttacher.hasCapacity(self.getPassengers().size()));
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z")
    )
    private boolean katamariIO$allowRidingKatamariPlayer(net.minecraft.world.entity.EntityType<?> entityType, Entity vehicle, boolean force, boolean sendGameEvent) {
        if (vehicle instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            return true;
        }
        return entityType.canSerialize();
    }

    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", at = @At("HEAD"), cancellable = true)
    protected void katamariIO$positionRider(Entity passenger, Entity.MoveFunction moveFunction, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            Vector3f localPos = KatamariEntityAttacher.getLocalPos(player, passenger);
            if (localPos != null) {
                ICustomPlayerData data = (ICustomPlayerData) player;
                float radius = data.katamariIO$getSize() / 2.0f;
                Vec3 ballCenter = player.position().add(0, radius, 0);
                Quaternionf ballRot = data.katamariIO$getQuaternion();
                Vector3f currentOffset = new Vector3f(localPos).rotate(ballRot);
                Vec3 targetPos = ballCenter.add(currentOffset.x, currentOffset.y, currentOffset.z);
                moveFunction.accept(passenger, targetPos.x, targetPos.y, targetPos.z);
                passenger.setDeltaMovement(Vec3.ZERO);
                passenger.fallDistance = 0.0f;
                ci.cancel();
            }
        }
    }

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    protected void katamariIO$onSyncedDataUpdated(java.util.List<?> dataValues, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ICustomPlayerData data && data.katamariIO$getFlag()) {
            data.katamariIO$invalidateBinRadii();
        }
    }
}
