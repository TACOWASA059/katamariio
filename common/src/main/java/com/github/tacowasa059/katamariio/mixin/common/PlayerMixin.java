package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.KatamariMovement;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Player.class)
public abstract class PlayerMixin implements ICustomPlayerData {
    @Unique
    private static final EntityDataAccessor<Float> sphericalPlayerMod$SIZE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> sphericalPlayerMod$RENDER_SIZE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Boolean> sphericalPlayerMod$FLAG = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Float> RESTITUTION_COEFFICIENT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<CompoundTag> sphericalPlayerMod$QUATERNION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
    @Unique
    private static final EntityDataAccessor<CompoundTag> CURRENT_POSITION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
    @Unique
    private static final EntityDataAccessor<List<Vec3>> SPHERICAL_PLAYER_POSITION_LIST = SynchedEntityData.defineId(Player.class, ModDataSerializers.VEC3_LIST);
    @Unique
    private static final EntityDataAccessor<List<Quaternionf>> SPHERICAL_PLAYER_QUATERNION_LIST = SynchedEntityData.defineId(Player.class, ModDataSerializers.QUATERNION_LIST);
    @Unique
    private static final EntityDataAccessor<List<Block>> SPHERICAL_PLAYER_BLOCK_LIST = SynchedEntityData.defineId(Player.class, ModDataSerializers.BLOCK_LIST);
    @Unique
    private static final EntityDataAccessor<Integer> SPHERICAL_PLAYER_BLOCK_COUNT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);

    @Unique
    private boolean sphericalPlayerMod$initialized = false;
    @Unique
    private Quaternionf sphericalPlayerMod$quaternion = new Quaternionf(0, 0, 0, 1);
    @Unique
    private Quaternionf sphericalPlayerMod$prevQuaternion = new Quaternionf(0, 0, 0, 1);
    @Unique
    private static final int KATAMARI_COLLISION_LAT_BINS = 6;
    @Unique
    private static final int KATAMARI_COLLISION_LON_BINS = 12;
    @Unique
    private static final float KATAMARI_BLOCK_EXTENT = 0.5f;
    @Unique
    private static final float KATAMARI_DP_CUTOFF_OFFSET = 2.0f;
    @Unique
    private static final String SPM_ATTACHED_BLOCKS = "SPM_AttachedBlocks";
    @Unique
    private float[] katamariIO$binMaxRadii = null;
    @Unique
    private boolean katamariIO$binRadiiDirty = true;

    @Inject(method = "tick", at = @At("HEAD"))
    protected void tick(CallbackInfo ci) {
        if (!sphericalPlayerMod$initialized) {
            sphericalPlayerMod$quaternion = katamariIO$getValidQuaternion(katamariIO$getQuaternion());
            sphericalPlayerMod$prevQuaternion = katamariIO$getValidQuaternion(katamariIO$getQuaternion());
            sphericalPlayerMod$initialized = true;
        }

        Player player = (Player) (Object) this;
        if (player.level().isClientSide) {
            sphericalPlayerMod$prevQuaternion = katamariIO$getValidQuaternion(new Quaternionf(sphericalPlayerMod$quaternion));
            Quaternionf quaternion = QuaternionUtils.getUpdatedQuaternion(player.position(),
                    katamariIO$getCurrentPosition(), (ICustomPlayerData) player);
            if (quaternion == null) {
                quaternion = katamariIO$getQuaternion();
            }
            sphericalPlayerMod$quaternion = katamariIO$getValidQuaternion(quaternion);
            EntityDimensions expectedDimensions = player.getDimensions(player.getPose());
            if (player.tickCount % 5 == 0
                    && (Math.abs(expectedDimensions.width - (float) player.getBoundingBox().getXsize()) > 1.0E-4f
                    || Math.abs(expectedDimensions.height - (float) player.getBoundingBox().getYsize()) > 1.0E-4f)) {
                player.refreshDimensions();
            }
        }
    }

    @Override
    public Quaternionf katamariIO$getInterpolatedQuaternion(float partialTicks) {
        return QuaternionUtils.slerp(sphericalPlayerMod$prevQuaternion, sphericalPlayerMod$quaternion, partialTicks);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    protected void defineSynchedData(CallbackInfo ci) {
        Player entity = (Player) (Object) this;
        entity.getEntityData().define(sphericalPlayerMod$SIZE, KatamariIO.DEFAULT_BALL_SIZE);
        entity.getEntityData().define(sphericalPlayerMod$RENDER_SIZE, KatamariIO.DEFAULT_BALL_SIZE);
        entity.getEntityData().define(sphericalPlayerMod$FLAG, true);
        CompoundTag quaternionTag = new CompoundTag();
        quaternionTag.putFloat("x", 0f);
        quaternionTag.putFloat("y", 0f);
        quaternionTag.putFloat("z", 0f);
        quaternionTag.putFloat("w", 1f);
        entity.getEntityData().define(sphericalPlayerMod$QUATERNION, quaternionTag);
        entity.getEntityData().define(RESTITUTION_COEFFICIENT, 0.55f);

        CompoundTag positionTag = new CompoundTag();
        positionTag.putFloat("x", 0);
        positionTag.putFloat("y", 0);
        positionTag.putFloat("z", 0);
        entity.getEntityData().define(CURRENT_POSITION, positionTag);

        entity.getEntityData().define(SPHERICAL_PLAYER_POSITION_LIST, new ArrayList<>());
        entity.getEntityData().define(SPHERICAL_PLAYER_QUATERNION_LIST, new ArrayList<>());
        entity.getEntityData().define(SPHERICAL_PLAYER_BLOCK_LIST, new ArrayList<>());
        entity.getEntityData().define(SPHERICAL_PLAYER_BLOCK_COUNT, 0);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeAdditional(CompoundTag compound, CallbackInfo ci) {
        Player entity = (Player) (Object) this;
        SynchedEntityData dataManager = entity.getEntityData();
        compound.putFloat("SPM_Size", dataManager.get(sphericalPlayerMod$SIZE));
        compound.putFloat("SPM_RenderSize", dataManager.get(sphericalPlayerMod$RENDER_SIZE));
        compound.putBoolean("SPM_isBall", dataManager.get(sphericalPlayerMod$FLAG));
        compound.put("SPM_Quaternion", dataManager.get(sphericalPlayerMod$QUATERNION));
        compound.putFloat("SPM_RESTITUTION", dataManager.get(RESTITUTION_COEFFICIENT));
        compound.put("SPM_POSITION", dataManager.get(CURRENT_POSITION));
        compound.putInt("SPM_BlockCount", dataManager.get(SPHERICAL_PLAYER_BLOCK_COUNT));

        List<Block> blocks = dataManager.get(SPHERICAL_PLAYER_BLOCK_LIST);
        List<Vec3> positions = dataManager.get(SPHERICAL_PLAYER_POSITION_LIST);
        List<Quaternionf> quaternions = dataManager.get(SPHERICAL_PLAYER_QUATERNION_LIST);
        int size = Math.min(blocks.size(), Math.min(positions.size(), quaternions.size()));
        ListTag attachedList = new ListTag();
        for (int i = 0; i < size; i++) {
            Block block = blocks.get(i);
            Vec3 pos = positions.get(i);
            Quaternionf quat = quaternions.get(i);
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null) {
                continue;
            }

            CompoundTag entry = new CompoundTag();
            entry.putString("block", key.toString());
            entry.putDouble("x", pos.x);
            entry.putDouble("y", pos.y);
            entry.putDouble("z", pos.z);
            entry.putFloat("qx", quat.x);
            entry.putFloat("qy", quat.y);
            entry.putFloat("qz", quat.z);
            entry.putFloat("qw", quat.w);
            attachedList.add(entry);
        }
        compound.put(SPM_ATTACHED_BLOCKS, attachedList);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readAdditional(CompoundTag compound, CallbackInfo ci) {
        Player entity = (Player) (Object) this;
        SynchedEntityData dataManager = entity.getEntityData();

        if (compound.contains("SPM_Size")) {
            katamariIO$setSize(compound.getFloat("SPM_Size"));
        }
        if (compound.contains("SPM_RenderSize")) {
            katamariIO$setRenderSize(compound.getFloat("SPM_RenderSize"));
        } else if (compound.contains("SPM_Size")) {
            katamariIO$setRenderSize(compound.getFloat("SPM_Size"));
        }
        if (compound.contains("SPM_RESTITUTION")) {
            katamariIO$setRestitutionCoefficient(compound.getFloat("SPM_RESTITUTION"));
        }
        if (compound.contains("SPM_isBall")) {
            katamariIO$setFlag(compound.getBoolean("SPM_isBall"));
        }
        if (compound.contains("SPM_Quaternion")) {
            dataManager.set(sphericalPlayerMod$QUATERNION, compound.getCompound("SPM_Quaternion"));
        }
        if (compound.contains("SPM_POSITION")) {
            dataManager.set(CURRENT_POSITION, compound.getCompound("SPM_POSITION"));
        }
        if (compound.contains("SPM_BlockCount")) {
            dataManager.set(SPHERICAL_PLAYER_BLOCK_COUNT, compound.getInt("SPM_BlockCount"));
        } else {
            dataManager.set(SPHERICAL_PLAYER_BLOCK_COUNT, dataManager.get(SPHERICAL_PLAYER_BLOCK_LIST).size());
        }

        if (compound.contains(SPM_ATTACHED_BLOCKS, Tag.TAG_LIST)) {
            ListTag attachedList = compound.getList(SPM_ATTACHED_BLOCKS, Tag.TAG_COMPOUND);
            List<Block> blocks = new ArrayList<>();
            List<Vec3> positions = new ArrayList<>();
            List<Quaternionf> quaternions = new ArrayList<>();

            for (int i = 0; i < attachedList.size(); i++) {
                CompoundTag entry = attachedList.getCompound(i);
                ResourceLocation key = ResourceLocation.tryParse(entry.getString("block"));
                if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
                    continue;
                }
                Block block = BuiltInRegistries.BLOCK.get(key);
                blocks.add(block);
                positions.add(new Vec3(entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z")));
                quaternions.add(new Quaternionf(
                        entry.getFloat("qx"),
                        entry.getFloat("qy"),
                        entry.getFloat("qz"),
                        entry.getFloat("qw")
                ));
            }

            dataManager.set(SPHERICAL_PLAYER_BLOCK_LIST, blocks, true);
            dataManager.set(SPHERICAL_PLAYER_POSITION_LIST, positions, true);
            dataManager.set(SPHERICAL_PLAYER_QUATERNION_LIST, quaternions, true);
            dataManager.set(SPHERICAL_PLAYER_BLOCK_COUNT, blocks.size());
            katamariIO$binRadiiDirty = true;
        }
    }

    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    public void getStandingEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (katamariIO$getFlag()) {
            cir.setReturnValue(0.85F * katamariIO$getSize());
        }
    }

    @Override
    public void katamariIO$setSize(float size) {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(sphericalPlayerMod$SIZE, size);
        katamariIO$binRadiiDirty = true;
        entity.refreshDimensions();
    }

    @Override
    public float katamariIO$getSize() {
        return ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$SIZE);
    }

    @Override
    public void katamariIO$setCollisionSize(float size) {
        katamariIO$setSize(size);
    }

    @Override
    public float katamariIO$getCollisionSize() {
        return katamariIO$getSize();
    }

    @Override
    public void katamariIO$setRenderSize(float size) {
        ((Player) (Object) this).getEntityData().set(sphericalPlayerMod$RENDER_SIZE, size);
    }

    @Override
    public float katamariIO$getRenderSize() {
        return ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$RENDER_SIZE);
    }

    @Override
    public void katamariIO$setRestitutionCoefficient(float value) {
        ((Player) (Object) this).getEntityData().set(RESTITUTION_COEFFICIENT, value);
    }

    @Override
    public float katamariIO$getRestitutionCoefficient() {
        return ((Player) (Object) this).getEntityData().get(RESTITUTION_COEFFICIENT);
    }

    @Override
    public void katamariIO$setFlag(boolean flag) {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(sphericalPlayerMod$FLAG, flag);
        katamariIO$binRadiiDirty = true;
        entity.refreshDimensions();
    }

    @Override
    public void katamariIO$setFlagAndSizeAndRestitution(boolean flag, float collisionSize, float renderSize, float value) {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(sphericalPlayerMod$FLAG, flag);
        entity.getEntityData().set(sphericalPlayerMod$SIZE, collisionSize);
        entity.getEntityData().set(sphericalPlayerMod$RENDER_SIZE, renderSize);
        entity.getEntityData().set(RESTITUTION_COEFFICIENT, value);
        katamariIO$binRadiiDirty = true;
        entity.refreshDimensions();
    }

    @Override
    public boolean katamariIO$getFlag() {
        return ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$FLAG);
    }

    @Override
    public void katamariIO$setQuaternion(Quaternionf quaternion) {
        Player entity = (Player) (Object) this;
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("x", quaternion.x());
        nbt.putFloat("y", quaternion.y());
        nbt.putFloat("z", quaternion.z());
        nbt.putFloat("w", quaternion.w());
        entity.getEntityData().set(sphericalPlayerMod$QUATERNION, nbt);
    }

    @Override
    public Quaternionf katamariIO$getQuaternion() {
        CompoundTag quaternionNbt = ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$QUATERNION);
        return katamariIO$getValidQuaternion(new Quaternionf(
                quaternionNbt.getFloat("x"),
                quaternionNbt.getFloat("y"),
                quaternionNbt.getFloat("z"),
                quaternionNbt.getFloat("w")
        ));
    }

    @Unique
    private static Quaternionf katamariIO$getValidQuaternion(Quaternionf quaternion) {
        if (quaternion.x() == 0 && quaternion.y() == 0 && quaternion.z() == 0 && quaternion.w() == 0) {
            return new Quaternionf(0, 0, 0, 1);
        }
        return quaternion;
    }

    @Override
    public void katamariIO$setCurrentPosition(Vec3 pos) {
        Player entity = (Player) (Object) this;
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("x", (float) pos.x());
        nbt.putFloat("y", (float) pos.y());
        nbt.putFloat("z", (float) pos.z());
        entity.getEntityData().set(CURRENT_POSITION, nbt);
    }

    @Override
    public Vec3 katamariIO$getCurrentPosition() {
        CompoundTag compoundNbt = ((Player) (Object) this).getEntityData().get(CURRENT_POSITION);
        return new Vec3(
                compoundNbt.getFloat("x"),
                compoundNbt.getFloat("y"),
                compoundNbt.getFloat("z")
        );
    }

    @Inject(method = "getMyRidingOffset", at = @At("HEAD"), cancellable = true)
    public void getYOffset(CallbackInfoReturnable<Double> cir) {
        if (katamariIO$getFlag()) {
            cir.setReturnValue(0.15);
        }
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (katamariIO$getFlag()) {
            cir.setReturnValue(EntityDimensions.scalable(katamariIO$getSize(), katamariIO$getSize()));
            cir.cancel();
        }
    }

    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    protected void maybeBackOffFromEdge(Vec3 movement, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
        if (katamariIO$getFlag()) {
            cir.setReturnValue(movement);
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void katamariIO$causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Player entity = (Player) (Object) this;
        if (katamariIO$getFlag()) {
            KatamariMovement.bouncePlayer(entity, fallDistance, katamariIO$getRestitutionCoefficient());
            cir.setReturnValue(false);
        }
    }

    @Override
    public void katamariIO$addBlock(Block block, Quaternionf quaternionf, Vec3 vec3) {
        Player entity = (Player) (Object) this;
        List<Block> blocks = entity.getEntityData().get(SPHERICAL_PLAYER_BLOCK_LIST);
        List<Quaternionf> quaternionfs = entity.getEntityData().get(SPHERICAL_PLAYER_QUATERNION_LIST);
        List<Vec3> vec3s = entity.getEntityData().get(SPHERICAL_PLAYER_POSITION_LIST);
        blocks.add(block);
        quaternionfs.add(quaternionf);
        vec3s.add(vec3);
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_LIST, blocks, true);
        entity.getEntityData().set(SPHERICAL_PLAYER_POSITION_LIST, vec3s, true);
        entity.getEntityData().set(SPHERICAL_PLAYER_QUATERNION_LIST, quaternionfs, true);
        int currentCount = entity.getEntityData().get(SPHERICAL_PLAYER_BLOCK_COUNT);
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_COUNT, currentCount + 1);
        katamariIO$applyBlockRadiusIncremental(entity, vec3);
        katamariIO$pruneDataParameterByRadius(entity);
    }

    @Override
    public int katamariIO$getAttachedBlockCount() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_BLOCK_COUNT);
    }

    @Override
    public void katamariIO$clearAttachedBlocks() {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_LIST, new ArrayList<>(), true);
        entity.getEntityData().set(SPHERICAL_PLAYER_POSITION_LIST, new ArrayList<>(), true);
        entity.getEntityData().set(SPHERICAL_PLAYER_QUATERNION_LIST, new ArrayList<>(), true);
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_COUNT, 0);
        katamariIO$binRadiiDirty = true;
    }

    @Override
    public List<Vec3> katamariIO$getSphericalPlayerPositions() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_POSITION_LIST);
    }

    @Override
    public List<Quaternionf> katamariIO$getSphericalPlayerQuaternions() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_QUATERNION_LIST);
    }

    @Override
    public List<Block> katamariIO$getSphericalPlayerBlocks() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_BLOCK_LIST);
    }

    @Unique
    private void katamariIO$applyBlockRadiusIncremental(Player entity, Vec3 vec3) {
        katamariIO$ensureBinRadiiInitialized(entity);

        float distance = (float) vec3.length();
        float blockOuterRadius = distance + KATAMARI_BLOCK_EXTENT;
        if (distance >= 1.0E-6f) {
            float nx = (float) (vec3.x / distance);
            float ny = (float) (vec3.y / distance);
            float nz = (float) (vec3.z / distance);
            int index = katamariIO$getAngularBinIndex(nx, ny, nz);
            if (blockOuterRadius > katamariIO$binMaxRadii[index]) {
                katamariIO$binMaxRadii[index] = blockOuterRadius;
            }
        }

        float collisionRadius = katamariIO$binMaxRadii[0];
        for (int i = 1; i < katamariIO$binMaxRadii.length; i++) {
            if (katamariIO$binMaxRadii[i] < collisionRadius) {
                collisionRadius = katamariIO$binMaxRadii[i];
            }
        }

        float newCollisionSize = collisionRadius * 2.0f;
        boolean collisionChanged = Math.abs(entity.getEntityData().get(sphericalPlayerMod$SIZE) - newCollisionSize) > 1.0E-4f;
        entity.getEntityData().set(sphericalPlayerMod$SIZE, newCollisionSize);
        if (collisionChanged) {
            entity.refreshDimensions();
        }
    }

    @Unique
    private void katamariIO$ensureBinRadiiInitialized(Player entity) {
        if (!katamariIO$binRadiiDirty && katamariIO$binMaxRadii != null) {
            return;
        }

        float baseCollisionRadius = Math.max(0.01f, entity.getEntityData().get(sphericalPlayerMod$SIZE) / 2.0f);
        int binCount = KATAMARI_COLLISION_LAT_BINS * KATAMARI_COLLISION_LON_BINS;
        katamariIO$binMaxRadii = new float[binCount];
        for (int i = 0; i < binCount; i++) {
            katamariIO$binMaxRadii[i] = baseCollisionRadius;
        }

        List<Vec3> vec3s = entity.getEntityData().get(SPHERICAL_PLAYER_POSITION_LIST);
        for (Vec3 offset : vec3s) {
            float distance = (float) offset.length();
            if (distance < 1.0E-6f) {
                continue;
            }

            float blockOuterRadius = distance + KATAMARI_BLOCK_EXTENT;
            float nx = (float) (offset.x / distance);
            float ny = (float) (offset.y / distance);
            float nz = (float) (offset.z / distance);
            int index = katamariIO$getAngularBinIndex(nx, ny, nz);
            if (blockOuterRadius > katamariIO$binMaxRadii[index]) {
                katamariIO$binMaxRadii[index] = blockOuterRadius;
            }
        }

        katamariIO$binRadiiDirty = false;
    }

    @Unique
    private static int katamariIO$getAngularBinIndex(float nx, float ny, float nz) {
        float clampedY = Math.max(-1.0f, Math.min(1.0f, ny));
        double lat = (Math.asin(clampedY) + Math.PI / 2.0) / Math.PI;
        int latIndex = (int) Math.floor(lat * KATAMARI_COLLISION_LAT_BINS);
        if (latIndex < 0) {
            latIndex = 0;
        } else if (latIndex >= KATAMARI_COLLISION_LAT_BINS) {
            latIndex = KATAMARI_COLLISION_LAT_BINS - 1;
        }

        double lon = (Math.atan2(nz, nx) + Math.PI) / (2.0 * Math.PI);
        int lonIndex = (int) Math.floor(lon * KATAMARI_COLLISION_LON_BINS);
        if (lonIndex < 0) {
            lonIndex = 0;
        } else if (lonIndex >= KATAMARI_COLLISION_LON_BINS) {
            lonIndex = KATAMARI_COLLISION_LON_BINS - 1;
        }

        return latIndex * KATAMARI_COLLISION_LON_BINS + lonIndex;
    }

    @Unique
    private void katamariIO$pruneDataParameterByRadius(Player entity) {
        float collisionRadius = entity.getEntityData().get(sphericalPlayerMod$SIZE) / 2.0f;
        float cutoffRadius = Math.max(0.0f, collisionRadius - KATAMARI_DP_CUTOFF_OFFSET);

        List<Block> blocks = entity.getEntityData().get(SPHERICAL_PLAYER_BLOCK_LIST);
        List<Quaternionf> quaternions = entity.getEntityData().get(SPHERICAL_PLAYER_QUATERNION_LIST);
        List<Vec3> positions = entity.getEntityData().get(SPHERICAL_PLAYER_POSITION_LIST);
        int size = Math.min(blocks.size(), Math.min(quaternions.size(), positions.size()));

        List<Block> keptBlocks = new ArrayList<>(size);
        List<Quaternionf> keptQuaternions = new ArrayList<>(size);
        List<Vec3> keptPositions = new ArrayList<>(size);
        boolean changed = false;

        for (int i = 0; i < size; i++) {
            Vec3 pos = positions.get(i);
            double blockOuterRadius = pos.length() + KATAMARI_BLOCK_EXTENT;
            if (blockOuterRadius < cutoffRadius) {
                changed = true;
                continue;
            }
            keptBlocks.add(blocks.get(i));
            keptQuaternions.add(quaternions.get(i));
            keptPositions.add(pos);
        }

        if (!changed) {
            return;
        }

        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_LIST, keptBlocks, true);
        entity.getEntityData().set(SPHERICAL_PLAYER_QUATERNION_LIST, keptQuaternions, true);
        entity.getEntityData().set(SPHERICAL_PLAYER_POSITION_LIST, keptPositions, true);
    }
}
