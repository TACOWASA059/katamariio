package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.client.utils.KatamariIndicatorHelper;
import com.github.tacowasa059.katamariio.client.utils.SphereRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.accessors.IPlayerRendererAccessor;
import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class KatamariPlayerRenderer {
    public static double MAX_SIZE = 0;
    // Above this radius the katamari is a dense shell, so far-side blocks can be culled. Below it the
    // ball is sparse and its far side shows through the gaps, so every block is drawn.
    private static final double KATAMARI_DENSE_RADIUS = 6.0;
    // Floor on block light so the katamari stays visible when buried in blocks (sampled light is 0).
    private static final int KATAMARI_MIN_BLOCK_LIGHT = 8;

    private KatamariPlayerRenderer() {
    }

    public static boolean render(PlayerRenderer renderer, AbstractClientPlayer player, float partialTicks, PoseStack poseStack,
                                 MultiBufferSource buffer, int packedLight) {
        ICustomPlayerData playerData = (ICustomPlayerData) player;
        if (!playerData.katamariIO$getFlag() || player.isSpectator()) {
            return false;
        }
        if (player.isInvisible()) {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        if (player.equals(mc.player) && mc.options.getCameraType().isFirstPerson()) {
            return true;
        }

        float renderSize = playerData.katamariIO$getRenderSize();
        float collisionSize = playerData.katamariIO$getSize();
        float outerSize = playerData.katamariIO$getOuterSize();
        float outerRadius = playerData.katamariIO$getOuterRadius();

        // When renderAttachedBlocks is disabled, render the textured skin ball scaled up
        // to match the katamari's actual outer growth rather than staying at initial renderSize.
        float sphereRadius = !KatamariConfig.get().renderAttachedBlocks
                ? Math.max(renderSize / 2.0f, Math.max(collisionSize / 2.0f, outerRadius))
                : (renderSize / 2.0f);

        packedLight = getKatamariPackedLight(player, collisionSize / 2.0f, partialTicks, packedLight);
        Quaternionf ballRotation;
        Entity vehicle = player.getVehicle();
        if (vehicle != null && !KatamariEntityAttacher.isKatamariVehicle(vehicle)) {
            // Riding a standard vehicle (boat, minecart, horse): orient with vehicle
            ballRotation = QuaternionUtils.getInterpolatedQuaternionFromEntity(player, partialTicks);
        } else {
            // Self-rolling, or passenger stuck to a katamari: use the ball's own rolling/synced quaternion
            ballRotation = playerData.katamariIO$getInterpolatedQuaternion(partialTicks);
        }
        ResourceLocation texture = player.getSkin().texture();

        poseStack.pushPose();
        poseStack.translate(0, collisionSize / 2.0, 0);
        poseStack.mulPose(ballRotation);

        int overlay = OverlayTexture.NO_OVERLAY;
        if (player.hurtTime > 0 || player.deathTime > 0) {
            overlay = OverlayTexture.pack(OverlayTexture.u(partialTicks), OverlayTexture.v(true));
        }

        poseStack.pushPose();
        SphereRenderer.drawTexturedSphere(poseStack, buffer, texture, sphereRadius, 24, 0, 0, packedLight, true, overlay);
        poseStack.popPose();

        // The stored blocks are already pruned to the visible surface, so there is no separate
        // render-side decimation: every stored block is drawn (one cached VBO draw, or per-block on
        // fallback).
        KatamariBlocks blocks = playerData.katamariIO$getBlocks();
        int count = blocks.size();

        double maxLenSq = 0.0;
        for (int i = 0; i < count; i++) {
            double lenSq = blocks.lengthSqr(i);
            if (lenSq > maxLenSq) {
                maxLenSq = lenSq;
            }
        }
        double ballRadius = Math.sqrt(maxLenSq);
        if (player.equals(Minecraft.getInstance().player)) {
            MAX_SIZE = Math.max(renderSize, outerSize);
        }

        if (KatamariConfig.get().renderAttachedBlocks && count > 0) {
            if (!KatamariBlockBuffer.tryRender(player, blocks, poseStack, packedLight)) {
                renderBlocksDirect(player, blocks, count, ballRadius, partialTicks, poseStack, buffer, packedLight, overlay, ballRotation);
            }
        }

        poseStack.popPose();

        float effectiveDiameter = Math.max(collisionSize, outerSize);
        if (player instanceof RemotePlayer) {
            renderPlayerIndicator((RemotePlayer) player, poseStack, buffer, packedLight, effectiveDiameter);
        }

        return true;
    }

    // Fallback used only when the cached buffer cannot be built/drawn.
    private static void renderBlocksDirect(Player player, KatamariBlocks blocks, int count, double ballRadius, float partialTicks,
                                           PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay, Quaternionf quaternion) {
        boolean dense = ballRadius >= KATAMARI_DENSE_RADIUS;

        Vector3f viewDirLocal = null;
        if (dense) {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 camPos = camera.getPosition();
            Vec3 playerPos = player.getPosition(partialTicks);
            Vec3 ballCenter = playerPos.add(0.0, ballRadius, 0.0);
            Vec3 toBall = ballCenter.subtract(camPos);
            double distSq = toBall.lengthSqr();
            if (distSq > 1.0E-6) {
                Vec3 viewDir = toBall.normalize();
                viewDirLocal = new Vector3f((float) viewDir.x, (float) viewDir.y, (float) viewDir.z);
                viewDirLocal.rotate(new Quaternionf(quaternion).invert());
            }
        }

        Vector3f localNormal = new Vector3f();
        Quaternionf blockRotation = new Quaternionf();
        for (int i = 0; i < count; i++) {
            float px = blocks.posX(i);
            float py = blocks.posY(i);
            float pz = blocks.posZ(i);
            if (dense && viewDirLocal != null) {
                localNormal.set(px, py, pz);
                float normalLenSq = localNormal.lengthSquared();
                if (normalLenSq > 1.0E-6f) {
                    localNormal.normalize();
                    // viewDirLocal points from camera to ball center.
                    // A surface normal facing towards camera opposes viewDirLocal (dot < 0).
                    // If dot >= 0, it faces away from camera (back-facing), so cull it.
                    if (localNormal.dot(viewDirLocal) >= 0.0f) {
                        continue;
                    }
                }
            }
            Block block = blocks.block(i);
            blocks.quatInto(i, blockRotation);
            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.mulPose(blockRotation);
            Vector3f centerOffset = com.github.tacowasa059.katamariio.common.utils.KatamariBlockUtils.getBlockCenterOffset(block, player.level());
            poseStack.translate(-centerOffset.x, -centerOffset.y, -centerOffset.z);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), poseStack, buffer, packedLight, overlay);
            poseStack.popPose();
        }
    }

    private static int getKatamariPackedLight(Player player, float ballRadius, float partialTicks, int vanillaLight) {
        Level level = player.level();
        Vec3 pos = player.getPosition(partialTicks);
        // Sample light at the top of the ball where it is exposed to the open air/sky,
        // rather than at the player's feet which may be buried in the ground.
        double sampleY = pos.y + Math.max(1.0, ballRadius * 1.5);
        BlockPos samplePos = BlockPos.containing(pos.x, sampleY, pos.z);

        int blockLight = level.getBrightness(LightLayer.BLOCK, samplePos);
        int skyLight = level.getBrightness(LightLayer.SKY, samplePos);

        // If the top position is inside a block, also check the eye position
        if (blockLight == 0 && skyLight == 0) {
            BlockPos eyePos = BlockPos.containing(pos.x, pos.y + player.getEyeHeight(), pos.z);
            blockLight = Math.max(blockLight, level.getBrightness(LightLayer.BLOCK, eyePos));
            skyLight = Math.max(skyLight, level.getBrightness(LightLayer.SKY, eyePos));
        }

        // Also blend with vanilla light if vanilla sampled a brighter area
        blockLight = Math.max(blockLight, LightTexture.block(vanillaLight));
        skyLight = Math.max(skyLight, LightTexture.sky(vanillaLight));

        // Subtle ambient floor so the ball is never completely pitch-black when totally enclosed
        blockLight = Math.max(blockLight, 3);

        return LightTexture.pack(blockLight, skyLight);
    }

    private static void renderPlayerIndicator(RemotePlayer player, PoseStack poseStack, MultiBufferSource buffer,
                                             int packedLight, float ballDiameter) {
        if (!KatamariConfig.get().showPlayerIndicator) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        // Calculate floating height dynamically based on ball size to prevent clipping inside big balls
        float margin = KatamariIndicatorHelper.computeIndicatorMargin(ballDiameter);
        float height = ballDiameter + margin;

        Vec3 indicatorWorldPos = player.position().add(0.0, height, 0.0);
        double distSq = camera.getPosition().distanceToSqr(indicatorWorldPos);
        double camDist = Math.sqrt(distSq);

        // Get local player size
        float mySize = 2.0f;
        LocalPlayer localPlayer = mc.player;
        if (localPlayer != null && ((ICustomPlayerData) localPlayer).katamariIO$getFlag()) {
            mySize = Math.max(2.0f, (float) Math.max(KatamariPlayerRenderer.MAX_SIZE, ((ICustomPlayerData) localPlayer).katamariIO$getSize()));
        }

        // Dynamically expand max draw distance when players are large
        double maxDist = KatamariIndicatorHelper.computeMaxDrawDistance(mySize, ballDiameter);
        if (camDist > maxDist) {
            return;
        }

        poseStack.pushPose();

        // If attached to a katamari ball, undo the tumble/roll rotation applied by EntityRenderDispatcherMixin
        // so the indicator always floats upright in world space and directly faces the camera
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Player parentPlayer && ((ICustomPlayerData) parentPlayer).katamariIO$getFlag()) {
            ICustomPlayerData parentData = (ICustomPlayerData) parentPlayer;
            float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(true);
            Quaternionf parentBallRot = parentData.katamariIO$getInterpolatedQuaternion(partialTicks);
            Quaternionf attachQuat = KatamariEntityAttacher.getAttachQuat(parentPlayer, player);
            Quaternionf delta;
            if (attachQuat != null) {
                delta = new Quaternionf(parentBallRot).mul(new Quaternionf(attachQuat).invert());
            } else {
                delta = new Quaternionf(parentBallRot);
            }
            poseStack.mulPose(new Quaternionf(delta).invert());
        }

        poseStack.translate(0.0, height, 0.0);
        // Face the camera (billboard)
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        // Calculate dynamic scale factor based on ball size, local player size, and camera distance
        float totalScale = KatamariIndicatorHelper.computeScaleFactor(mySize, ballDiameter, camDist);
        float scale = KatamariIndicatorHelper.BASE_SCALE * totalScale;
        poseStack.scale(-scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        Font font = mc.font;

        // Use fullbright lighting so the UI indicator remains visible in darkness
        int indicatorLight = LightTexture.pack(15, 15);

        // 1. Draw player face GUI icon (16x16)
        // Two-pass rendering: see-through pass (visible behind obstacles) and normal pass (sharp in direct view)
        float iconSize = 16.0f;
        float x0 = -iconSize / 2.0f;
        float x1 = iconSize / 2.0f;
        float y0 = -28.0f;
        float y1 = y0 + iconSize;

        // See-through pass (no depth test)
        VertexConsumer seeThroughFace = buffer.getBuffer(RenderType.textSeeThrough(player.getSkin().texture()));
        drawFaceQuad(seeThroughFace, matrix, x0, y0, x1, y1, 8.0f / 64.0f, 8.0f / 64.0f, 16.0f / 64.0f, 16.0f / 64.0f, indicatorLight, 255);
        drawFaceQuad(seeThroughFace, matrix, x0, y0, x1, y1, 40.0f / 64.0f, 8.0f / 64.0f, 48.0f / 64.0f, 16.0f / 64.0f, indicatorLight, 255);

        // Normal pass (opaque in direct line of sight)
        VertexConsumer normalFace = buffer.getBuffer(RenderType.text(player.getSkin().texture()));
        drawFaceQuad(normalFace, matrix, x0, y0, x1, y1, 8.0f / 64.0f, 8.0f / 64.0f, 16.0f / 64.0f, 16.0f / 64.0f, indicatorLight, 255);
        drawFaceQuad(normalFace, matrix, x0, y0, x1, y1, 40.0f / 64.0f, 8.0f / 64.0f, 48.0f / 64.0f, 16.0f / 64.0f, indicatorLight, 255);

        // 2. Downward indicator arrow "▼"
        String arrow = "▼";
        float arrowWidth = font.width(arrow);
        // See-through pass
        font.drawInBatch(arrow, -arrowWidth / 2.0f, y1 + 1.0f, 0xFFFFD700, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0, indicatorLight);
        // Normal pass
        font.drawInBatch(arrow, -arrowWidth / 2.0f, y1 + 1.0f, 0xFFFFD700, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, indicatorLight);

        // 3. Player display name
        net.minecraft.network.chat.Component name = player.getDisplayName();
        float nameWidth = font.width(name);
        // See-through pass (with semi-transparent background)
        font.drawInBatch(name, -nameWidth / 2.0f, y1 + 10.0f, 0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0x20000000, indicatorLight);
        // Normal pass
        font.drawInBatch(name, -nameWidth / 2.0f, y1 + 10.0f, 0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, indicatorLight);

        poseStack.popPose();
    }

    private static void drawFaceQuad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float x1, float y1,
                                     float u0, float v0, float u1, float v1, int packedLight, int alpha) {
        consumer.addVertex(matrix, x0, y1, 0.0f).setColor(255, 255, 255, alpha).setUv(u0, v1).setLight(packedLight);
        consumer.addVertex(matrix, x1, y1, 0.0f).setColor(255, 255, 255, alpha).setUv(u1, v1).setLight(packedLight);
        consumer.addVertex(matrix, x1, y0, 0.0f).setColor(255, 255, 255, alpha).setUv(u1, v0).setLight(packedLight);
        consumer.addVertex(matrix, x0, y0, 0.0f).setColor(255, 255, 255, alpha).setUv(u0, v0).setLight(packedLight);
    }
}
