package com.github.tacowasa059.katamariio.common.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility methods for computing accurate geometric bounds, center offsets, and extent radii
 * for attached blocks on a katamari sphere.
 */
public final class KatamariBlockUtils {
    private static final Map<Block, Vector3f> CENTER_OFFSET_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Float> EXTENT_CACHE = new ConcurrentHashMap<>();
    private static final Vector3f DEFAULT_CENTER = new Vector3f(0.5f, 0.5f, 0.5f);
    private static final float DEFAULT_EXTENT = 0.5f;

    private KatamariBlockUtils() {
    }

    /**
     * Gets the geometric center offset (0.0 to 1.0) of a block's default shape.
     * Full cubes return (0.5, 0.5, 0.5). Thin layers like snow return e.g. (0.5, 0.0625, 0.5).
     */
    public static Vector3f getBlockCenterOffset(Block block, BlockGetter level) {
        if (block == null || level == null) {
            return DEFAULT_CENTER;
        }
        return CENTER_OFFSET_CACHE.computeIfAbsent(block, b -> {
            try {
                BlockState state = b.defaultBlockState();
                VoxelShape shape = state.getShape(level, BlockPos.ZERO);
                if (shape.isEmpty()) {
                    return DEFAULT_CENTER;
                }
                AABB bounds = shape.bounds();
                return new Vector3f(
                        (float) ((bounds.minX + bounds.maxX) * 0.5),
                        (float) ((bounds.minY + bounds.maxY) * 0.5),
                        (float) ((bounds.minZ + bounds.maxZ) * 0.5)
                );
            } catch (Throwable t) {
                return DEFAULT_CENTER;
            }
        });
    }

    /**
     * Gets the effective radial extent (half-thickness) of a block's default shape.
     * Used to expand the katamari outer radius without over-inflating for thin blocks.
     */
    public static float getBlockExtent(Block block, BlockGetter level) {
        if (block == null || level == null) {
            return DEFAULT_EXTENT;
        }
        return EXTENT_CACHE.computeIfAbsent(block, b -> {
            try {
                BlockState state = b.defaultBlockState();
                VoxelShape shape = state.getShape(level, BlockPos.ZERO);
                if (shape.isEmpty()) {
                    return DEFAULT_EXTENT;
                }
                AABB bounds = shape.bounds();
                double height = bounds.maxY - bounds.minY;
                float halfHeight = (float) (height * 0.5);
                return Math.max(0.0625f, Math.min(0.5f, halfHeight));
            } catch (Throwable t) {
                return DEFAULT_EXTENT;
            }
        });
    }

    /**
     * Computes the bounding box of a specific BlockState in the world.
     */
    public static AABB getBounds(BlockState state, BlockGetter level, BlockPos pos) {
        if (state == null || level == null || pos == null) {
            return new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        }
        try {
            VoxelShape shape = state.getShape(level, pos);
            if (!shape.isEmpty()) {
                return shape.bounds();
            }
        } catch (Throwable ignored) {
        }
        return new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }
}
