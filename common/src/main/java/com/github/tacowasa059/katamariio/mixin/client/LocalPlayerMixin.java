package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void katamariIO$cancelPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (((ICustomPlayerData) player).katamariIO$getFlag()) {
            ci.cancel();
        }
    }

    @Inject(method = "rideTick", at = @At("HEAD"))
    private void katamariIO$onRideTick(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!KatamariConfig.get().allowSelfDismount) {
            if (player.getVehicle() instanceof net.minecraft.world.entity.player.Player vehicle && ((ICustomPlayerData) vehicle).katamariIO$getFlag()) {
                if (player.input != null) {
                    player.input.shiftKeyDown = false;
                }
            }
        }
    }
}
