package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.networks.S2CConfigSyncPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CKatamariDeltaPacket;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = KatamariIO.MODID, value = Dist.CLIENT)
public final class NeoForgeClientStateSync {
    private static S2CPlayerPacket pendingPacket;

    private NeoForgeClientStateSync() {
    }

    public static void receive(S2CPlayerPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            pendingPacket = packet;
            return;
        }
        packet.applyToPlayer(player);
    }

    public static void receiveDelta(S2CKatamariDeltaPacket packet) {
        packet.apply(Minecraft.getInstance().level);
    }

    public static void receiveConfig(S2CConfigSyncPacket packet) {
        if (packet != null) {
            packet.apply();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (pendingPacket == null) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        S2CPlayerPacket packet = pendingPacket;
        pendingPacket = null;
        packet.applyToPlayer(player);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        KatamariConfig.load();
    }
}
