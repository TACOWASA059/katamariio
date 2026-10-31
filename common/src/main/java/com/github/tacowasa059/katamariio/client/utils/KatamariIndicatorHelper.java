package com.github.tacowasa059.katamariio.client.utils;

public final class KatamariIndicatorHelper {
    public static final float BASE_SCALE = 0.025f * (2.0f / 3.0f);
    public static final float MAX_TOTAL_SCALE = 150.0f;
    public static final double DEFAULT_CAMERA_DISTANCE = 6.0;

    private KatamariIndicatorHelper() {}

    /**
     * 球体の頂点からの浮遊マージンを計算。
     * 球体が大きくなるにつれてマージンを適度に広げ、球体表面に埋もれないようにする。
     */
    public static float computeIndicatorMargin(float ballDiameter) {
        return 0.6f + Math.max(0.0f, (ballDiameter - 2.0f) * 0.06f);
    }

    /**
     * Computes the maximum draw distance (in blocks/meters) for the player indicator.
     * Guarantees rendering across large distances (min 512m, max 1024m) so distant players remain visible.
     */
    public static double computeMaxDrawDistance(float mySize, float targetDiameter) {
        double maxDist = Math.max(512.0, Math.max(mySize, targetDiameter) * 12.0);
        return Math.min(maxDist, 1024.0);
    }

    /**
     * Computes the total scale multiplier for the indicator.
     * - targetDiameter: Diameter of the target player's ball (larger balls have larger indicators)
     * - mySize: Diameter of the local player's ball (compensates for zoomed-out camera on large katamaris)
     * - camDistance: Distance from the camera to the indicator
     */
    public static float computeScaleFactor(float mySize, float targetDiameter, double camDistance) {
        // 1. Scale with target player's size
        float targetFactor = (float) Math.pow(Math.max(1.0f, targetDiameter / 2.0f), 0.45);

        // 2. Scale with local player's size (compensate for camera distance)
        float myFactor = (float) Math.pow(Math.max(1.0f, mySize / 2.0f), 0.50);

        // 3. Maintain visibility across distances (keep at least 75% apparent size)
        float distFactor = (float) (0.25 + 0.75 * Math.max(1.0, camDistance / DEFAULT_CAMERA_DISTANCE));

        // Use maximum of myFactor and distFactor to preserve readability
        float viewFactor = Math.max(myFactor, distFactor);
        float totalScale = targetFactor * viewFactor;

        return Math.min(totalScale, MAX_TOTAL_SCALE);
    }
}
