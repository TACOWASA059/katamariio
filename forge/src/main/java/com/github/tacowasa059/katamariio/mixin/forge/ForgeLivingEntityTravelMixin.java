package com.github.tacowasa059.katamariio.mixin.forge;

import com.github.tacowasa059.katamariio.common.KatamariTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LivingEntity.class)
public class ForgeLivingEntityTravelMixin {
    @Inject(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 2),
            locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void injectCustomMotion(Vec3 travelVector, CallbackInfo ci, double d0, AttributeInstance gravity, boolean flag,
                                    FluidState fluidstate, BlockPos blockpos, float f2, float f3, Vec3 vec35, double d2) {
        KatamariTravel.applyCustomMotion((LivingEntity) (Object) this, vec35, ci);
    }

    @Inject(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 3),
            locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void injectCustomMotion2(Vec3 travelVector, CallbackInfo ci, double d0, AttributeInstance gravity, boolean flag,
                                     FluidState fluidstate, BlockPos blockpos, float f2, float f3, Vec3 vec35, double d2) {
        KatamariTravel.applyCustomMotion((LivingEntity) (Object) this, vec35, ci);
    }
}
