package com.github.tacowasa059.katamariio.common.event;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerRespawnEventListener {
    public static void onPlayerClone(Player originalPlayer, Player newPlayer) {
        if (originalPlayer instanceof ServerPlayer serverOriginal) {
            com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.onPlayerLogout(serverOriginal);
        }
        ICustomPlayerData originalPlayerData = (ICustomPlayerData) originalPlayer;
        ICustomPlayerData newPlayerData = (ICustomPlayerData) newPlayer;

        newPlayerData.katamariIO$setFlagAndSizeAndRestitution(
                originalPlayerData.katamariIO$getFlag(),
                KatamariIO.DEFAULT_BALL_SIZE,
                KatamariIO.DEFAULT_BALL_SIZE,
                originalPlayerData.katamariIO$getRestitutionCoefficient());
        newPlayerData.katamariIO$clearAttachedBlocks();
    }

    public static void onPlayerLogout(ServerPlayer player) {
        if (player != null) {
            com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.onPlayerLogout(player);
        }
    }

    public static void onPlayerChangeDimension(ServerPlayer player) {
        if (player != null) {
            com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.onPlayerLogout(player);
        }
    }

    public static void onPlayerLogin(ServerPlayer player) {
        if (player != null) {
            if (player.getVehicle() == null) {
                player.setNoGravity(false);
                player.noPhysics = false;
            }
            ICustomPlayerData data = (ICustomPlayerData) player;
            ModNetwork.sendInitialData(
                    player,
                    data.katamariIO$getSize(),
                    data.katamariIO$getRenderSize(),
                    data.katamariIO$getFlag(),
                    data.katamariIO$getRestitutionCoefficient(),
                    data.katamariIO$getQuaternion(),
                    data.katamariIO$getCurrentPosition()
            );
            ModNetwork.sendFullSnapshot(player, player);
            ModNetwork.sendConfigSync(player);
        }
    }

    public static void onStartTracking(ServerPlayer viewer, ServerPlayer tracked) {
        if (viewer != null && tracked != null) {
            ModNetwork.sendFullSnapshot(viewer, tracked);
        }
    }
}
