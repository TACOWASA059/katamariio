package com.github.tacowasa059.katamariio.common.utils;

import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KatamariBlockUtilsTest {

    @Test
    @DisplayName("Verify null safety and defaults for center offset")
    void testGetBlockCenterOffsetNull() {
        Vector3f offset = KatamariBlockUtils.getBlockCenterOffset(null, null);
        assertNotNull(offset);
        assertEquals(0.5f, offset.x, 1e-5f);
        assertEquals(0.5f, offset.y, 1e-5f);
        assertEquals(0.5f, offset.z, 1e-5f);
    }

    @Test
    @DisplayName("Verify null safety and defaults for block extent")
    void testGetBlockExtentNull() {
        float extent = KatamariBlockUtils.getBlockExtent(null, null);
        assertEquals(0.5f, extent, 1e-5f);
    }

    @Test
    @DisplayName("Verify null safety and defaults for getBounds")
    void testGetBoundsNull() {
        AABB bounds = KatamariBlockUtils.getBounds(null, null, null);
        assertNotNull(bounds);
        assertEquals(0.0, bounds.minX, 1e-5);
        assertEquals(0.0, bounds.minY, 1e-5);
        assertEquals(0.0, bounds.minZ, 1e-5);
        assertEquals(1.0, bounds.maxX, 1e-5);
        assertEquals(1.0, bounds.maxY, 1e-5);
        assertEquals(1.0, bounds.maxZ, 1e-5);
    }
}
