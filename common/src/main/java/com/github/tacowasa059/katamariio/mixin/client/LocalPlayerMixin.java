package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (!KatamariConfig.get().allowSelfDismount
                && player.getVehicle() instanceof Player vehiclePlayer
                && ((ICustomPlayerData) vehiclePlayer).katamariIO$getFlag()) {
            if (player.input != null && player.input.keyPresses.shift()) {
                Input kp = player.input.keyPresses;
                player.input.keyPresses = new Input(
                        kp.forward(), kp.backward(), kp.left(), kp.right(), kp.jump(), false, kp.sprint()
                );
            }
            player.setShiftKeyDown(false);
        }
    }

    @Inject(method = "isHandsBusy", at = @At("HEAD"), cancellable = true)
    private void katamariIO$allowAttackWhileCaptured(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.getVehicle() instanceof Player vehiclePlayer
                && ((ICustomPlayerData) vehiclePlayer).katamariIO$getFlag()) {
            cir.setReturnValue(false);
        }
    }
}
