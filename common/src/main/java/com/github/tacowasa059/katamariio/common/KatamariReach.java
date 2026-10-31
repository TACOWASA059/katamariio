package com.github.tacowasa059.katamariio.common;

/**
 * Shared interaction-reach model so Forge (reach attributes) and Fabric (client pick range) agree on
 * how far a katamari can reach. The eye sits high on the ball, so the far-side ground is roughly
 * "size" blocks from the eye; a small margin lets the player reach a little past the surface.
 */
public final class KatamariReach {
    public static final double MARGIN = 2.0;

    private KatamariReach() {
    }

    public static double reachFor(float size) {
        return size + MARGIN;
    }
}
