package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.client.utils.KatamariIndicatorHelper;
import com.github.tacowasa059.katamariio.client.utils.SphereRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher;
import com.github.tacowasa059.katamariio.common.utils.KatamariBlockUtils;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class KatamariPlayerRenderer {
    public static double MAX_SIZE = 0;
    private static final double KATAMARI_DENSE_RADIUS = 6.0;

    private KatamariPlayerRenderer() {
    }

    public static boolean render(AbstractClientPlayer player, float partialTicks, PoseStack poseStack,
                                 SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState,
                                 int packedLight, int outlineColor) {
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

        float sphereRadius = !KatamariConfig.get().renderAttachedBlocks
                ? Math.max(renderSize / 2.0f, Math.max(collisionSize / 2.0f, outerRadius))
                : (renderSize / 2.0f);

        final int finalPackedLight = getKatamariPackedLight(player, collisionSize / 2.0f, partialTicks, packedLight);
        Quaternionf ballRotation;
        Entity vehicle = player.getVehicle();
        if (vehicle != null && !KatamariEntityAttacher.isKatamariVehicle(vehicle)) {
            ballRotation = QuaternionUtils.getInterpolatedQuaternionFromEntity(player, partialTicks);
        } else {
            ballRotation = playerData.katamariIO$getInterpolatedQuaternion(partialTicks);
        }
        Identifier texture = player.getSkin().body().texturePath();

        poseStack.pushPose();
        poseStack.translate(0, collisionSize / 2.0, 0);
        poseStack.mulPose(ballRotation);

        int overlay = OverlayTexture.NO_OVERLAY;
        if (player.hurtTime > 0 || player.deathTime > 0) {
            overlay = OverlayTexture.pack(OverlayTexture.u(partialTicks), OverlayTexture.v(true));
        }
        final int packedOverlay = overlay;

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.PI / 4, 0, 1, 0)));
        nodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutoutNoCull(texture),
                (pose, consumer) -> SphereRenderer.drawTexturedSphere(pose, consumer, sphereRadius, 24, 0, 0, finalPackedLight, true, packedOverlay)
        );
        poseStack.popPose();

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
        if (player.equals(mc.player)) {
            MAX_SIZE = Math.max(renderSize, outerSize);
        }

        if (KatamariConfig.get().renderAttachedBlocks && count > 0) {
            renderBlocksDirect(player, blocks, count, ballRadius, partialTicks, poseStack, nodeCollector, finalPackedLight, packedOverlay, outlineColor, ballRotation);
        }

        poseStack.popPose();

        float effectiveDiameter = Math.max(collisionSize, outerSize);
        if (player instanceof RemotePlayer remotePlayer) {
            renderPlayerIndicator(remotePlayer, partialTicks, poseStack, nodeCollector, cameraRenderState, effectiveDiameter);
        }

        return true;
    }

    private static void renderBlocksDirect(Player player, KatamariBlocks blocks, int count, double ballRadius, float partialTicks,
                                           PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight,
                                           int packedOverlay, int outlineColor, Quaternionf quaternion) {
        boolean dense = ballRadius >= KATAMARI_DENSE_RADIUS;

        Vector3f viewDirLocal = null;
        if (dense) {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 camPos = camera.position();
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
            Vector3f centerOffset = KatamariBlockUtils.getBlockCenterOffset(block, player.level());
            poseStack.translate(-centerOffset.x, -centerOffset.y, -centerOffset.z);
            nodeCollector.submitBlock(poseStack, block.defaultBlockState(), packedLight, packedOverlay, outlineColor);
            poseStack.popPose();
        }
    }

    private static int getKatamariPackedLight(Player player, float ballRadius, float partialTicks, int vanillaLight) {
        Level level = player.level();
        Vec3 pos = player.getPosition(partialTicks);
        double sampleY = pos.y + Math.max(1.0, ballRadius * 1.5);
        BlockPos samplePos = BlockPos.containing(pos.x, sampleY, pos.z);

        int blockLight = level.getBrightness(LightLayer.BLOCK, samplePos);
        int skyLight = level.getBrightness(LightLayer.SKY, samplePos);

        if (blockLight == 0 && skyLight == 0) {
            BlockPos eyePos = BlockPos.containing(pos.x, pos.y + player.getEyeHeight(), pos.z);
            blockLight = Math.max(blockLight, level.getBrightness(LightLayer.BLOCK, eyePos));
            skyLight = Math.max(skyLight, level.getBrightness(LightLayer.SKY, eyePos));
        }

        blockLight = Math.max(blockLight, LightTexture.block(vanillaLight));
        skyLight = Math.max(skyLight, LightTexture.sky(vanillaLight));
        blockLight = Math.max(blockLight, 3);

        return LightTexture.pack(blockLight, skyLight);
    }

    private static void renderPlayerIndicator(RemotePlayer player, float partialTicks, PoseStack poseStack,
                                              SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState,
                                              float ballDiameter) {
        if (!KatamariConfig.get().showPlayerIndicator) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        float margin = KatamariIndicatorHelper.computeIndicatorMargin(ballDiameter);
        float height = ballDiameter + margin;

        Vec3 indicatorWorldPos = player.position().add(0.0, height, 0.0);
        double distSq = camera.position().distanceToSqr(indicatorWorldPos);
        double camDist = Math.sqrt(distSq);

        float mySize = 2.0f;
        LocalPlayer localPlayer = mc.player;
        if (localPlayer != null && ((ICustomPlayerData) localPlayer).katamariIO$getFlag()) {
            mySize = Math.max(2.0f, (float) Math.max(KatamariPlayerRenderer.MAX_SIZE, ((ICustomPlayerData) localPlayer).katamariIO$getSize()));
        }

        double maxDist = KatamariIndicatorHelper.computeMaxDrawDistance(mySize, ballDiameter);
        if (camDist > maxDist) {
            return;
        }

        poseStack.pushPose();

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Player parentPlayer && ((ICustomPlayerData) parentPlayer).katamariIO$getFlag()) {
            ICustomPlayerData parentData = (ICustomPlayerData) parentPlayer;
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
        if (cameraRenderState != null && cameraRenderState.orientation != null) {
            poseStack.mulPose(cameraRenderState.orientation);
        }

        float totalScale = KatamariIndicatorHelper.computeScaleFactor(mySize, ballDiameter, camDist);
        float scale = KatamariIndicatorHelper.BASE_SCALE * totalScale;
        poseStack.scale(scale, -scale, scale);

        Font font = mc.font;
        int indicatorLight = LightTexture.pack(15, 15);
        Identifier skinTexture = player.getSkin().body().texturePath();

        float iconSize = 16.0f;
        float x0 = -iconSize / 2.0f;
        float x1 = iconSize / 2.0f;
        float y0 = -28.0f;
        float y1 = y0 + iconSize;

        nodeCollector.submitCustomGeometry(poseStack, RenderTypes.textSeeThrough(skinTexture), (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            drawFaceQuad(consumer, matrix, x0, y0, x1, y1, 8.0f / 64.0f, 8.0f / 64.0f, 16.0f / 64.0f, 16.0f / 64.0f, indicatorLight, 255);
            drawFaceQuad(consumer, matrix, x0, y0, x1, y1, 40.0f / 64.0f, 8.0f / 64.0f, 48.0f / 64.0f, 16.0f / 64.0f, indicatorLight, 255);
        });
        nodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(skinTexture), (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            drawFaceQuad(consumer, matrix, x0, y0, x1, y1, 8.0f / 64.0f, 8.0f / 64.0f, 16.0f / 64.0f, 16.0f / 64.0f, indicatorLight, 255);
            drawFaceQuad(consumer, matrix, x0, y0, x1, y1, 40.0f / 64.0f, 8.0f / 64.0f, 48.0f / 64.0f, 16.0f / 64.0f, indicatorLight, 255);
        });

        Component arrowComp = Component.literal("▼");
        FormattedCharSequence arrowSeq = arrowComp.getVisualOrderText();
        float arrowWidth = font.width(arrowComp);
        nodeCollector.submitText(poseStack, -arrowWidth / 2.0f, y1 + 1.0f, arrowSeq, false, Font.DisplayMode.SEE_THROUGH, indicatorLight, 0xFFFFD700, 0, 0);
        nodeCollector.submitText(poseStack, -arrowWidth / 2.0f, y1 + 1.0f, arrowSeq, false, Font.DisplayMode.NORMAL, indicatorLight, 0xFFFFD700, 0, 0);

        Component name = player.getDisplayName();
        FormattedCharSequence nameSeq = name.getVisualOrderText();
        float nameWidth = font.width(name);
        nodeCollector.submitText(poseStack, -nameWidth / 2.0f, y1 + 10.0f, nameSeq, false, Font.DisplayMode.SEE_THROUGH, indicatorLight, 0xFFFFFFFF, 0x20000000, 0);
        nodeCollector.submitText(poseStack, -nameWidth / 2.0f, y1 + 10.0f, nameSeq, false, Font.DisplayMode.NORMAL, indicatorLight, 0xFFFFFFFF, 0, 0);

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
