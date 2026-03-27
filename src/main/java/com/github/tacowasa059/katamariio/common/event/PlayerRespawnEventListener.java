package com.github.tacowasa059.katamariio.common.event;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KatamariIO.MODID,bus= Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerRespawnEventListener {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player originalPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        ICustomPlayerData original_playerData = (ICustomPlayerData)originalPlayer;
        ICustomPlayerData new_playerData = (ICustomPlayerData)newPlayer;

        new_playerData.katamariIO$setFlagAndSizeAndRestitution(
                original_playerData.katamariIO$getFlag(), KatamariIO.DEFAULT_BALL_SIZE,
                KatamariIO.DEFAULT_BALL_SIZE,
                original_playerData.katamariIO$getRestitutionCoefficient());
        new_playerData.katamariIO$clearAttachedBlocks();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ICustomPlayerData data = (ICustomPlayerData) event.getEntity();
            ModNetwork.sendInitialData(
                    (ServerPlayer) event.getEntity(),
                    data.katamariIO$getSize(),
                    data.katamariIO$getRenderSize(),
                    data.katamariIO$getFlag(),
                    data.katamariIO$getRestitutionCoefficient(),
                    data.katamariIO$getQuaternion(),
                    data.katamariIO$getCurrentPosition()
            );
        }
    }

}
