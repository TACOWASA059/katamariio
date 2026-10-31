package com.github.tacowasa059.katamariio.client.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KatamariIndicatorHelperTest {

    @Test
    @DisplayName("Verify default scale factor at normal size and close distance")
    void testDefaultScale() {
        float mySize = 2.0f;
        float targetDiameter = 2.0f;
        double camDist = 6.0;

        float scale = KatamariIndicatorHelper.computeScaleFactor(mySize, targetDiameter, camDist);
        assertEquals(1.0f, scale, 0.05f, "Scale factor should be approximately 1.0 at normal size and standard distance");
    }

    @Test
    @DisplayName("Verify scaling when target player is large")
    void testTargetSizeScaling() {
        float mySize = 2.0f;
        double camDist = 6.0;

        float scaleNormal = KatamariIndicatorHelper.computeScaleFactor(mySize, 2.0f, camDist);
        float scale10m = KatamariIndicatorHelper.computeScaleFactor(mySize, 10.0f, camDist);
        float scale50m = KatamariIndicatorHelper.computeScaleFactor(mySize, 50.0f, camDist);

        assertTrue(scale10m > scaleNormal * 1.8f, "Scale should increase significantly for 10m target");
        assertTrue(scale50m > scale10m * 1.8f, "Scale should increase further for 50m target");
    }

    @Test
    @DisplayName("Verify scaling when local player is large")
    void testMySizeScaling() {
        float targetDiameter = 2.0f;
        double camDist = 6.0;

        float scaleNormal = KatamariIndicatorHelper.computeScaleFactor(2.0f, targetDiameter, camDist);
        float scaleMy10m = KatamariIndicatorHelper.computeScaleFactor(10.0f, targetDiameter, camDist);
        float scaleMy50m = KatamariIndicatorHelper.computeScaleFactor(50.0f, targetDiameter, camDist);

        assertTrue(scaleMy10m > scaleNormal * 2.0f, "Indicator scale should double for 10m player");
        assertTrue(scaleMy50m > scaleMy10m * 2.0f, "Indicator scale should expand further for 50m player");
    }

    @Test
    @DisplayName("Verify distance correction scaling when camera is far")
    void testDistanceScaling() {
        float mySize = 2.0f;
        float targetDiameter = 2.0f;

        float scaleNear = KatamariIndicatorHelper.computeScaleFactor(mySize, targetDiameter, 6.0);
        float scaleFar = KatamariIndicatorHelper.computeScaleFactor(mySize, targetDiameter, 60.0);

        assertTrue(scaleFar > scaleNear * 5.0f, "Scale factor should scale up by 5x+ for 10x distance to prevent tiny appearance");
    }

    @Test
    @DisplayName("Verify maximum scale clamping (150.0f)")
    void testMaxScaleClamp() {
        float scale = KatamariIndicatorHelper.computeScaleFactor(500.0f, 500.0f, 1000.0);
        assertEquals(KatamariIndicatorHelper.MAX_TOTAL_SCALE, scale, 1e-4f, "Extreme values must safely clamp to 150.0f");
    }

    @Test
    @DisplayName("Verify dynamic calculation of indicator margin and max draw distance")
    void testMarginAndDrawDistance() {
        // Margin
        float margin2m = KatamariIndicatorHelper.computeIndicatorMargin(2.0f);
        float margin20m = KatamariIndicatorHelper.computeIndicatorMargin(20.0f);
        assertEquals(0.6f, margin2m, 1e-4f, "Margin is 0.6f for 2m ball");
        assertTrue(margin20m > 1.5f, "Margin should expand beyond 1.5f for 20m ball to avoid clipping");

        // Draw distance
        double maxDistNormal = KatamariIndicatorHelper.computeMaxDrawDistance(2.0f, 2.0f);
        double maxDistHuge = KatamariIndicatorHelper.computeMaxDrawDistance(50.0f, 50.0f);
        assertTrue(maxDistNormal >= 512.0, "Normal max draw distance is at least 512m");
        assertTrue(maxDistHuge >= 600.0, "Huge player draw distance expands beyond 600m up to 1024m");
    }
}
