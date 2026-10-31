package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.event.PlayerRespawnEventListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    @Shadow @Final private Entity entity;

    @Inject(method = "addPairing", at = @At("TAIL"))
    private void katamariIO$onAddPairing(ServerPlayer viewer, CallbackInfo ci) {
        if (this.entity instanceof ServerPlayer trackedPlayer) {
            PlayerRespawnEventListener.onStartTracking(viewer, trackedPlayer);
        }
    }
}
