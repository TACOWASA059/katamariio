package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.KatamariReach;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Redirect(method = "handleBlockBreakAction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"),
            require = 0)
    private double katamariIO$scaleBlockBreakReach(Vec3 eyePos, Vec3 blockCenter) {
        double real = eyePos.distanceToSqr(blockCenter);
        if (this.player != null) {
            ICustomPlayerData data = (ICustomPlayerData) this.player;
            if (data.katamariIO$getFlag()) {
                double reach = KatamariReach.reachFor(data.katamariIO$getSize());
                if (real <= reach * reach) {
                    return 0.0;
                }
            }
        }
        return real;
    }
}
