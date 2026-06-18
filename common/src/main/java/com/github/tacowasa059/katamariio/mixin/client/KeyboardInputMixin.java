package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void katamariIO$disableSneak(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) player;
        KeyboardInput keyboardInput = (KeyboardInput) (Object) this;
        if (player.onGround() && !player.isPassenger() && playerData.katamariIO$getFlag()) {
            Input keyPresses = keyboardInput.keyPresses;
            keyboardInput.keyPresses = new Input(
                    keyPresses.forward(),
                    keyPresses.backward(),
                    keyPresses.left(),
                    keyPresses.right(),
                    keyPresses.jump(),
                    false,
                    keyPresses.sprint()
            );
        }
    }
}
