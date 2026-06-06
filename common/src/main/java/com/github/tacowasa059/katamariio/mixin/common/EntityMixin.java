package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private Vec3 katamariIO$previousPosition;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player) {
            katamariIO$updateQuaternion(player);
        }
    }

    @Unique
    private void katamariIO$updateQuaternion(Player player) {
        Level world = player.level();
        if (!world.isClientSide) {
            if (player.getVehicle() != null) {
                ((ICustomPlayerData) player).katamariIO$setQuaternion(QuaternionUtils.getQuaternionFromEntity(player));
                return;
            }

            Vec3 currentPosition = player.position();
            ICustomPlayerData playerData = (ICustomPlayerData) player;
            Quaternionf rotation = QuaternionUtils.getUpdatedQuaternion(currentPosition, katamariIO$previousPosition, playerData);
            katamariIO$previousPosition = currentPosition;
            playerData.katamariIO$setCurrentPosition(player.position());
            if (rotation != null) {
                playerData.katamariIO$setQuaternion(rotation);
            }
        }
    }

    @Inject(method = "checkInsideBlocks", at = @At("HEAD"), cancellable = true)
    public void checkInsideBlocks(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            ci.cancel();
        }
    }
}
