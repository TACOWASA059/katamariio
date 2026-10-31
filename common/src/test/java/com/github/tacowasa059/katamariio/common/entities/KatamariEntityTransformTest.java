package com.github.tacowasa059.katamariio.common.entities;

import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KatamariEntityTransformTest {

    @Test
    @DisplayName("Verify distance invariance under local coordinate transform and rotation")
    void testEntityRotationInvariance() {
        Vector3f ballCenter = new Vector3f(100f, 64f, 100f);
        // Mob located 3 blocks in front of the ball
        Vector3f mobWorldPos = new Vector3f(100f, 64f, 103f);
        Vector3f offset = new Vector3f(mobWorldPos).sub(ballCenter);
        float originalDist = offset.length();

        // Initial ball rotation
        Quaternionf ballRot = new Quaternionf(new AxisAngle4f((float) Math.toRadians(30), 0, 1, 0));

        // 1. On attach: transform world offset to local coordinates
        Quaternionf invRot = new Quaternionf(ballRot).invert();
        Vector3f localPos = new Vector3f(offset).rotate(invRot);

        // 2. Ball rotates heavily (e.g. 90 deg around Y, 45 deg around X)
        Quaternionf newBallRot = new Quaternionf(ballRot)
                .rotateAxis((float) Math.toRadians(90), 0, 1, 0)
                .rotateAxis((float) Math.toRadians(45), 1, 0, 0);

        // 3. Tick update: compute current world position from local coordinates
        Vector3f currentOffset = new Vector3f(localPos).rotate(newBallRot);
        Vector3f newMobWorldPos = new Vector3f(ballCenter).add(currentOffset);

        // Distance from ball center to mob must be strictly preserved
        float currentDist = new Vector3f(newMobWorldPos).sub(ballCenter).length();
        assertEquals(originalDist, currentDist, 1e-4f, "Radial distance on ball surface must be completely preserved after rotation");
    }

    @Test
    @DisplayName("Verify ball returns to original position after full 360 degree rotation")
    void testFullCircleRotationReturnsToOriginal() {
        Vector3f ballCenter = new Vector3f(0f, 0f, 0f);
        Vector3f mobWorldPos = new Vector3f(0f, 0f, 5f);
        Quaternionf initialRot = new Quaternionf(); // No rotation

        Quaternionf invRot = new Quaternionf(initialRot).invert();
        Vector3f localPos = new Vector3f(mobWorldPos).rotate(invRot);

        // 360 degree rotation (2pi)
        Quaternionf fullTurn = new Quaternionf(initialRot).rotateAxis((float) (Math.PI * 2.0), 0, 1, 0);
        Vector3f returnedOffset = new Vector3f(localPos).rotate(fullTurn);

        assertEquals(mobWorldPos.x, returnedOffset.x, 1e-4f);
        assertEquals(mobWorldPos.y, returnedOffset.y, 1e-4f);
        assertEquals(mobWorldPos.z, returnedOffset.z, 1e-4f);
    }
}
