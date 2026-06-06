package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void katamariIO$disableSneak(boolean isSneaking, float slowDownFactor, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) player;
        KeyboardInput keyboardInput = (KeyboardInput) (Object) this;
        if (player.onGround() && !player.isPassenger() && playerData.katamariIO$getFlag()) {
            keyboardInput.shiftKeyDown = false;
        }
    }
}
