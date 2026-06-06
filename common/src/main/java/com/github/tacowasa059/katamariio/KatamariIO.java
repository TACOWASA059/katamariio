package com.github.tacowasa059.katamariio;

import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;

public final class KatamariIO {
    public static final String MODID = "katamariio";
    public static final float DEFAULT_BALL_SIZE = 2.0f;

    private static boolean initialized;

    private KatamariIO() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ModDataSerializers.register();
        ModNetwork.register();
    }
}
