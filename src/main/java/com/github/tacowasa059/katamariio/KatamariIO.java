package com.github.tacowasa059.katamariio;

import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import net.minecraftforge.fml.common.Mod;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(KatamariIO.MODID)
public class KatamariIO {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "katamariio";
    public static final float DEFAULT_BALL_SIZE = 2.0f;

    public KatamariIO() {
        ModDataSerializers.register();
        ModNetwork.register();
    }
}
