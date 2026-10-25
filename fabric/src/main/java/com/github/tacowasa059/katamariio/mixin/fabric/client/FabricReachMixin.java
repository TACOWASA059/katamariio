package com.github.tacowasa059.katamariio.mixin.fabric.client;

import com.github.tacowasa059.katamariio.common.KatamariReach;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric interaction-reach scaling. Fabric has no reach attributes (unlike Forge), so we widen the
 * client pick range directly. Block reach scales with the ball size; entity reach is lifted from the
 * survival 3-block cap to the "far pick range" 6-block cap. The reach follows the current size, so it
 * returns to vanilla the moment the size is reset.
 */
@Mixin(MultiPlayerGameMode.class)
public class FabricReachMixin {
    @Inject(method = "getPickRange", at = @At("RETURN"), cancellable = true)
    private void katamariIO$scaleBlockReach(CallbackInfoReturnable<Float> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ICustomPlayerData data = (ICustomPlayerData) player;
        if (!data.katamariIO$getFlag()) {
            return;
        }
        float scaled = (float) KatamariReach.reachFor(data.katamariIO$getSize());
        if (scaled > cir.getReturnValueF()) {
            cir.setReturnValue(scaled);
        }
    }

    @Inject(method = "hasFarPickRange", at = @At("RETURN"), cancellable = true)
    private void katamariIO$extendEntityReach(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            cir.setReturnValue(true);
        }
    }
}
