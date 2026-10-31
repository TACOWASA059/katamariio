package com.github.tacowasa059.katamariio.common.entities;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public final class KatamariEntityAttacher {
    private static final Map<UUID, List<AttachedEntity>> PLAYER_ENTITIES = new HashMap<>();
    private static final Map<UUID, Map<UUID, Vector3f>> CLIENT_PASSENGER_LOCAL_POS = new HashMap<>();
    public static final long COOLDOWN_TICKS = 60L; // 3 seconds (20 ticks/sec * 3)
    private static final Map<UUID, Long> ATTACH_COOLDOWNS = new HashMap<>();

    public static void setCooldown(Entity entity) {
        if (entity != null && entity.level() != null) {
            ATTACH_COOLDOWNS.put(entity.getUUID(), entity.level().getGameTime() + COOLDOWN_TICKS);
        }
    }

    public static boolean isCoolingDown(Entity entity) {
        if (entity == null || entity.level() == null) return false;
        Long expireTick = ATTACH_COOLDOWNS.get(entity.getUUID());
        if (expireTick == null) return false;
        if (entity.level().getGameTime() >= expireTick) {
            ATTACH_COOLDOWNS.remove(entity.getUUID());
            return false;
        }
        return true;
    }

    public static boolean isKatamariVehicle(Entity vehicle) {
        return vehicle instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag();
    }

    public static void applyPopout(Player parent, Entity passenger) {
        if (parent == null || passenger == null) return;

        ICustomPlayerData parentData = (ICustomPlayerData) parent;
        float radius = Math.max(parentData.katamariIO$getOuterRadius(), KatamariIO.DEFAULT_BALL_SIZE / 2.0f);
        Vec3 ballCenter = parent.position().add(0, radius, 0);
        Vec3 passengerPos = passenger.position();

        Vec3 dir = passengerPos.subtract(ballCenter);
        double horizontalDistSq = dir.x * dir.x + dir.z * dir.z;

        // When passenger is riding, its X and Z often match the parent's exactly, leaving horizontal distance ~ 0.
        // We calculate a distinct radial horizontal outward vector so passengers reliably scatter outwards.
        if (horizontalDistSq < 0.04) {
            double angle = ((passenger.getId() * 1.6180339887) % 1.0) * Math.PI * 2.0;
            dir = new Vec3(Math.cos(angle), 0.35, Math.sin(angle));
        }
        Vec3 norm = dir.normalize();

        // 1. Move position outside of the outer radius + passenger bounding box + margin
        double passengerSize = Math.max(passenger.getBbWidth(), passenger.getBbHeight());
        double popDist = radius + (passengerSize / 2.0) + 0.6;
        double targetX = ballCenter.x + norm.x * popDist;
        double targetY = Math.max(parent.getY(), ballCenter.y + norm.y * popDist);
        double targetZ = ballCenter.z + norm.z * popDist;

        if (passenger instanceof ServerPlayer serverPassenger && serverPassenger.connection != null) {
            serverPassenger.teleportTo(targetX, targetY, targetZ);
        } else {
            passenger.moveTo(targetX, targetY, targetZ, passenger.getYRot(), passenger.getXRot());
            passenger.setOldPosAndRot();
        }

        // 2. Impart outward pop-out velocity (angled outward and slightly upward)
        double horizontalSpeed = 0.55;
        double verticalSpeed = Math.max(norm.y * 0.45 + 0.3, 0.28);
        Vec3 popVelocity = new Vec3(norm.x * horizontalSpeed, verticalSpeed, norm.z * horizontalSpeed);
        passenger.setDeltaMovement(popVelocity);
        passenger.hurtMarked = true;
    }

    public static boolean hasCapacity(int currentCount) {
        int max = KatamariConfig.get().maxAttachedEntities;
        return max < 0 || currentCount < max;
    }

    public static class AttachedEntity {
        public final Entity entity;
        public final Vector3f localPos;
        public final Quaternionf attachQuat;
        public final float initialYRot;
        public final float initialXRot;
        public final float initialYBodyRot;
        public final float initialYHeadRot;

        public AttachedEntity(Entity entity, Vector3f localPos, Quaternionf attachQuat) {
            this.entity = entity;
            this.localPos = localPos;
            this.attachQuat = attachQuat;
            this.initialYRot = entity.getYRot();
            this.initialXRot = entity.getXRot();
            if (entity instanceof LivingEntity living) {
                this.initialYBodyRot = living.yBodyRot;
                this.initialYHeadRot = living.yHeadRot;
            } else {
                this.initialYBodyRot = entity.getYRot();
                this.initialYHeadRot = entity.getYRot();
            }
        }
    }

    public static Quaternionf getAttachQuat(Player player, Entity passenger) {
        ICustomPlayerData data = (ICustomPlayerData) player;
        Quaternionf synched = data.katamariIO$getPassengerAttachQuat(passenger.getId());
        if (synched != null) {
            return synched;
        }

        if (!player.level().isClientSide()) {
            List<AttachedEntity> list = PLAYER_ENTITIES.get(player.getUUID());
            if (list != null) {
                for (AttachedEntity a : list) {
                    if (a.entity.equals(passenger)) {
                        return a.attachQuat;
                    }
                }
            }
        }
        return null;
    }

    public static Vector3f getLocalPos(Player player, Entity passenger) {
        ICustomPlayerData data = (ICustomPlayerData) player;
        Vector3f synched = data.katamariIO$getPassengerLocalPos(passenger.getId());
        if (synched != null) {
            Map<UUID, Vector3f> map = CLIENT_PASSENGER_LOCAL_POS.get(player.getUUID());
            if (map != null) {
                map.remove(passenger.getUUID());
            }
            return synched;
        }

        if (!player.level().isClientSide()) {
            List<AttachedEntity> list = PLAYER_ENTITIES.get(player.getUUID());
            if (list != null) {
                for (AttachedEntity a : list) {
                    if (a.entity.equals(passenger)) {
                        return a.localPos;
                    }
                }
            }
            return null;
        }

        // Client-side fallback
        Map<UUID, Vector3f> map = CLIENT_PASSENGER_LOCAL_POS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        Vector3f local = map.get(passenger.getUUID());
        if (local == null) {
            float radius = data.katamariIO$getSize() / 2.0f;
            Vec3 ballCenter = player.position().add(0, radius, 0);
            Vec3 offset = passenger.position().subtract(ballCenter);
            Vec3 norm = offset.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : offset.normalize();
            Quaternionf invRot = new Quaternionf(data.katamariIO$getQuaternion()).invert();
            Vector3f localDir = new Vector3f((float) norm.x, (float) norm.y, (float) norm.z).rotate(invRot);
            float surfaceRadius = data.katamariIO$getRadiusInDirection(localDir.x, localDir.y, localDir.z);
            float entityHeight = passenger.getBbHeight();
            float embedDepth = Math.max(0.2f, entityHeight * 0.5f);
            float minFootRadius = Math.max(0.2f, radius * 0.25f);
            float targetRadius = Math.max(minFootRadius, surfaceRadius - embedDepth);
            local = new Vector3f(localDir).mul(targetRadius);
            map.put(passenger.getUUID(), local);
        }
        return local;
    }

    public static void tickPlayer(ServerPlayer player) {
        ICustomPlayerData data = (ICustomPlayerData) player;
        if (!data.katamariIO$getFlag() || player.isSpectator()) {
            releaseAll(player);
            return;
        }

        List<AttachedEntity> attached = PLAYER_ENTITIES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());

        boolean passengersChanged = attached.removeIf(a -> {
            if (!a.entity.isAlive() || a.entity.isRemoved()) {
                a.entity.stopRiding();
                data.katamariIO$removePassengerLocalPos(a.entity.getId());
                return true;
            }
            if (a.entity.level() != player.level()) {
                // If player traveled to Nether/End, release the mob safely in its original world
                a.entity.stopRiding();
                a.entity.setNoGravity(false);
                a.entity.noPhysics = false;
                if (a.entity instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                data.katamariIO$removePassengerLocalPos(a.entity.getId());
                return true;
            }
            return false;
        });

        float coreRadius = data.katamariIO$getSize() / 2.0f;
        Vec3 ballCenter = player.position().add(0, coreRadius, 0);
        Quaternionf ballRot = data.katamariIO$getQuaternion();
        float outerRadius = Math.max(coreRadius, data.katamariIO$getOuterRadius());

        // 1. Detect & attach nearby living entities if attachEntities is enabled and capacity allows
        boolean canAttachEntities = (!player.isPassenger() || KatamariConfig.get().attachWhileRiding);
        if (KatamariConfig.get().attachEntities && canAttachEntities && hasCapacity(attached.size())) {
            double searchRadius = outerRadius + 2.5;
            AABB searchBox = new AABB(ballCenter.x - searchRadius, ballCenter.y - searchRadius, ballCenter.z - searchRadius,
                                      ballCenter.x + searchRadius, ballCenter.y + searchRadius, ballCenter.z + searchRadius);

            List<Entity> nearby = player.level().getEntities(player, searchBox, e ->
                    e instanceof LivingEntity
                    && e.isAlive()
                    && KatamariConfig.get().isEntityAllowed(e)
                    && !isAttachedToAnyone(e)
                    && canAttachBall(player, e)
            );

            Quaternionf invRot = new Quaternionf(ballRot).invert();
            for (Entity entity : nearby) {
                if (!hasCapacity(attached.size())) {
                    break;
                }
                AABB box = entity.getBoundingBox();

                // Compute closest point on the entity's AABB to the ball center (true Sphere-AABB collision)
                double cx = Math.max(box.minX, Math.min(ballCenter.x, box.maxX));
                double cy = Math.max(box.minY, Math.min(ballCenter.y, box.maxY));
                double cz = Math.max(box.minZ, Math.min(ballCenter.z, box.maxZ));
                double dx = cx - ballCenter.x;
                double dy = cy - ballCenter.y;
                double dz = cz - ballCenter.z;
                double distSq = dx * dx + dy * dy + dz * dz;

                // Query the exact directional surface radius from the angular collision grid
                Vector3f localDir;
                if (distSq > 1.0E-6) {
                    float dist = (float) Math.sqrt(distSq);
                    localDir = new Vector3f((float) (dx / dist), (float) (dy / dist), (float) (dz / dist)).rotate(invRot);
                } else {
                    localDir = new Vector3f(0, 1, 0);
                }
                // Eat check based on current collision shell surface R
                double eatRadius = coreRadius + 0.5;
                if (distSq > eatRadius * eatRadius) {
                    continue; // Outside the spherical shell; do not attach!
                }

                float entityHeight = entity.getBbHeight();
                float entityWidth = entity.getBbWidth();
                float entityVolume = (float) (entityWidth * entityWidth * entityHeight * 0.5f);
                // Effective protrusion contribution to the surface shell
                float entityExtent = Math.min(0.4f, Math.max(entityWidth, entityHeight) * 0.25f);

                // Entities render upwards from their feet (origin y=0).
                // Embed foot position directly on the current collision shell (coreRadius)
                float embedDepth = Math.max(0.2f, entityHeight * 0.5f);
                float minFootRadius = Math.max(0.2f, coreRadius * 0.25f);
                float targetRadius = Math.max(minFootRadius, coreRadius - embedDepth);

                // Attach directly onto the collision shell surface in that direction
                Vector3f localPos = new Vector3f(localDir).mul(targetRadius);

                entity.setNoGravity(true);
                entity.noPhysics = true;
                if (entity instanceof Mob mob) {
                    mob.setNoAi(true);
                }
                // Register localPos and attachQuat in SynchedData and internal list prior to startRiding
                Quaternionf attachQuat = new Quaternionf(ballRot);
                attached.add(new AttachedEntity(entity, localPos, attachQuat));
                data.katamariIO$setPassengerLocalPos(entity.getId(), localPos, attachQuat, entityExtent, entityVolume);

                boolean started = entity.startRiding(player, true);
                if (started || entity.getVehicle() == player) {
                    passengersChanged = true;
                } else {
                    attached.removeIf(a -> a.entity.equals(entity));
                    data.katamariIO$removePassengerLocalPos(entity.getId());
                }
            }
        }

        // Vanilla ServerEntity tracker only broadcasts passenger changes to other tracking players ("seenBy"),
        // NEVER to the player entity itself. We must explicitly send ClientboundSetPassengersPacket to the player
        // so the local client knows it has passengers!
        if (passengersChanged) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(player));
        }

        // 2. Maintain riding state & synchronize position
        for (AttachedEntity a : new ArrayList<>(attached)) {
            if (a.entity instanceof ServerPlayer passengerPlayer && KatamariConfig.get().allowSelfDismount) {
                if (passengerPlayer.isShiftKeyDown()) {
                    detachPassenger(player, passengerPlayer);
                    continue;
                }
            }

            if (!a.entity.isPassenger() || a.entity.getVehicle() != player) {
                if (KatamariConfig.get().allowSelfDismount && a.entity instanceof Player) {
                    detachPassenger(player, a.entity);
                    continue;
                } else {
                    a.entity.startRiding(player, true);
                }
            }
            Vector3f currentOffset = new Vector3f(a.localPos).rotate(ballRot);
            Vec3 targetPos = ballCenter.add(currentOffset.x, currentOffset.y, currentOffset.z);
            a.entity.setPos(targetPos.x, targetPos.y, targetPos.z);
            a.entity.setYRot(a.initialYRot);
            a.entity.setXRot(a.initialXRot);
            if (a.entity instanceof LivingEntity living) {
                living.setYBodyRot(a.initialYBodyRot);
                living.setYHeadRot(a.initialYHeadRot);
            }
            a.entity.setOldPosAndRot();
            a.entity.setDeltaMovement(Vec3.ZERO);
            a.entity.fallDistance = 0.0f;
        }
    }

    public static void onPlayerLogout(Player player) {
        if (player == null) return;
        // 1. If player was a vehicle, release all its passengers
        releaseAll(player);

        // 2. If player was a passenger, detach it from its vehicle
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Player parentPlayer) {
            detachPassenger(parentPlayer, player);
        } else {
            detachFromAnyVehicle(player);
        }
    }

    public static void detachPassenger(Player parent, Entity passenger) {
        if (parent == null || passenger == null) return;

        List<AttachedEntity> attached = PLAYER_ENTITIES.get(parent.getUUID());
        if (attached != null) {
            attached.removeIf(a -> a.entity.equals(passenger) || a.entity.getUUID().equals(passenger.getUUID()));
            if (attached.isEmpty()) {
                PLAYER_ENTITIES.remove(parent.getUUID());
            }
        }

        ICustomPlayerData parentData = (ICustomPlayerData) parent;
        parentData.katamariIO$removePassengerLocalPos(passenger.getId());

        Map<UUID, Vector3f> clientMap = CLIENT_PASSENGER_LOCAL_POS.get(parent.getUUID());
        if (clientMap != null) {
            clientMap.remove(passenger.getUUID());
        }

        if (passenger.getVehicle() == parent) {
            passenger.stopRiding();
        }
        passenger.setNoGravity(false);
        passenger.noPhysics = false;
        if (passenger instanceof Mob mob) {
            mob.setNoAi(false);
        }
        applyPopout(parent, passenger);
        setCooldown(passenger);

        if (parent instanceof ServerPlayer serverParent && serverParent.connection != null) {
            serverParent.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(serverParent));
            if (serverParent.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(serverParent, new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(serverParent));
            }
        }
        if (passenger instanceof ServerPlayer serverPassenger && serverPassenger.connection != null) {
            serverPassenger.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(parent));
            serverPassenger.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPassenger));
        }
        if (passenger.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(passenger, new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(passenger));
            serverLevel.getChunkSource().broadcast(passenger, new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(passenger));
        }
    }

    public static void detachFromAnyVehicle(Entity passenger) {
        if (passenger == null) return;
        Player foundParent = null;
        for (Map.Entry<UUID, List<AttachedEntity>> entry : new ArrayList<>(PLAYER_ENTITIES.entrySet())) {
            List<AttachedEntity> list = entry.getValue();
            boolean found = list.removeIf(a -> a.entity.equals(passenger) || a.entity.getUUID().equals(passenger.getUUID()));
            if (found) {
                if (passenger.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    Entity parent = serverLevel.getEntity(entry.getKey());
                    if (parent instanceof ServerPlayer serverParent) {
                        foundParent = serverParent;
                        ((ICustomPlayerData) serverParent).katamariIO$removePassengerLocalPos(passenger.getId());
                        if (serverParent.connection != null) {
                            serverParent.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(serverParent));
                            serverLevel.getChunkSource().broadcast(serverParent, new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(serverParent));
                        }
                    }
                }
            }
        }
        passenger.setNoGravity(false);
        passenger.noPhysics = false;
        if (passenger instanceof Mob mob) {
            mob.setNoAi(false);
        }
        if (passenger.isPassenger()) {
            passenger.stopRiding();
        }
        if (foundParent != null) {
            applyPopout(foundParent, passenger);
        }
        setCooldown(passenger);
        if (passenger instanceof ServerPlayer serverPassenger && serverPassenger.connection != null) {
            if (foundParent != null) {
                serverPassenger.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(foundParent));
            }
            serverPassenger.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPassenger));
        }
        if (passenger.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(passenger, new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(passenger));
            serverLevel.getChunkSource().broadcast(passenger, new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(passenger));
        }
    }

    public static void releaseAll(Player player) {
        if (player == null) return;

        // Collect all passenger entities from BOTH player.getPassengers() and PLAYER_ENTITIES
        Set<Entity> allPassengers = new LinkedHashSet<>(player.getPassengers());
        List<AttachedEntity> attached = PLAYER_ENTITIES.remove(player.getUUID());
        if (attached != null) {
            for (AttachedEntity a : attached) {
                if (a != null && a.entity != null) {
                    allPassengers.add(a.entity);
                }
            }
        }
        CLIENT_PASSENGER_LOCAL_POS.remove(player.getUUID());
        ICustomPlayerData data = (ICustomPlayerData) player;
        data.katamariIO$clearPassengerLocalPos();

        // Unmount, restore physics/AI, pop out radially, and sync to clients
        for (Entity passenger : allPassengers) {
            if (passenger == null) continue;
            if (passenger.isPassenger() && passenger.getVehicle() == player) {
                passenger.stopRiding();
            }
            passenger.setNoGravity(false);
            passenger.noPhysics = false;
            if (passenger instanceof Mob mob) {
                mob.setNoAi(false);
            }
            applyPopout(player, passenger);
            setCooldown(passenger);

            if (passenger instanceof ServerPlayer serverPassenger && serverPassenger.connection != null) {
                serverPassenger.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(player));
                serverPassenger.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPassenger));
            }
            if (passenger.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(passenger, new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(passenger));
                serverLevel.getChunkSource().broadcast(passenger, new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(passenger));
            }
        }

        player.ejectPassengers();

        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(serverPlayer));
            if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(serverPlayer, new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(serverPlayer));
            }
        }
    }

    private static boolean isAttachedToAnyone(Entity e) {
        for (List<AttachedEntity> list : PLAYER_ENTITIES.values()) {
            for (AttachedEntity a : list) {
                if (a.entity.equals(e)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean canAttachBall(Player player, Entity entity) {
        if (entity == null || player == null) {
            return false;
        }

        if (isCoolingDown(entity)) {
            return false;
        }

        ICustomPlayerData myData = (ICustomPlayerData) player;
        float myOuterSize = myData.katamariIO$getOuterSize();

        if (entity instanceof Player otherPlayer) {
            ICustomPlayerData otherData = (ICustomPlayerData) otherPlayer;

            if (otherData.katamariIO$getFlag()) {
                // Ball vs Ball: the katamari with the larger outer size (or more attached blocks) consumes the smaller one
                float otherOuterSize = otherData.katamariIO$getOuterSize();
                if (Math.abs(myOuterSize - otherOuterSize) < 1e-4f) {
                    return myData.katamariIO$getAttachedBlockCount() > otherData.katamariIO$getAttachedBlockCount();
                }
                return myOuterSize > otherOuterSize;
            } else {
                // Regular player: outer size must be strictly larger than the player's bounding box extent
                double otherSize = Math.max(otherPlayer.getBbWidth(), otherPlayer.getBbHeight());
                return myOuterSize > otherSize;
            }
        }

        // Living entities: outer size must be strictly larger than the entity's bounding box extent
        double entitySize = Math.max(entity.getBbWidth(), entity.getBbHeight());
        return myOuterSize > entitySize;
    }

    public static boolean canAttachEntity(Player player, Entity entity) {
        return canAttachBall(player, entity);
    }
}
