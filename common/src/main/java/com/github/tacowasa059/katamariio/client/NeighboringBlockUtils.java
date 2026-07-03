package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public class NeighboringBlockUtils {
    // Cap how many blocks the katamari eats per tick. Contacted-but-not-yet-eaten blocks are still
    // solid, so they are re-detected on following ticks and eaten then. This spreads the destruction
    // (and the resulting setBlock / chunk re-mesh / lighting / sync work) over several ticks instead
    // of doing a huge batch in a single tick, which caused severe spikes.
    private static final int MAX_REMOVALS_PER_TICK = 48;

    private static long currentTick = Long.MIN_VALUE;
    private static int removalsThisTick = 0;

    public static void processNeighboring(Player player, BlockPos blockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.equals(player)) {
            return;
        }

        long tick = player.level().getGameTime();
        if (tick != currentTick) {
            currentTick = tick;
            removalsThisTick = 0;
        }
        if (removalsThisTick >= MAX_REMOVALS_PER_TICK) {
            return;
        }
        removalsThisTick++;

        ModNetwork.sendToServer(new RemoveBlockPacket(blockPos, player.position()));
    }
}
