package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.client.utils.SphereRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.accessors.IPlayerRendererAccessor;
import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
        if (!playerData.katamariIO$getFlag()) {
            return false;
        }
        if (player.isInvisible()) {
            return true;
        }

        // Keep the katamari lit even when buried in blocks, where the sampled light would be 0 (black).
        packedLight = LightTexture.pack(Math.max(LightTexture.block(packedLight), KATAMARI_MIN_BLOCK_LIGHT),
                LightTexture.sky(packedLight));

        float renderSize = playerData.katamariIO$getRenderSize();
        float collisionSize = playerData.katamariIO$getSize();
        Quaternionf quaternion = playerData.katamariIO$getInterpolatedQuaternion(partialTicks);
        ResourceLocation texture = player.getSkinTextureLocation();

        poseStack.pushPose();
        poseStack.translate(0, collisionSize / 2.0, 0);
        if (player.getVehicle() == null) {
            poseStack.mulPose(quaternion);
        } else {
            poseStack.mulPose(QuaternionUtils.getQuaternionFromEntity(player));
        }

        int overlay = OverlayTexture.NO_OVERLAY;
        if (player.hurtTime > 0 || player.deathTime > 0) {
            overlay = OverlayTexture.pack(OverlayTexture.u(partialTicks), OverlayTexture.v(true));
        }

        poseStack.pushPose();
        SphereRenderer.drawTexturedSphere(poseStack, buffer, texture, renderSize / 2.0f, 12, 0, 0, packedLight, true, overlay);
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
            MAX_SIZE = Math.max(renderSize, ballRadius * 2.0);
        }

        if (!KatamariBlockBuffer.tryRender(player, blocks, poseStack, packedLight)) {
            renderBlocksDirect(player, blocks, count, ballRadius, partialTicks, poseStack, buffer, packedLight, overlay, quaternion);
        }

        poseStack.popPose();

        EntityRenderer<?> entityRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (player instanceof RemotePlayer && entityRenderer instanceof IPlayerRendererAccessor accessor) {
            if (accessor.katamariIO$shouldShowName(player)) {
                accessor.katamariIO$callRenderName(player, player.getDisplayName(), poseStack, buffer, packedLight);
            }
        }

        return true;
    }

    // Fallback used only when the cached buffer cannot be built/drawn.
    private static void renderBlocksDirect(Player player, KatamariBlocks blocks, int count, double ballRadius, float partialTicks,
                                           PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay, Quaternionf quaternion) {
        boolean dense = ballRadius >= KATAMARI_DENSE_RADIUS;

        Vector3f viewDirLocal = null;
        CameraType cameraType = null;
        if (dense) {
            Entity camera = Minecraft.getInstance().getCameraEntity();
            Entity cameraEntity = camera == null ? player : camera;
            cameraType = Minecraft.getInstance().options.getCameraType();
            Vec3 viewDir = cameraEntity.getViewVector(partialTicks);
            viewDirLocal = new Vector3f((float) viewDir.x, (float) viewDir.y, (float) viewDir.z);
            viewDirLocal.rotate(new Quaternionf(quaternion).invert());
        }

        Vector3f localNormal = new Vector3f();
        Quaternionf blockRotation = new Quaternionf();
        for (int i = 0; i < count; i++) {
            float px = blocks.posX(i);
            float py = blocks.posY(i);
            float pz = blocks.posZ(i);
            if (dense) {
                localNormal.set(px, py, pz);
                float normalLenSq = localNormal.lengthSquared();
                if (normalLenSq > 1.0E-6f) {
                    localNormal.normalize();
                    float dot = localNormal.dot(viewDirLocal);
                    if (cameraType == CameraType.THIRD_PERSON_BACK ? dot >= 0.0f : dot <= 0.0f) {
                        continue;
                    }
                }
            }
            Block block = blocks.block(i);
            blocks.quatInto(i, blockRotation);
            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.mulPose(blockRotation);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), poseStack, buffer, packedLight, overlay);
            poseStack.popPose();
        }
    }
}
