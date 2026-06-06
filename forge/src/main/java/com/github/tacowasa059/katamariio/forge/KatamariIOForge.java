package com.github.tacowasa059.katamariio.forge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.commands.KatamariCommands;
import com.github.tacowasa059.katamariio.common.event.PlayerRespawnEventListener;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(KatamariIO.MODID)
public class KatamariIOForge {
    public KatamariIOForge() {
        ModNetwork.bootstrap(new ForgeModNetwork());
        KatamariIO.init();

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        KatamariCommands.register(event.getDispatcher());
    }

    private void onPlayerClone(PlayerEvent.Clone event) {
        PlayerRespawnEventListener.onPlayerClone(event.getOriginal(), event.getEntity());
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PlayerRespawnEventListener.onPlayerLogin(player);
        }
    }
}
