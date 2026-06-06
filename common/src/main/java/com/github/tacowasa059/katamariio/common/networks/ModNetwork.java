package com.github.tacowasa059.katamariio.common.networks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class ModNetwork {
    private static NetworkHandler handler;

    public static void register() {
        requireHandler().register();
    }

    public static void bootstrap(NetworkHandler handler) {
        ModNetwork.handler = handler;
    }

    public static void sendToServer(RemoveBlockPacket packet) {
        requireHandler().sendToServer(packet);
    }

    public static void sendInitialData(ServerPlayer player,
                                       float collisionSize, float renderSize, boolean flag, float restitution,
                                       Quaternionf quaternion, Vec3 currentPos) {
        requireHandler().sendInitialData(player,
                new S2CPlayerPacket(collisionSize, renderSize, flag, restitution, quaternion, currentPos));
    }

    private static NetworkHandler requireHandler() {
        if (handler == null) {
            throw new IllegalStateException("Katamari networking has not been bootstrapped");
        }
        return handler;
    }

    public interface NetworkHandler {
        void register();

        void sendToServer(RemoveBlockPacket packet);

        void sendInitialData(ServerPlayer player, S2CPlayerPacket packet);
    }
}
