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

    /** Sends a katamari block delta to everyone tracking {@code owner} (including the owner). */
    public static void sendBlocksDelta(ServerPlayer owner, S2CKatamariDeltaPacket packet) {
        requireHandler().sendBlocksDelta(owner, packet);
    }

    /** Sends a full block snapshot of {@code owner} to {@code viewer} (or all trackers if viewer is null). */
    public static void sendFullSnapshot(ServerPlayer viewer, ServerPlayer owner) {
        requireHandler().sendFullSnapshot(viewer, owner);
    }

    private static NetworkHandler requireHandler() {
        if (handler == null) {
            throw new IllegalStateException("Katamari networking has not been bootstrapped");
        }
        return handler;
    }

    public static void sendRotationToServer(Quaternionf quaternion) {
        requireHandler().sendRotationToServer(quaternion);
    }

    public static void sendConfigSync(ServerPlayer player) {
        requireHandler().sendConfigSync(player);
    }

    public static void broadcastConfigSync(net.minecraft.server.MinecraftServer server) {
        requireHandler().broadcastConfigSync(server);
    }

    public interface NetworkHandler {
        void register();

        void sendToServer(RemoveBlockPacket packet);

        void sendRotationToServer(Quaternionf quaternion);

        void sendInitialData(ServerPlayer player, S2CPlayerPacket packet);

        void sendBlocksDelta(ServerPlayer owner, S2CKatamariDeltaPacket packet);

        void sendFullSnapshot(ServerPlayer viewer, ServerPlayer owner);

        void sendConfigSync(ServerPlayer player);

        void broadcastConfigSync(net.minecraft.server.MinecraftServer server);
    }
}
