package com.github.tacowasa059.katamariio.common.data;

import org.joml.Quaternionf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KatamariBlocksTest {

    @Test
    @DisplayName("Verify initial state of empty store")
    void testEmptyStore() {
        KatamariBlocks blocks = new KatamariBlocks();
        assertEquals(0, blocks.size());
    }

    @Test
    @DisplayName("Verify adding and retrieving blocks, positions, and rotations")
    void testAddAndRetrieve() {
        KatamariBlocks blocks = new KatamariBlocks(4);
        blocks.addRaw(null, 0.0f, 0.0f, 0.0f, 1.0f, 1.5f, 2.5f, 3.5f);

        assertEquals(1, blocks.size());
        assertEquals(1.5f, blocks.posX(0), 1e-5f);
        assertEquals(2.5f, blocks.posY(0), 1e-5f);
        assertEquals(3.5f, blocks.posZ(0), 1e-5f);
        assertEquals(0.0f, blocks.quatX(0), 1e-5f);
        assertEquals(1.0f, blocks.quatW(0), 1e-5f);

        double expectedLenSqr = 1.5 * 1.5 + 2.5 * 2.5 + 3.5 * 3.5;
        assertEquals(expectedLenSqr, blocks.lengthSqr(0), 1e-4);
        assertEquals(Math.sqrt(expectedLenSqr), blocks.length(0), 1e-4);

        Quaternionf q = new Quaternionf();
        blocks.quatInto(0, q);
        assertEquals(1.0f, q.w(), 1e-5f);
    }

    @Test
    @DisplayName("Verify auto capacity expansion when adding many blocks")
    void testCapacityExpansion() {
        KatamariBlocks blocks = new KatamariBlocks(2);
        int count = 500;
        for (int i = 0; i < count; i++) {
            blocks.addRaw(null, 0f, 0f, 0f, 1f, (float) i, 0f, 0f);
        }
        assertEquals(count, blocks.size());
        assertEquals(499.0f, blocks.posX(499), 1e-5f);
    }

    @Test
    @DisplayName("Verify small balls are not pruned")
    void testPruneToSurfaceSmallBall() {
        KatamariBlocks blocks = new KatamariBlocks(10);
        // Blocks at radius 2.0 (below minRadius=6.0)
        blocks.addRaw(null, 0, 0, 0, 1, 1.0f, 0f, 0f);
        blocks.addRaw(null, 0, 0, 0, 1, 2.0f, 0f, 0f);

        boolean pruned = blocks.pruneToSurface(6.0f, 12, 48, 0.5f);
        assertFalse(pruned, "Balls smaller than minRadius should not be pruned");
        assertEquals(2, blocks.size());
    }

    @Test
    @DisplayName("Verify inner blocks are removed and only surface blocks remain in large balls")
    void testPruneToSurfaceLargeBall() {
        KatamariBlocks blocks = new KatamariBlocks(10);
        // In the same direction (+X), place inner block (r=2.0) and surface blocks (r=10.0)
        blocks.addRaw(null, 0, 0, 0, 1, 2.0f, 0f, 0f);   // Inner (to be pruned)
        blocks.addRaw(null, 0, 0, 0, 1, 9.8f, 0f, 0f);   // Near surface (within 0.5 thickness, kept)
        blocks.addRaw(null, 0, 0, 0, 1, 10.0f, 0f, 0f);  // Outermost (kept)

        boolean pruned = blocks.pruneToSurface(6.0f, 12, 48, 0.5f);
        assertTrue(pruned, "Inner blocks should be pruned");
        assertTrue(blocks.size() < 3, "Block count should decrease");
        // All remaining blocks should have radius >= 9.0
        for (int i = 0; i < blocks.size(); i++) {
            assertTrue(blocks.length(i) >= 9.0, "Remaining blocks should be near the surface");
        }
    }

    @Test
    @DisplayName("Verify clear operation sets size to 0")
    void testClear() {
        KatamariBlocks blocks = new KatamariBlocks(10);
        blocks.addRaw(null, 0, 0, 0, 1, 1f, 1f, 1f);
        blocks.addRaw(null, 0, 0, 0, 1, 2f, 2f, 2f);
        assertEquals(2, blocks.size());

        blocks.clear();
        assertEquals(0, blocks.size());
    }
}
