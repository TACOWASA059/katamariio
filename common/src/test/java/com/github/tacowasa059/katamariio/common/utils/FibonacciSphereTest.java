package com.github.tacowasa059.katamariio.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FibonacciSphereTest {

    private static final double FIBONACCI_GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private float[][] generateFibonacciPoints(int N) {
        float[][] dirs = new float[N][3];
        for (int i = 0; i < N; i++) {
            float y = 1.0f - (2.0f * i + 1.0f) / (float) N;
            float r = (float) Math.sqrt(Math.max(0.0f, 1.0f - y * y));
            double theta = i * FIBONACCI_GOLDEN_ANGLE;
            dirs[i][0] = (float) (r * Math.cos(theta));
            dirs[i][1] = y;
            dirs[i][2] = (float) (r * Math.sin(theta));
        }
        return dirs;
    }

    private int findClosestFibonacciBin(float nx, float ny, float nz, int N, float[][] dirs) {
        float clampedY = Math.max(-1.0f, Math.min(1.0f, ny));
        int k = Math.round(((1.0f - clampedY) * N - 1.0f) * 0.5f);
        if (k < 0) k = 0;
        if (k >= N) k = N - 1;

        int window = Math.min(16, N / 2);
        int start = Math.max(0, k - window);
        int end = Math.min(dirs.length - 1, k + window);

        int bestIdx = k;
        float bestDot = nx * dirs[k][0] + ny * dirs[k][1] + nz * dirs[k][2];

        for (int i = start; i <= end; i++) {
            if (i == k) continue;
            float dot = nx * dirs[i][0] + ny * dirs[i][1] + nz * dirs[i][2];
            if (dot > bestDot) {
                bestDot = dot;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    @Test
    @DisplayName("Verify all Fibonacci points are on unit sphere surface")
    void testPointsAreOnUnitSphere() {
        int N = 64;
        float[][] points = generateFibonacciPoints(N);
        assertEquals(N, points.length);

        for (int i = 0; i < N; i++) {
            float x = points[i][0];
            float y = points[i][1];
            float z = points[i][2];
            float lenSq = x * x + y * y + z * z;
            assertEquals(1.0f, lenSq, 1.0e-5f, "Point " + i + " must have unit length");
        }
    }

    @Test
    @DisplayName("Verify absence of pole singularities and uniform latitude spacing")
    void testUniformYDistribution() {
        int N = 64;
        float[][] points = generateFibonacciPoints(N);
        float expectedDeltaY = 2.0f / N;

        // Verify strictly monotonic decrease in Y with uniform spacing
        for (int i = 0; i < N - 1; i++) {
            float yCurrent = points[i][1];
            float yNext = points[i + 1][1];
            assertTrue(yCurrent > yNext, "Y must decrease monotonically");
            assertEquals(expectedDeltaY, yCurrent - yNext, 1.0e-5f, "Y spacing must be exactly 2/N");
        }

        // Top and bottom pole distances to boundary
        float northPoleY = points[0][1];
        float southPoleY = points[N - 1][1];
        assertEquals(1.0f - 1.0f / N, northPoleY, 1.0e-5f, "North pole Y must be 1 - 1/N");
        assertEquals(-1.0f + 1.0f / N, southPoleY, 1.0e-5f, "South pole Y must be -1 + 1/N");
    }

    @Test
    @DisplayName("Verify closest bin search matches global brute-force search for arbitrary directions")
    void testClosestBinSearchAccuracy() {
        int N = 64;
        float[][] points = generateFibonacciPoints(N);

        // Test diverse directions: cardinal axes, diagonals, arbitrary angles
        float[][] testDirs = new float[][]{
                {0, 1, 0},   // North pole
                {0, -1, 0},  // South pole
                {1, 0, 0},   // East (+X)
                {-1, 0, 0},  // West (-X)
                {0, 0, 1},   // South (+Z)
                {0, 0, -1},  // North (-Z)
                {0.577f, 0.577f, 0.577f},
                {-0.577f, 0.577f, -0.577f},
                {0.1f, 0.98f, 0.1f}, // near north pole
                {-0.1f, -0.98f, 0.1f} // near south pole
        };

        for (float[] dir : testDirs) {
            float len = (float) Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
            float nx = dir[0] / len;
            float ny = dir[1] / len;
            float nz = dir[2] / len;

            // Global brute force
            int bruteBest = 0;
            float bruteBestDot = -Float.MAX_VALUE;
            for (int i = 0; i < N; i++) {
                float dot = nx * points[i][0] + ny * points[i][1] + nz * points[i][2];
                if (dot > bruteBestDot) {
                    bruteBestDot = dot;
                    bruteBest = i;
                }
            }

            // Windowed search
            int windowBest = findClosestFibonacciBin(nx, ny, nz, N, points);
            float windowBestDot = nx * points[windowBest][0] + ny * points[windowBest][1] + nz * points[windowBest][2];

            assertEquals(bruteBestDot, windowBestDot, 1.0e-5f,
                    "Windowed search must match maximum dot product for dir (" + nx + ", " + ny + ", " + nz + ")");
        }
    }
}
