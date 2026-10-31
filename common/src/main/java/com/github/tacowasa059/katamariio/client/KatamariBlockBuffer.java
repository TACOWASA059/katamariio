package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * GPU-resident cache of a katamari's blocks. Since the stored blocks are already pruned to the
 * visible surface, the whole set is baked (in ball-local space, i.e. before the ball's spin) into a
 * {@link VertexBuffer} whenever it changes, and drawn with a single call each frame using the
 * current ball transform.
 * <p>
 * This touches GL/shader state that can only be validated in a running client, so every failure
 * mode disables the cache for that player and the renderer falls back to per-block rendering.
 * There is therefore no crash risk: worst case is the previous behaviour.
 */
public final class KatamariBlockBuffer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Integer, Entry> CACHE = new HashMap<>();
    private static final long REBUILD_TICKS = 10L;
    // Reused across rebuilds so we don't allocate a fresh (direct-memory) buffer every 10 ticks.
    // Replaced only if a build fails, since the shared instance could be left mid-build.
    private static ByteBufferBuilder byteBuffer = new ByteBufferBuilder(4096);

    private KatamariBlockBuffer() {
    }

    /**
     * Draws every stored block via the cached buffer. Returns true if it handled the draw (including
     * the "nothing to draw" case); returns false if the caller should fall back to per-block rendering.
     */
    public static boolean tryRender(Player player, KatamariBlocks blocks, PoseStack poseStack, int packedLight) {
        int id = player.getId();
        Entry entry = CACHE.get(id);
        if (entry != null && entry.disabled) {
            return false;
        }

        try {
            int count = blocks.size();
            if (count == 0) {
                return true;
            }

            long now = player.level().getGameTime();
            // Rebuild only when the set actually changed, and at most once per interval. This avoids
            // both re-baking on every pickup (severe stutter for a large ball) and pointless rebuilds
            // while idle.
            if (entry == null || (count != entry.bakedCount && now - entry.builtTick >= REBUILD_TICKS)) {
                if (entry != null && entry.buffer != null) {
                    entry.buffer.close();
                }
                entry = build(blocks, player.level(), packedLight, now);
                CACHE.put(id, entry);
            }

            if (entry.buffer == null) {
                return false;
            }
            draw(entry.buffer, poseStack);
            return true;
        } catch (Throwable t) {
            if (entry != null && entry.buffer != null) {
                try {
                    entry.buffer.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
            LOGGER.warn("[KatamariIO] cached block buffer failed; falling back to per-block rendering", t);
            // The shared builder may be stuck mid-build; replace it so other players are unaffected.
            try {
                byteBuffer.close();
            } catch (Throwable ignored) {
                // ignore
            }
            byteBuffer = new ByteBufferBuilder(4096);
            CACHE.put(id, Entry.disabled());
            return false;
        }
    }

    private static Entry build(KatamariBlocks blocks, net.minecraft.world.level.BlockGetter level, int packedLight, long now) {
        int count = blocks.size();
        BufferBuilder builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        MultiBufferSource source = renderType -> builder;

        PoseStack local = new PoseStack();
        Quaternionf rotation = new Quaternionf();
        for (int i = 0; i < count; i++) {
            local.pushPose();
            local.translate(blocks.posX(i), blocks.posY(i), blocks.posZ(i));
            rotation.set(blocks.quatX(i), blocks.quatY(i), blocks.quatZ(i), blocks.quatW(i));
            local.mulPose(rotation);
            net.minecraft.world.level.block.Block block = blocks.block(i);
            Vector3f centerOffset = com.github.tacowasa059.katamariio.common.utils.KatamariBlockUtils.getBlockCenterOffset(block, level);
            local.translate(-centerOffset.x, -centerOffset.y, -centerOffset.z);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    block.defaultBlockState(), local, source, packedLight, OverlayTexture.NO_OVERLAY);
            local.popPose();
        }

        MeshData rendered = builder.build();
        if (rendered == null) {
            return new Entry(null, count, now, false);
        }
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(rendered);
        VertexBuffer.unbind();
        return new Entry(buffer, count, now, false);
    }

    private static void draw(VertexBuffer buffer, PoseStack poseStack) {
        RenderType renderType = RenderType.cutout();
        renderType.setupRenderState();
        buffer.bind();
        buffer.drawWithShader(new Matrix4f(poseStack.last().pose()), RenderSystem.getProjectionMatrix(),
                GameRenderer.getRendertypeCutoutShader());
        VertexBuffer.unbind();
        renderType.clearRenderState();
    }

    private static final class Entry {
        private final VertexBuffer buffer;
        private final int bakedCount;
        private final long builtTick;
        private final boolean disabled;

        private Entry(VertexBuffer buffer, int bakedCount, long builtTick, boolean disabled) {
            this.buffer = buffer;
            this.bakedCount = bakedCount;
            this.builtTick = builtTick;
            this.disabled = disabled;
        }

        static Entry disabled() {
            return new Entry(null, 0, 0L, true);
        }
    }
}
