package com.github.tacowasa059.katamariio.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class KatamariTravel {
    private KatamariTravel() {
    }

    public static void applyCustomMotion(LivingEntity entity, Vec3 movement, CallbackInfo ci) {
        if (!(entity instanceof Player)) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) entity;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        double y = movement.y;
        if (entity.hasEffect(MobEffects.LEVITATION)) {
            MobEffectInstance effect = entity.getEffect(MobEffects.LEVITATION);
            if (effect != null) {
                y += (0.05D * (effect.getAmplifier() + 1) - movement.y) * 0.2D;
            }
        } else if (!entity.isNoGravity()) {
            y -= 0.08D;
        }

        double x = movement.x * 0.95F;
        double z = movement.z * 0.95F;
        double sqr = x * x + z * z;
        if (sqr >= 0.8 * 0.8) {
            double scale = 0.8 / Math.sqrt(sqr);
            x *= scale;
            z *= scale;
        }

        Vec3 newVector = new Vec3(x, y * 0.9800000190734863, z);
        entity.setDeltaMovement(newVector);
        ci.cancel();
    }
}
