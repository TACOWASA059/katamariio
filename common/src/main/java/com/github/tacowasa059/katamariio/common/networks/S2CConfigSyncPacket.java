package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import net.minecraft.network.FriendlyByteBuf;

public class S2CConfigSyncPacket {
    private final String json;

    public S2CConfigSyncPacket(String json) {
        this.json = json != null ? json : "";
    }

    public static void encode(S2CConfigSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.json);
    }

    public static S2CConfigSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CConfigSyncPacket(buf.readUtf());
    }

    public void apply() {
        KatamariConfig.applySyncedJson(json);
        try {
            net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (localPlayer instanceof com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData data) {
                data.katamariIO$invalidateBinRadii();
            }
        } catch (Throwable ignored) {
            // Dedicated server environment or before player is created
        }
    }
}
