package com.github.tacowasa059.katamariio.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class KatamariTravel {
    private KatamariTravel() {
    }

    public static boolean applyCustomMotion(LivingEntity entity, Vec3 movement, boolean discardFriction, double gravity, float verticalDecay) {
        if (!(entity instanceof Player)) {
            return false;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) entity;
        if (!playerData.katamariIO$getFlag()) {
            return false;
        }

        // movement is the velocity right after moveRelative (last tick's residual + input).
        // Vanilla's handleRelativeFrictionAndCalculateMovement does move() *before* applying
        // friction/gravity, so we mirror that order here: move first, then derive the residual
        // velocity from the post-move delta. The katamari uses a fixed 0.95 horizontal friction
        // and a 0.08 gravity / 0.98 vertical decay rather than vanilla block friction.
        entity.move(MoverType.SELF, movement);
        Vec3 moved = entity.getDeltaMovement();

        double y = moved.y;
        if (entity.hasEffect(MobEffects.LEVITATION)) {
            MobEffectInstance effect = entity.getEffect(MobEffects.LEVITATION);
            if (effect != null) {
                y += (0.05D * (effect.getAmplifier() + 1) - moved.y) * 0.2D;
            }
        } else if (!entity.isNoGravity()) {
            y -= gravity;
        }

        double x = moved.x * 0.95F;
        double z = moved.z * 0.95F;
        double sqr = x * x + z * z;
        if (sqr >= 0.8 * 0.8) {
            double scale = 0.8 / Math.sqrt(sqr);
            x *= scale;
            z *= scale;
        }

        if (discardFriction) {
            entity.setDeltaMovement(x, y, z);
        } else {
            entity.setDeltaMovement(x, y * verticalDecay, z);
        }
        return true;
    }
}
