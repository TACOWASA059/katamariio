package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onApplyBobbing(PoseStack matrixStack, float partialTicks, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (((ICustomPlayerData) player).katamariIO$getFlag()) {
            ci.cancel();
        }
    }
}
