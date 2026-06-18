package com.github.tacowasa059.katamariio.forge;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.commands.KatamariCommands;
import com.github.tacowasa059.katamariio.common.event.PlayerRespawnEventListener;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(KatamariIO.MODID)
@Mod.EventBusSubscriber(modid = KatamariIO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KatamariIOForge {
    public KatamariIOForge() {
        ModNetwork.bootstrap(new ForgeModNetwork());
        ModDataSerializers.registerVanilla();
        KatamariIO.init();
    }

    @SubscribeEvent
    private static void onRegisterCommands(RegisterCommandsEvent event) {
        KatamariCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    private static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerRespawnEventListener.onPlayerClone(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PlayerRespawnEventListener.onPlayerLogin(player);
        }
    }
}
