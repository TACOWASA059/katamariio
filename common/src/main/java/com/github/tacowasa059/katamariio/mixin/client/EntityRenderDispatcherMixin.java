package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow public Camera camera;
    @Shadow public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public <E extends Entity> void katamariIO$renderAttachedEntity(
            E entity, double x, double y, double z, float rotationYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        Player player = null;
        if (entity.getVehicle() instanceof Player p) {
            player = p;
        } else if (Minecraft.getInstance().level != null) {
            // If ClientboundSetPassengersPacket hasn't arrived or was delayed, resolve from player's synched data directly
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer != null && ((ICustomPlayerData) localPlayer).katamariIO$getPassengerLocalPos(entity.getId()) != null) {
                player = localPlayer;
            }
        }

        if (player != null) {
            ICustomPlayerData data = (ICustomPlayerData) player;
            if (data.katamariIO$getFlag()) {
                Vector3f localPos = KatamariEntityAttacher.getLocalPos(player, entity);
                if (localPos != null && this.camera != null) {
                    ci.cancel();

                    float radius = data.katamariIO$getSize() / 2.0f;
                    Vec3 cameraPos = this.camera.getPosition();
                    Vec3 playerPos = player.getPosition(partialTicks);
                    Vec3 ballCenter = playerPos.add(0, radius, 0);

                    Quaternionf ballRot = data.katamariIO$getInterpolatedQuaternion(partialTicks);
                    Vector3f currentOffset = new Vector3f(localPos).rotate(ballRot);
                    Vec3 mobWorldPos = ballCenter.add(currentOffset.x, currentOffset.y, currentOffset.z);
                    Vec3 camRel = mobWorldPos.subtract(cameraPos);

                    EntityRenderer<? super E> renderer = this.getRenderer(entity);
                    Vec3 renderOffset = renderer.getRenderOffset(entity, partialTicks);
                    double dx = camRel.x + renderOffset.x();
                    double dy = camRel.y + renderOffset.y();
                    double dz = camRel.z + renderOffset.z();

                    poseStack.pushPose();
                    poseStack.translate(dx, dy, dz);

                    // Apply rotation difference so the mob tumbles, rolls, and flips synchronously with the katamari!
                    Quaternionf attachQuat = KatamariEntityAttacher.getAttachQuat(player, entity);
                    if (attachQuat != null) {
                        Quaternionf delta = new Quaternionf(ballRot).mul(new Quaternionf(attachQuat).invert());
                        poseStack.mulPose(delta);
                    } else {
                        poseStack.mulPose(ballRot);
                    }

                    renderer.render(entity, rotationYaw, partialTicks, poseStack, buffer, packedLight);
                    poseStack.popPose();
                }
            }
        }
    }
}
