package com.github.tacowasa059.katamariio.mixin.neoforge;

import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge 21.11 added {@code CommonHooks.verifyEntityDataAccessorRegistration}, which throws (only when
 * running in the IDE/dev environment) if a mod adds {@link net.minecraft.network.syncher.EntityDataAccessor}
 * synced data to a vanilla entity via a mixin. KatamariIO intentionally adds its katamari state to
 * {@code Player} through {@code PlayerMixin}, so this guard aborts mod loading in the NeoForge dev runs.
 *
 * <p>In a packaged install the check only logs a warning and the mod loads fine; this mixin simply skips the
 * dev-only throw so the NeoForge development client/server can launch as well. The actual {@code defineId}
 * registration that follows the check is left untouched.
 */
@Mixin(CommonHooks.class)
public class CommonHooksMixin {
    @Inject(method = "verifyEntityDataAccessorRegistration", at = @At("HEAD"), cancellable = true)
    private static void katamariio$skipForeignEntityCheck(Class<?> callerClass, Class<?> holderClass, CallbackInfo ci) {
        ci.cancel();
    }
}
