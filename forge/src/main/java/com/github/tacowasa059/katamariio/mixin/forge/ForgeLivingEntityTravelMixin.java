package com.github.tacowasa059.katamariio.mixin.forge;

import com.github.tacowasa059.katamariio.common.KatamariTravel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ForgeLivingEntityTravelMixin {
    @Inject(method = "travelInAir", at = @At("HEAD"), cancellable = true)
    private void katamariio$travelInAir(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        double acceleration = entity.onGround() ? entity.getSpeed() : 0.02D;
        entity.moveRelative((float) acceleration, travelVector);
        Vec3 movement = entity.getDeltaMovement();
        if (KatamariTravel.applyCustomMotion(entity, movement, false, 0.08D, 0.98F)) {
            ci.cancel();
        }
    }
}
