package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.client.KatamariPlayerRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public class CameraMixin {
    @Redirect(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"
            )
    )
    private float katamariIO$redirectCameraDistance(Camera instance, float originalDistance) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 4.0f;
        }

        if (((ICustomPlayerData) player).katamariIO$getFlag()) {
            return (float) (KatamariPlayerRenderer.MAX_SIZE * 2.0);
        }
        return 4.0f;
    }
}
