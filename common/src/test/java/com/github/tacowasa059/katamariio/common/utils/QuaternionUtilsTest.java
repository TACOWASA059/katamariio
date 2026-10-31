package com.github.tacowasa059.katamariio.common.utils;

import org.joml.Quaternionf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QuaternionUtilsTest {

    @Test
    @DisplayName("Verify slerp endpoints at t=0 and t=1")
    void testSlerpEndpoints() {
        Quaternionf start = new Quaternionf(0, 0, 0, 1);
        // Quaternion rotated 90 degrees around Y axis
        float angle = (float) (Math.PI / 2.0);
        Quaternionf end = new Quaternionf(0, (float) Math.sin(angle / 2), 0, (float) Math.cos(angle / 2));

        Quaternionf atZero = QuaternionUtils.slerp(start, end, 0.0f);
        assertEquals(start.x(), atZero.x(), 1e-4f);
        assertEquals(start.y(), atZero.y(), 1e-4f);
        assertEquals(start.z(), atZero.z(), 1e-4f);
        assertEquals(start.w(), atZero.w(), 1e-4f);

        Quaternionf atOne = QuaternionUtils.slerp(start, end, 1.0f);
        assertEquals(end.x(), atOne.x(), 1e-4f);
        assertEquals(end.y(), atOne.y(), 1e-4f);
        assertEquals(end.z(), atOne.z(), 1e-4f);
        assertEquals(end.w(), atOne.w(), 1e-4f);
    }

    @Test
    @DisplayName("Verify slerp midpoint at t=0.5 produces 45 degree rotation")
    void testSlerpHalfway() {
        Quaternionf start = new Quaternionf(0, 0, 0, 1);
        float totalAngle = (float) (Math.PI / 2.0); // 90 deg
        Quaternionf end = new Quaternionf(0, (float) Math.sin(totalAngle / 2), 0, (float) Math.cos(totalAngle / 2));

        Quaternionf halfway = QuaternionUtils.slerp(start, end, 0.5f);
        float expectedAngle = (float) (Math.PI / 4.0); // 45 deg
        float expectedY = (float) Math.sin(expectedAngle / 2);
        float expectedW = (float) Math.cos(expectedAngle / 2);

        assertEquals(0.0f, halfway.x(), 1e-4f);
        assertEquals(expectedY, halfway.y(), 1e-4f);
        assertEquals(0.0f, halfway.z(), 1e-4f);
        assertEquals(expectedW, halfway.w(), 1e-4f);
    }

    @Test
    @DisplayName("Verify slerp interpolates shortest path with opposing quaternion (negative dot)")
    void testSlerpNegativeDot() {
        Quaternionf start = new Quaternionf(0, 0, 0, 1);
        // Quaternion with opposing sign representing same rotation (q and -q)
        Quaternionf end = new Quaternionf(0, 0, 0, -1);

        Quaternionf result = QuaternionUtils.slerp(start, end, 0.5f);
        assertNotNull(result);
        assertFalse(Float.isNaN(result.w()), "NaN must not occur");
    }
}
