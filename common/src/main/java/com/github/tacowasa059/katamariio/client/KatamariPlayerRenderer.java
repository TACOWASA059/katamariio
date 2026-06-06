package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.client.utils.SphereRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.accessors.IPlayerRendererAccessor;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KatamariPlayerRenderer {
    public static double MAX_SIZE = 0;
    private static final float KATAMARI_BAKED_RENDER_THRESHOLD = 10.0f;
    private static final int KATAMARI_BAKED_LAT_BINS = 12;
    private static final int KATAMARI_BAKED_LON_BINS = 24;
    private static final long KATAMARI_BAKED_REBUILD_TICKS = 10L;
    private static final Map<Integer, BakedIndexCache> KATAMARI_BAKED_CACHE = new HashMap<>();

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

        double tmpSize = renderSize;
        List<Vec3> vec3List = playerData.katamariIO$getSphericalPlayerPositions();
        List<Block> blockList = playerData.katamariIO$getSphericalPlayerBlocks();
        List<Quaternionf> quaternionfList = playerData.katamariIO$getSphericalPlayerQuaternions();
        int lodStep = getLodStepForPlayer(player);
        Entity camera = Minecraft.getInstance().getCameraEntity();
        Entity cameraEntity = camera == null ? player : camera;
        CameraType cameraType = Minecraft.getInstance().options.getCameraType();
        Vec3 viewDir = cameraEntity.getViewVector(partialTicks);
        Vector3f viewDirLocal = new Vector3f((float) viewDir.x, (float) viewDir.y, (float) viewDir.z);
        Quaternionf inverseBallRotation = new Quaternionf(quaternion).invert();
        viewDirLocal.rotate(inverseBallRotation);
        int sharedSize = Math.min(vec3List.size(), Math.min(blockList.size(), quaternionfList.size()));

        List<Integer> renderIndices;
        if (renderSize < KATAMARI_BAKED_RENDER_THRESHOLD) {
            renderIndices = new ArrayList<>(sharedSize);
            for (int i = 0; i < sharedSize; i++) {
                renderIndices.add(i);
            }
        } else {
            renderIndices = getBakedIndices(player, vec3List, sharedSize);
        }

        for (int i : renderIndices) {
            Vec3 vec = vec3List.get(i);
            if (renderSize < KATAMARI_BAKED_RENDER_THRESHOLD && !shouldRenderByHash(vec, i, player.getId(), lodStep)) {
                continue;
            }
            Vector3f localNormal = new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
            float normalLenSq = localNormal.lengthSquared();
            if (normalLenSq > 1.0E-6f) {
                localNormal.normalize();
                float dot = localNormal.dot(viewDirLocal);
                if (cameraType == CameraType.THIRD_PERSON_BACK ? dot >= 0.0f : dot <= 0.0f) {
                    continue;
                }
            }
            Block block = blockList.get(i);
            Quaternionf blockRotation = quaternionfList.get(i);

            poseStack.pushPose();
            poseStack.translate(vec.x, vec.y, vec.z);
            poseStack.mulPose(blockRotation);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), poseStack, buffer, packedLight, overlay);
            poseStack.popPose();

            if (player.equals(Minecraft.getInstance().player)) {
                tmpSize = Math.max(tmpSize, vec.length() * 2);
            }
        }

        if (player.equals(Minecraft.getInstance().player)) {
            MAX_SIZE = tmpSize;
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

    private static int getLodStepForPlayer(Player player) {
        double distanceSq = getCameraDistanceSq(player);
        if (distanceSq >= 256.0 * 256.0) return 8;
        if (distanceSq >= 128.0 * 128.0) return 4;
        if (distanceSq >= 64.0 * 64.0) return 2;
        return 1;
    }

    private static double getCameraDistanceSq(Player player) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return 0.0;
        }
        return camera.position().distanceToSqr(player.position());
    }

    private static boolean shouldRenderByHash(Vec3 vec, int index, int entityId, int step) {
        if (step <= 1) return true;
        long x = Math.round(vec.x * 2.0);
        long y = Math.round(vec.y * 2.0);
        long z = Math.round(vec.z * 2.0);
        int hash = (int) (x * 73428767L ^ y * 912931L ^ z * 389129L ^ (long) index * 12289L ^ (long) entityId * 19997L);
        return Math.floorMod(hash, step) == 0;
    }

    private static List<Integer> getBakedIndices(Player player, List<Vec3> vec3List, int sharedSize) {
        long nowTick = player.level().getGameTime();
        int playerId = player.getId();
        BakedIndexCache cache = KATAMARI_BAKED_CACHE.get(playerId);
        if (cache != null && cache.sharedSize == sharedSize && nowTick - cache.builtTick < KATAMARI_BAKED_REBUILD_TICKS) {
            return cache.indices;
        }

        int totalBins = KATAMARI_BAKED_LAT_BINS * KATAMARI_BAKED_LON_BINS;
        double[] maxDistSq = new double[totalBins];
        int[] maxIndex = new int[totalBins];
        for (int i = 0; i < totalBins; i++) {
            maxDistSq[i] = -1.0;
            maxIndex[i] = -1;
        }

        for (int i = 0; i < sharedSize; i++) {
            Vec3 vec = vec3List.get(i);
            double lenSq = vec.lengthSqr();
            if (lenSq < 1.0E-8) {
                continue;
            }

            int binIndex = getBinIndex(lenSq, vec);

            if (lenSq > maxDistSq[binIndex]) {
                maxDistSq[binIndex] = lenSq;
                maxIndex[binIndex] = i;
            }
        }

        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < totalBins; i++) {
            if (maxIndex[i] >= 0) {
                indices.add(maxIndex[i]);
            }
        }

        KATAMARI_BAKED_CACHE.put(playerId, new BakedIndexCache(nowTick, sharedSize, indices));
        return indices;
    }

    private static int getBinIndex(double lenSq, Vec3 vec) {
        double len = Math.sqrt(lenSq);
        double nx = vec.x / len;
        double ny = vec.y / len;
        double nz = vec.z / len;
        int latBin = (int) ((Math.asin(Math.max(-1.0, Math.min(1.0, ny))) + (Math.PI / 2.0)) / Math.PI * KATAMARI_BAKED_LAT_BINS);
        int lonBin = (int) ((Math.atan2(nz, nx) + Math.PI) / (Math.PI * 2.0) * KATAMARI_BAKED_LON_BINS);
        if (latBin < 0) latBin = 0;
        if (latBin >= KATAMARI_BAKED_LAT_BINS) latBin = KATAMARI_BAKED_LAT_BINS - 1;
        if (lonBin < 0) lonBin = 0;
        if (lonBin >= KATAMARI_BAKED_LON_BINS) lonBin = KATAMARI_BAKED_LON_BINS - 1;
        return latBin * KATAMARI_BAKED_LON_BINS + lonBin;
    }

    private record BakedIndexCache(long builtTick, int sharedSize, List<Integer> indices) {
    }
}
