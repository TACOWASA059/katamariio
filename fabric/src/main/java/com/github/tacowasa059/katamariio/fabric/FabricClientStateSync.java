package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class FabricClientStateSync {
    private static S2CPlayerPacket pendingPacket;

    private FabricClientStateSync() {
    }

    public static void receive(S2CPlayerPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            pendingPacket = packet;
            return;
        }
        packet.applyToPlayer(player);
    }

    public static void flushPending() {
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
