package com.github.tacowasa059.katamariio.forge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KatamariIO.MODID, value = Dist.CLIENT)
public final class ForgeClientStateSync {
    private static S2CPlayerPacket pendingPacket;

    private ForgeClientStateSync() {
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingPacket == null) {
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
