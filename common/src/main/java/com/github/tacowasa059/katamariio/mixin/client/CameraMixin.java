package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.client.KatamariPlayerRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void move(float x, float y, float z);
    @Shadow protected abstract float getMaxZoom(float startingDistance);
    @Shadow private float xRot;
    @Shadow private float yRot;

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
            return this.getMaxZoom(originalDistance);
        }

        // Camera distance scales with 4.0 * maxRadius (equivalent to 2.0 * diameter),
        // preserving a comfortable, scale-invariant third-person view of the growing ball.
        if (((ICustomPlayerData) player).katamariIO$getFlag()) {
            float maxRadius = ((ICustomPlayerData) player).katamariIO$getOuterRadius();
            return Math.max(4.0f, maxRadius * 4.0f);
        }
        if (player.getVehicle() instanceof Player vehicle && ((ICustomPlayerData) vehicle).katamariIO$getFlag()) {
            float parentMaxRadius = ((ICustomPlayerData) vehicle).katamariIO$getOuterRadius();
            return Math.max(4.0f, parentMaxRadius * 4.0f);
        }
        return this.getMaxZoom(originalDistance);
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void katamariIO$setupPassengerCamera(
            BlockGetter blockGetter, Entity entity, boolean thirdPerson, boolean detached, float partialTicks, CallbackInfo ci) {
        if (entity != null && entity.getVehicle() instanceof Player vehicle) {
            ICustomPlayerData parentData = (ICustomPlayerData) vehicle;
            if (parentData.katamariIO$getFlag()) {
                Vector3f localPos = KatamariEntityAttacher.getLocalPos(vehicle, entity);
                if (localPos != null) {
                    float radius = parentData.katamariIO$getSize() / 2.0f;
                    Vec3 parentPos = vehicle.getPosition(partialTicks);
                    Vec3 ballCenter = parentPos.add(0, radius, 0);

                    Quaternionf ballRot = parentData.katamariIO$getInterpolatedQuaternion(partialTicks);
                    Vector3f currentOffset = new Vector3f(localPos).rotate(ballRot);
                    Vec3 riderPos = ballCenter.add(currentOffset.x, currentOffset.y, currentOffset.z);

                    // Passenger eye position (interpolated)
                    Vec3 eyePos = riderPos.add(0, entity.getEyeHeight(), 0);
                    this.setPosition(eyePos.x, eyePos.y, eyePos.z);

                    if (thirdPerson) {
                        this.setRotation(entity.getViewYRot(partialTicks), entity.getViewXRot(partialTicks));
                        if (detached) {
                            this.setRotation(this.yRot + 180.0F, -this.xRot);
                        }
                        float parentMaxRadius = parentData.katamariIO$getOuterRadius();
                        float zoomDist = Math.max(4.0f, parentMaxRadius * 4.0f);
                        this.move(-this.getMaxZoom(zoomDist), 0.0f, 0.0f);
                    }
                }
            }
        }
    }
}
