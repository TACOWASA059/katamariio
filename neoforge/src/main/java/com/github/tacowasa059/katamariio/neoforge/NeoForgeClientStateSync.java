package com.github.tacowasa059.katamariio.neoforge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

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

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
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
}
