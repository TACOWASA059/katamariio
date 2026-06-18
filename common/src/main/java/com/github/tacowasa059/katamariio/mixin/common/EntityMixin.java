package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
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
        }
    }

    @Inject(method = "checkInsideBlocks(Ljava/util/List;Lnet/minecraft/world/entity/InsideBlockEffectApplier$StepBasedCollector;)V", at = @At("HEAD"), cancellable = true)
    public void checkInsideBlocks(List<Object> movements, InsideBlockEffectApplier.StepBasedCollector stepBasedCollector, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
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

    @Unique
    private void katamariIO$updateQuaternion(Player player) {
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        if (player.getVehicle() != null) {
            ((ICustomPlayerData) player).katamariIO$setQuaternion(QuaternionUtils.getQuaternionFromEntity(player));
            return;
        }

        Vec3 currentPosition = player.position();
        ICustomPlayerData playerData = (ICustomPlayerData) player;
        Quaternionf rotation = QuaternionUtils.getUpdatedQuaternion(currentPosition, katamariIO$previousPosition, playerData);
        katamariIO$previousPosition = currentPosition;
        playerData.katamariIO$setCurrentPosition(currentPosition);
        if (rotation != null) {
            playerData.katamariIO$setQuaternion(rotation);
        }
    }
}
