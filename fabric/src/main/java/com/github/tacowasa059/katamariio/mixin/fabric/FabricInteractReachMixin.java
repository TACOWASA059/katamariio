package com.github.tacowasa059.katamariio.mixin.fabric;

import com.github.tacowasa059.katamariio.common.KatamariReach;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Relaxes the server-side entity interaction distance for a ball player on Fabric. Vanilla gates
 * attacks on {@code aabb.distanceToSqr(eye) < MAX_INTERACTION_DISTANCE} (6 blocks); we report 0 when
 * the target is within the ball's scaled reach so the server accepts attacks the (also-widened)
 * client sends. Non-ball players and out-of-reach targets keep the real distance, so vanilla behaviour
 * is unchanged for them.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class FabricInteractReachMixin {
    @Shadow
    public ServerPlayer player;

    @Redirect(method = "handleInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private double katamariIO$reachDistance(AABB aabb, Vec3 eye) {
        double real = aabb.distanceToSqr(eye);
        ICustomPlayerData data = (ICustomPlayerData) this.player;
        if (data.katamariIO$getFlag()) {
            double reach = KatamariReach.reachFor(data.katamariIO$getSize());
            if (real <= reach * reach) {
                return 0.0;
            }
        }
        return real;
    }
}
