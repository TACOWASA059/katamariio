package com.github.tacowasa059.katamariio.common;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class KatamariMovement {
    private KatamariMovement() {
    }

    public static void bouncePlayer(Player player, float fallDistance, float restitutionCoefficient) {
        if (Math.abs(fallDistance) < 1f || player.level().isClientSide) {
            return;
        }

        double bounceVelocityY = Math.sqrt(2 * fallDistance * 0.08) * restitutionCoefficient;
        double restoredMotionX = player.getX() - player.xOld;
        double restoredMotionZ = player.getZ() - player.zOld;

        player.hasImpulse = true;
        player.setDeltaMovement(restoredMotionX, bounceVelocityY, restoredMotionZ);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }
}
