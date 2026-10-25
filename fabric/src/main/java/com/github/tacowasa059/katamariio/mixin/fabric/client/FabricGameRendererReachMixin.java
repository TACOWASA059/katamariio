package com.github.tacowasa059.katamariio.mixin.fabric.client;

import com.github.tacowasa059.katamariio.common.KatamariReach;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Scales the client-side entity pick distance with the ball size on Fabric. In GameRenderer.pick the
 * "far pick range" entity reach is a hard-coded 6.0 blocks; combined with FabricReachMixin forcing
 * hasFarPickRange() true for a ball, this widens the entity reach to the ball's scaled reach so the
 * crosshair can target (and thus attack) entities near a large ball's surface.
 */
@Mixin(GameRenderer.class)
public class FabricGameRendererReachMixin {
    @ModifyConstant(method = "pick", constant = @Constant(doubleValue = 6.0))
    private double katamariIO$scaleEntityReach(double original) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return original;
        }
        ICustomPlayerData data = (ICustomPlayerData) player;
        if (!data.katamariIO$getFlag()) {
            return original;
        }
        return Math.max(original, KatamariReach.reachFor(data.katamariIO$getSize()));
    }
}
