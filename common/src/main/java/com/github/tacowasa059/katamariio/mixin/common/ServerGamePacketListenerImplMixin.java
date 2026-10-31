package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;
    @Shadow private int aboveGroundTickCount;
    @Shadow private boolean clientIsFloating;
    @Shadow private int aboveGroundVehicleTickCount;
    @Shadow private boolean clientVehicleIsFloating;

    @Inject(method = "tick", at = @At("TAIL"))
    private void katamariIO$onTick(CallbackInfo ci) {
        if (this.player != null && KatamariConfig.get().disableFlyCheck) {
            ICustomPlayerData data = (ICustomPlayerData) this.player;
            if (data.katamariIO$getFlag()) {
                this.aboveGroundTickCount = 0;
                this.clientIsFloating = false;
                this.aboveGroundVehicleTickCount = 0;
                this.clientVehicleIsFloating = false;
            }
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void katamariIO$onHandleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (this.player != null && KatamariConfig.get().disableFlyCheck) {
            ICustomPlayerData data = (ICustomPlayerData) this.player;
            if (data.katamariIO$getFlag()) {
                this.aboveGroundTickCount = 0;
                this.clientIsFloating = false;
            }
        }
    }

    @Inject(method = "handleMoveVehicle", at = @At("HEAD"))
    private void katamariIO$onHandleMoveVehicle(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        if (this.player != null && KatamariConfig.get().disableFlyCheck) {
            ICustomPlayerData data = (ICustomPlayerData) this.player;
            if (data.katamariIO$getFlag()) {
                this.aboveGroundVehicleTickCount = 0;
                this.clientVehicleIsFloating = false;
            }
        }
    }

    @Inject(method = "handlePlayerInput", at = @At("TAIL"))
    private void katamariIO$onHandlePlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        if (packet.input().shift() && this.player != null && this.player.isPassenger()) {
            Entity vehicle = this.player.getVehicle();
            if (vehicle instanceof Player vehiclePlayer && ((ICustomPlayerData) vehiclePlayer).katamariIO$getFlag()) {
                if (KatamariConfig.get().allowSelfDismount) {
                    KatamariEntityAttacher.detachPassenger(vehiclePlayer, this.player);
                } else {
                    this.player.setShiftKeyDown(false);
                }
            }
        }
    }
}
