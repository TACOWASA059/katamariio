package com.github.tacowasa059.katamariio.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KatamariReachTest {

    @Test
    @DisplayName("Verify reach calculation for default size")
    void testDefaultSizeReach() {
        // Default size 1.0f -> reach is 1.0 + 2.0 = 3.0
        assertEquals(3.0, KatamariReach.reachFor(1.0f), 1e-4);
    }

    @Test
    @DisplayName("Verify reach scaling calculation for large size")
    void testLargeSizeReach() {
        // Size 10.0f -> reach is 12.0
        assertEquals(12.0, KatamariReach.reachFor(10.0f), 1e-4);

        // Size 50.0f -> reach is 52.0
        assertEquals(52.0, KatamariReach.reachFor(50.0f), 1e-4);
    }
}
