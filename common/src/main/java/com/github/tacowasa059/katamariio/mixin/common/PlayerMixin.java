package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.KatamariMovement;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.networks.S2CKatamariDeltaPacket;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    private static final EntityDataAccessor<CompoundTag> sphericalPlayerMod$QUATERNION = SynchedEntityData.defineId(Player.class, ModDataSerializers.COMPOUND_TAG);
    @Unique
    private static final EntityDataAccessor<CompoundTag> CURRENT_POSITION = SynchedEntityData.defineId(Player.class, ModDataSerializers.COMPOUND_TAG);
    @Unique
    private static final EntityDataAccessor<KatamariBlocks> SPHERICAL_PLAYER_BLOCKS = SynchedEntityData.defineId(Player.class, ModDataSerializers.KATAMARI_BLOCKS);
    @Unique
    private static final EntityDataAccessor<Integer> SPHERICAL_PLAYER_BLOCK_COUNT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<CompoundTag> SPHERICAL_PLAYER_ATTACHED_ENTITIES = SynchedEntityData.defineId(Player.class, ModDataSerializers.COMPOUND_TAG);

    @Unique
    private boolean sphericalPlayerMod$initialized = false;
    @Unique
    private Quaternionf sphericalPlayerMod$quaternion = new Quaternionf(0, 0, 0, 1);
    @Unique
    private Quaternionf sphericalPlayerMod$prevQuaternion = new Quaternionf(0, 0, 0, 1);
    @Unique
    private Quaternionf sphericalPlayerMod$lastSentQuaternion = null;
    @Unique
    private Vec3 sphericalPlayerMod$prevClientPos = null;
    @Unique
    private static int katamariIO$cachedFibonacciN = -1;
    @Unique
    private static float[][] katamariIO$cachedFibonacciDirections = null;
    @Unique
    private static final double FIBONACCI_GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0)); // ~2.399963229728653
    @Unique
    private static final float KATAMARI_BLOCK_EXTENT = 0.5f;
    @Unique
    private static final float KATAMARI_PRUNE_MIN_RADIUS = 6.0f;
    @Unique
    private static final int KATAMARI_PRUNE_MIN_LAT_BINS = 12;
    @Unique
    private static final int KATAMARI_PRUNE_MAX_LAT_BINS = 48;
    @Unique
    private static final float KATAMARI_PRUNE_SHELL_THICKNESS = 0.5f;
    @Unique
    private static final int KATAMARI_PRUNE_ADD_INTERVAL = 32;
    @Unique
    private static final String SPM_ATTACHED_BLOCKS = "SPM_AttachedBlocks";
    @Unique
    private static final String SPM_ATTACHED_BLOCKS_V2 = "SPM_AttachedBlocksV2";
    @Unique
    private float[] katamariIO$binMaxRadii = null;
    @Unique
    private boolean katamariIO$binRadiiDirty = true;
    @Unique
    private int katamariIO$addsSincePrune = 0;
    @Unique
    private boolean katamariIO$blocksDirty = false;
    @Unique
    private static final int KATAMARI_KEYFRAME_INTERVAL = 40;
    @Unique
    private final KatamariBlocks katamariIO$pendingAdds = new KatamariBlocks();

    @Inject(method = "tick", at = @At("HEAD"))
    protected void katamariIO$tick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!sphericalPlayerMod$initialized) {
            sphericalPlayerMod$quaternion = katamariIO$getSynchedQuaternion();
            sphericalPlayerMod$prevQuaternion = katamariIO$getValidQuaternion(new Quaternionf(sphericalPlayerMod$quaternion));
            sphericalPlayerMod$prevClientPos = player.position();
            sphericalPlayerMod$initialized = true;
        }

        // Ensure non-riding players never get stuck with noGravity or noPhysics in the air
        if (player.getVehicle() == null && !player.isSpectator()) {
            if (player.isNoGravity() && !player.getAbilities().flying) {
                player.setNoGravity(false);
            }
            if (player.noPhysics) {
                player.noPhysics = false;
            }
        }

        if (player.level().isClientSide()) {
            if (player.isLocalPlayer() && player.getVehicle() == null) {
                Vec3 currentPos = player.position();
                if (sphericalPlayerMod$prevClientPos == null) {
                    sphericalPlayerMod$prevClientPos = currentPos;
                }
                sphericalPlayerMod$prevQuaternion = katamariIO$getValidQuaternion(new Quaternionf(sphericalPlayerMod$quaternion));
                Quaternionf quaternion = QuaternionUtils.getUpdatedQuaternion(currentPos,
                        sphericalPlayerMod$prevClientPos, (ICustomPlayerData) player);
                sphericalPlayerMod$prevClientPos = currentPos;
                if (quaternion == null) {
                    quaternion = sphericalPlayerMod$quaternion;
                }
                sphericalPlayerMod$quaternion = katamariIO$getValidQuaternion(quaternion);

                // Send updated quaternion to server when rolling, ensuring server & other players stay perfectly synced
                if (sphericalPlayerMod$lastSentQuaternion == null ||
                        Math.abs(sphericalPlayerMod$quaternion.x - sphericalPlayerMod$lastSentQuaternion.x) > 1e-4f ||
                        Math.abs(sphericalPlayerMod$quaternion.y - sphericalPlayerMod$lastSentQuaternion.y) > 1e-4f ||
                        Math.abs(sphericalPlayerMod$quaternion.z - sphericalPlayerMod$lastSentQuaternion.z) > 1e-4f ||
                        Math.abs(sphericalPlayerMod$quaternion.w - sphericalPlayerMod$lastSentQuaternion.w) > 1e-4f) {
                    ModNetwork.sendRotationToServer(sphericalPlayerMod$quaternion);
                    sphericalPlayerMod$lastSentQuaternion = new Quaternionf(sphericalPlayerMod$quaternion);
                }
            } else {
                // Remote players or riding players: update from server-synched EntityData
                sphericalPlayerMod$prevQuaternion = katamariIO$getValidQuaternion(new Quaternionf(sphericalPlayerMod$quaternion));
                sphericalPlayerMod$quaternion = katamariIO$getSynchedQuaternion();
            }

            EntityDimensions expectedDimensions = player.getDimensions(player.getPose());
            if (player.tickCount % 5 == 0
                    && (Math.abs(expectedDimensions.width() - (float) player.getBoundingBox().getXsize()) > 1.0E-4f
                    || Math.abs(expectedDimensions.height() - (float) player.getBoundingBox().getYsize()) > 1.0E-4f)) {
                player.refreshDimensions();
            }
        } else {
            // Send only the blocks added this tick (a small delta) to everyone tracking this player,
            // instead of re-serializing the whole store. A copy is sent so clearing is safe regardless
            // of when the loader serializes the packet.
            if (katamariIO$pendingAdds.size() > 0) {
                ModNetwork.sendBlocksDelta((ServerPlayer) player,
                        new S2CKatamariDeltaPacket(player.getId(), new KatamariBlocks(katamariIO$pendingAdds)));
                katamariIO$pendingAdds.clear();
            }
            // Occasional keyframe: resync the full store via entity data so clients stay bounded and
            // any drift is corrected. New trackers still get the current store as their snapshot.
            if (katamariIO$blocksDirty && player.tickCount % KATAMARI_KEYFRAME_INTERVAL == 0) {
                ModNetwork.sendFullSnapshot(null, (ServerPlayer) player);
                player.getEntityData().set(SPHERICAL_PLAYER_BLOCKS, player.getEntityData().get(SPHERICAL_PLAYER_BLOCKS), true);
                katamariIO$blocksDirty = false;
            }
            com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.tickPlayer((ServerPlayer) player);
            com.github.tacowasa059.katamariio.common.KatamariReachScaling.tick(player);
        }
    }

    @Override
    public Quaternionf katamariIO$getInterpolatedQuaternion(float partialTicks) {
        return QuaternionUtils.slerp(sphericalPlayerMod$prevQuaternion, sphericalPlayerMod$quaternion, partialTicks);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    protected void defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(sphericalPlayerMod$SIZE, KatamariIO.DEFAULT_BALL_SIZE);
        builder.define(sphericalPlayerMod$RENDER_SIZE, KatamariIO.DEFAULT_BALL_SIZE);
        builder.define(sphericalPlayerMod$FLAG, true);
        CompoundTag quaternionTag = new CompoundTag();
        quaternionTag.putFloat("x", 0f);
        quaternionTag.putFloat("y", 0f);
        quaternionTag.putFloat("z", 0f);
        quaternionTag.putFloat("w", 1f);
        builder.define(sphericalPlayerMod$QUATERNION, quaternionTag);
        builder.define(RESTITUTION_COEFFICIENT, 0.55f);

        CompoundTag positionTag = new CompoundTag();
        positionTag.putFloat("x", 0);
        positionTag.putFloat("y", 0);
        positionTag.putFloat("z", 0);
        builder.define(CURRENT_POSITION, positionTag);

        builder.define(SPHERICAL_PLAYER_BLOCKS, new KatamariBlocks());
        builder.define(SPHERICAL_PLAYER_BLOCK_COUNT, 0);
        builder.define(SPHERICAL_PLAYER_ATTACHED_ENTITIES, new CompoundTag());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeAdditional(ValueOutput output, CallbackInfo ci) {
        Player entity = (Player) (Object) this;
        SynchedEntityData dataManager = entity.getEntityData();
        output.putFloat("SPM_Size", dataManager.get(sphericalPlayerMod$SIZE));
        output.putFloat("SPM_RenderSize", dataManager.get(sphericalPlayerMod$RENDER_SIZE));
        output.putBoolean("SPM_isBall", dataManager.get(sphericalPlayerMod$FLAG));
        output.store("SPM_Quaternion", CompoundTag.CODEC, dataManager.get(sphericalPlayerMod$QUATERNION));
        output.putFloat("SPM_RESTITUTION", dataManager.get(RESTITUTION_COEFFICIENT));
        output.store("SPM_POSITION", CompoundTag.CODEC, dataManager.get(CURRENT_POSITION));

        int blockCount = dataManager.get(SPHERICAL_PLAYER_BLOCK_COUNT);
        output.putInt("SPM_BlockCount", blockCount);
        output.putInt("KatamariBlockCount", blockCount);

        CompoundTag attachedTag = dataManager.get(SPHERICAL_PLAYER_ATTACHED_ENTITIES);
        int entityCount = attachedTag != null ? attachedTag.keySet().size() : 0;
        output.putInt("SPM_EntityCount", entityCount);
        output.putInt("KatamariEntityCount", entityCount);

        int totalCount = blockCount + entityCount;
        output.putInt("SPM_TotalCount", totalCount);
        output.putInt("KatamariTotalCount", totalCount);

        KatamariBlocks attached = dataManager.get(SPHERICAL_PLAYER_BLOCKS);
        int attachedSize = attached.size();

        // Compact layout instead of one CompoundTag per block: a deduped block palette,
        // int palette-indices, 16-bit positions (shared float scale), and 8-bit quaternions.
        float maxAbs = 0.0f;
        for (int i = 0; i < attachedSize; i++) {
            maxAbs = Math.max(maxAbs,
                    Math.max(Math.abs(attached.posX(i)), Math.max(Math.abs(attached.posY(i)), Math.abs(attached.posZ(i)))));
        }
        float invScale = maxAbs > 0.0f ? 32767.0f / maxAbs : 0.0f;

        ListTag palette = new ListTag();
        Map<Block, Integer> paletteIndex = new HashMap<>();
        int[] blockIds = new int[attachedSize];
        byte[] posBytes = new byte[attachedSize * 6];
        byte[] quatBytes = new byte[attachedSize * 4];
        int kept = 0;
        for (int i = 0; i < attachedSize; i++) {
            Block block = attached.block(i);
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null) {
                continue;
            }
            Integer pi = paletteIndex.get(block);
            if (pi == null) {
                pi = palette.size();
                palette.add(StringTag.valueOf(key.toString()));
                paletteIndex.put(block, pi);
            }

            blockIds[kept] = pi;
            katamariIO$putShort(posBytes, kept * 6, (short) Math.round(attached.posX(i) * invScale));
            katamariIO$putShort(posBytes, kept * 6 + 2, (short) Math.round(attached.posY(i) * invScale));
            katamariIO$putShort(posBytes, kept * 6 + 4, (short) Math.round(attached.posZ(i) * invScale));
            quatBytes[kept * 4] = katamariIO$quantizeQuat(attached.quatX(i));
            quatBytes[kept * 4 + 1] = katamariIO$quantizeQuat(attached.quatY(i));
            quatBytes[kept * 4 + 2] = katamariIO$quantizeQuat(attached.quatZ(i));
            quatBytes[kept * 4 + 3] = katamariIO$quantizeQuat(attached.quatW(i));
            kept++;
        }

        CompoundTag packed = new CompoundTag();
        packed.put("palette", palette);
        packed.putFloat("posScale", maxAbs);
        packed.putIntArray("blocks", kept == attachedSize ? blockIds : Arrays.copyOf(blockIds, kept));
        packed.putByteArray("pos", kept * 6 == posBytes.length ? posBytes : Arrays.copyOf(posBytes, kept * 6));
        packed.putByteArray("quat", kept * 4 == quatBytes.length ? quatBytes : Arrays.copyOf(quatBytes, kept * 4));
        output.store(SPM_ATTACHED_BLOCKS_V2, CompoundTag.CODEC, packed);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readAdditional(ValueInput input, CallbackInfo ci) {
        Player entity = (Player) (Object) this;
        SynchedEntityData dataManager = entity.getEntityData();
        if (entity.getVehicle() == null) {
            entity.setNoGravity(false);
            entity.noPhysics = false;
        }

        float size = input.getFloatOr("SPM_Size", KatamariIO.DEFAULT_BALL_SIZE);
        katamariIO$setSize(size);
        katamariIO$setRenderSize(input.getFloatOr("SPM_RenderSize", size));
        katamariIO$setRestitutionCoefficient(input.getFloatOr("SPM_RESTITUTION", 0.55f));
        katamariIO$setFlag(input.getBooleanOr("SPM_isBall", true));

        int defaultBlockCount = dataManager.get(SPHERICAL_PLAYER_BLOCKS).size();
        int blockCount = input.getIntOr("SPM_BlockCount", input.getIntOr("KatamariBlockCount", defaultBlockCount));
        dataManager.set(SPHERICAL_PLAYER_BLOCK_COUNT, blockCount);

        input.read("SPM_Quaternion", CompoundTag.CODEC).ifPresent(quaternionTag ->
                dataManager.set(sphericalPlayerMod$QUATERNION, quaternionTag));
        input.read("SPM_POSITION", CompoundTag.CODEC).ifPresent(positionTag ->
                dataManager.set(CURRENT_POSITION, positionTag));

        var v2Opt = input.read(SPM_ATTACHED_BLOCKS_V2, CompoundTag.CODEC);
        if (v2Opt.isPresent()) {
            CompoundTag packed = v2Opt.get();
            ListTag palette = packed.getListOrEmpty("palette");
            float scale = packed.getFloatOr("posScale", 0f) / 32767.0f;
            int[] blockIds = packed.getIntArray("blocks").orElse(new int[0]);
            byte[] posBytes = packed.getByteArray("pos").orElse(new byte[0]);
            byte[] quatBytes = packed.getByteArray("quat").orElse(new byte[0]);

            Block[] paletteBlocks = new Block[palette.size()];
            for (int p = 0; p < paletteBlocks.length; p++) {
                Identifier key = Identifier.tryParse(palette.getStringOr(p, ""));
                paletteBlocks[p] = (key != null && BuiltInRegistries.BLOCK.containsKey(key))
                        ? BuiltInRegistries.BLOCK.getValue(key) : null;
            }

            int count = Math.min(blockIds.length, Math.min(posBytes.length / 6, quatBytes.length / 4));
            KatamariBlocks attached = new KatamariBlocks(Math.max(1, count));
            for (int i = 0; i < count; i++) {
                int pi = blockIds[i];
                if (pi < 0 || pi >= paletteBlocks.length || paletteBlocks[pi] == null) {
                    continue;
                }
                attached.add(paletteBlocks[pi],
                        katamariIO$dequantizeQuat(quatBytes[i * 4], quatBytes[i * 4 + 1], quatBytes[i * 4 + 2], quatBytes[i * 4 + 3]),
                        katamariIO$getShort(posBytes, i * 6) * scale,
                        katamariIO$getShort(posBytes, i * 6 + 2) * scale,
                        katamariIO$getShort(posBytes, i * 6 + 4) * scale);
            }

            dataManager.set(SPHERICAL_PLAYER_BLOCKS, attached, true);
            dataManager.set(SPHERICAL_PLAYER_BLOCK_COUNT, attached.size());
            katamariIO$binRadiiDirty = true;
            katamariIO$addsSincePrune = 0;
        } else {
            input.list(SPM_ATTACHED_BLOCKS, CompoundTag.CODEC).ifPresent(attachedList -> {
                KatamariBlocks attached = new KatamariBlocks();
                for (CompoundTag entry : attachedList) {
                    Identifier key = Identifier.tryParse(entry.getStringOr("block", ""));
                    if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
                        continue;
                    }
                    Block block = BuiltInRegistries.BLOCK.getValue(key);
                    if (block != null) {
                        attached.add(block,
                                new Quaternionf(entry.getFloatOr("qx", 0f), entry.getFloatOr("qy", 0f), entry.getFloatOr("qz", 0f), entry.getFloatOr("qw", 1f)),
                                entry.getDoubleOr("x", 0.0), entry.getDoubleOr("y", 0.0), entry.getDoubleOr("z", 0.0));
                    }
                }
                dataManager.set(SPHERICAL_PLAYER_BLOCKS, attached, true);
                dataManager.set(SPHERICAL_PLAYER_BLOCK_COUNT, attached.size());
                katamariIO$binRadiiDirty = true;
                katamariIO$addsSincePrune = 0;
            });
        }

        // Bound the loaded shell immediately (old saves may hold tens of thousands of blocks).
        katamariIO$pruneDataParameterByRadius(entity);
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
    public float katamariIO$getOuterRadius() {
        Player entity = (Player) (Object) this;
        katamariIO$ensureBinRadiiInitialized(entity);

        float maxRadius = entity.getEntityData().get(sphericalPlayerMod$RENDER_SIZE) / 2.0f;
        if (katamariIO$binMaxRadii != null) {
            for (float r : katamariIO$binMaxRadii) {
                if (r > maxRadius) {
                    maxRadius = r;
                }
            }
        }
        return maxRadius;
    }

    @Override
    public float katamariIO$getRadiusInDirection(float nx, float ny, float nz) {
        Player entity = (Player) (Object) this;
        katamariIO$ensureBinRadiiInitialized(entity);
        int numBins = com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().getCollisionBins();
        int index = katamariIO$getFibonacciBinIndex(nx, ny, nz, numBins);
        if (katamariIO$binMaxRadii != null && index >= 0 && index < katamariIO$binMaxRadii.length) {
            return katamariIO$binMaxRadii[index];
        }
        return entity.getEntityData().get(sphericalPlayerMod$RENDER_SIZE) / 2.0f;
    }

    @Override
    public float katamariIO$getOuterSize() {
        return katamariIO$getOuterRadius() * 2.0f;
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
        sphericalPlayerMod$quaternion = katamariIO$getValidQuaternion(quaternion);
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("x", sphericalPlayerMod$quaternion.x());
        nbt.putFloat("y", sphericalPlayerMod$quaternion.y());
        nbt.putFloat("z", sphericalPlayerMod$quaternion.z());
        nbt.putFloat("w", sphericalPlayerMod$quaternion.w());
        entity.getEntityData().set(sphericalPlayerMod$QUATERNION, nbt, true);
    }

    @Override
    public Quaternionf katamariIO$getQuaternion() {
        return katamariIO$getValidQuaternion(sphericalPlayerMod$quaternion);
    }

    @Unique
    private Quaternionf katamariIO$getSynchedQuaternion() {
        CompoundTag quaternionNbt = ((Player) (Object) this).getEntityData().get(sphericalPlayerMod$QUATERNION);
        return katamariIO$getValidQuaternion(new Quaternionf(
                quaternionNbt.getFloatOr("x", 0f),
                quaternionNbt.getFloatOr("y", 0f),
                quaternionNbt.getFloatOr("z", 0f),
                quaternionNbt.getFloatOr("w", 1f)
        ));
    }

    @Unique
    private static Quaternionf katamariIO$getValidQuaternion(Quaternionf quaternion) {
        if (quaternion.x() == 0 && quaternion.y() == 0 && quaternion.z() == 0 && quaternion.w() == 0) {
            return new Quaternionf(0, 0, 0, 1);
        }
        return quaternion;
    }

    @Unique
    private static byte katamariIO$quantizeQuat(float value) {
        float clamped = Math.max(-1.0f, Math.min(1.0f, value));
        return (byte) Math.round(clamped * 127.0f);
    }

    @Unique
    private static Quaternionf katamariIO$dequantizeQuat(byte x, byte y, byte z, byte w) {
        Quaternionf q = new Quaternionf(x / 127.0f, y / 127.0f, z / 127.0f, w / 127.0f);
        if (q.lengthSquared() > 1.0E-6f) {
            q.normalize();
        } else {
            q.set(0.0f, 0.0f, 0.0f, 1.0f);
        }
        return q;
    }

    @Unique
    private static void katamariIO$putShort(byte[] array, int offset, short value) {
        array[offset] = (byte) (value >> 8);
        array[offset + 1] = (byte) value;
    }

    @Unique
    private static short katamariIO$getShort(byte[] array, int offset) {
        return (short) (((array[offset] & 0xFF) << 8) | (array[offset + 1] & 0xFF));
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
                compoundNbt.getFloatOr("x", 0f),
                compoundNbt.getFloatOr("y", 0f),
                compoundNbt.getFloatOr("z", 0f)
        );
    }

    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    protected void maybeBackOffFromEdge(Vec3 movement, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
        if (katamariIO$getFlag()) {
            cir.setReturnValue(movement);
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void katamariIO$causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Player entity = (Player) (Object) this;
        if (katamariIO$getFlag()) {
            KatamariMovement.bouncePlayer(entity, (float) fallDistance, katamariIO$getRestitutionCoefficient());
            cir.setReturnValue(false);
        }
    }

    @Override
    public void katamariIO$addBlock(Block block, Quaternionf quaternionf, Vec3 vec3) {
        katamariIO$addBlock(block, quaternionf, vec3, KATAMARI_BLOCK_EXTENT);
    }

    @Override
    public void katamariIO$addBlock(Block block, Quaternionf quaternionf, Vec3 vec3, float extent) {
        Player entity = (Player) (Object) this;
        KatamariBlocks blocks = entity.getEntityData().get(SPHERICAL_PLAYER_BLOCKS);
        blocks.add(block, quaternionf, vec3.x, vec3.y, vec3.z);
        katamariIO$pendingAdds.add(block, quaternionf, vec3.x, vec3.y, vec3.z);
        katamariIO$blocksDirty = true;
        int currentCount = entity.getEntityData().get(SPHERICAL_PLAYER_BLOCK_COUNT);
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_COUNT, currentCount + 1);
        katamariIO$applyObjectRadiusIncremental(entity, vec3, extent);
        if (++katamariIO$addsSincePrune >= KATAMARI_PRUNE_ADD_INTERVAL) {
            katamariIO$pruneDataParameterByRadius(entity);
        }
    }

    @Override
    public int katamariIO$getAttachedBlockCount() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_BLOCK_COUNT);
    }

    @Override
    public int katamariIO$getAttachedEntityCount() {
        CompoundTag attachedTag = ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_ATTACHED_ENTITIES);
        return attachedTag != null ? attachedTag.keySet().size() : 0;
    }

    @Override
    public int katamariIO$getTotalAttachedCount() {
        return katamariIO$getAttachedBlockCount() + katamariIO$getAttachedEntityCount();
    }

    @Override
    public void katamariIO$clearAttachedBlocks() {
        Player entity = (Player) (Object) this;
        KatamariBlocks emptyBlocks = new KatamariBlocks();
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCKS, emptyBlocks, true);
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_COUNT, 0);
        katamariIO$binRadiiDirty = true;
        katamariIO$binMaxRadii = null;
        katamariIO$addsSincePrune = 0;
        katamariIO$blocksDirty = false;
        katamariIO$ensureBinRadiiInitialized(entity);
    }

    @Override
    public void katamariIO$clearAttachedEntities() {
        Player entity = (Player) (Object) this;
        com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.releaseAll(entity);
        entity.getEntityData().set(SPHERICAL_PLAYER_ATTACHED_ENTITIES, new CompoundTag(), true);
        katamariIO$binRadiiDirty = true;
        katamariIO$binMaxRadii = null;
        katamariIO$ensureBinRadiiInitialized(entity);
    }

    @Override
    public void katamariIO$resetAll() {
        Player entity = (Player) (Object) this;
        com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.releaseAll(entity);
        entity.getEntityData().set(SPHERICAL_PLAYER_ATTACHED_ENTITIES, new CompoundTag(), true);
        KatamariBlocks emptyBlocks = new KatamariBlocks();
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCKS, emptyBlocks, true);
        entity.getEntityData().set(SPHERICAL_PLAYER_BLOCK_COUNT, 0);
        float baseSize = entity.getEntityData().get(sphericalPlayerMod$RENDER_SIZE);
        entity.getEntityData().set(sphericalPlayerMod$SIZE, baseSize);
        entity.refreshDimensions();
        katamariIO$binRadiiDirty = true;
        katamariIO$binMaxRadii = null;
        katamariIO$addsSincePrune = 0;
        katamariIO$blocksDirty = false;
    }

    @Override
    public void katamariIO$invalidateBinRadii() {
        katamariIO$binRadiiDirty = true;
        katamariIO$binMaxRadii = null;
    }

    @Override
    public KatamariBlocks katamariIO$getBlocks() {
        return ((Player) (Object) this).getEntityData().get(SPHERICAL_PLAYER_BLOCKS);
    }

    @Unique
    private static synchronized float[][] katamariIO$getFibonacciDirections(int N) {
        if (katamariIO$cachedFibonacciDirections != null && katamariIO$cachedFibonacciN == N) {
            return katamariIO$cachedFibonacciDirections;
        }
        float[][] dirs = new float[N][3];
        for (int i = 0; i < N; i++) {
            float y = 1.0f - (2.0f * i + 1.0f) / (float) N;
            float r = (float) Math.sqrt(Math.max(0.0f, 1.0f - y * y));
            double theta = i * FIBONACCI_GOLDEN_ANGLE;
            dirs[i][0] = (float) (r * Math.cos(theta));
            dirs[i][1] = y;
            dirs[i][2] = (float) (r * Math.sin(theta));
        }
        katamariIO$cachedFibonacciN = N;
        katamariIO$cachedFibonacciDirections = dirs;
        return dirs;
    }

    @Unique
    private static int katamariIO$getFibonacciBinIndex(float nx, float ny, float nz, int N) {
        float[][] dirs = katamariIO$getFibonacciDirections(N);
        float clampedY = Math.max(-1.0f, Math.min(1.0f, ny));
        int k = Math.round(((1.0f - clampedY) * N - 1.0f) * 0.5f);
        if (k < 0) k = 0;
        if (k >= N) k = N - 1;

        int window = Math.min(16, N / 2);
        int start = Math.max(0, k - window);
        int end = Math.min(dirs.length - 1, k + window);

        int bestIdx = k;
        float bestDot = nx * dirs[k][0] + ny * dirs[k][1] + nz * dirs[k][2];

        for (int i = start; i <= end; i++) {
            if (i == k) continue;
            float dot = nx * dirs[i][0] + ny * dirs[i][1] + nz * dirs[i][2];
            if (dot > bestDot) {
                bestDot = dot;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    @Unique
    private double katamariIO$calcTotalEntityVolume(Player entity) {
        CompoundTag attachedTag = entity.getEntityData().get(SPHERICAL_PLAYER_ATTACHED_ENTITIES);
        if (attachedTag == null || attachedTag.isEmpty()) {
            return 0.0;
        }
        double totalVolume = 0.0;
        for (String key : attachedTag.keySet()) {
            CompoundTag entry = attachedTag.getCompoundOrEmpty(key);
            if (entry.contains("volume")) {
                totalVolume += entry.getFloatOr("volume", 0f);
            } else {
                float extent = entry.getFloatOr("extent", 0.35f);
                totalVolume += extent * extent * extent * 0.5;
            }
        }
        return totalVolume;
    }

    @Unique
    private float katamariIO$computeBaseRadius(Player entity) {
        float baseRenderRadius = Math.max(0.01f, entity.getEntityData().get(sphericalPlayerMod$RENDER_SIZE) / 2.0f);
        double v0 = (4.0 / 3.0) * Math.PI * Math.pow(baseRenderRadius, 3);
        double volumeEntities = katamariIO$calcTotalEntityVolume(entity);
        double totalVolume = v0 + volumeEntities;
        return (float) Math.cbrt((3.0 * totalVolume) / (4.0 * Math.PI));
    }

    @Unique
    private void katamariIO$applyObjectToBins(float nx, float ny, float nz, float distance, float extent, int numBins) {
        float objectOuterRadius = distance + extent;
        float[][] binDirections = katamariIO$getFibonacciDirections(numBins);

        boolean hasCoverage = (extent > 0.05f && distance > 1.0E-4f);
        float cosTheta = 1.0f;
        if (hasCoverage) {
            float centerDist = distance + extent * 0.5f;
            float objectRadius = extent;
            float sinTheta = Math.min(1.0f, objectRadius / Math.max(0.1f, centerDist));
            cosTheta = (float) Math.sqrt(Math.max(0.0f, 1.0f - sinTheta * sinTheta));
        }

        int bestIdx = 0;
        float bestDot = -Float.MAX_VALUE;

        for (int i = 0; i < binDirections.length && i < katamariIO$binMaxRadii.length; i++) {
            float[] dir = binDirections[i];
            float dot = nx * dir[0] + ny * dir[1] + nz * dir[2];
            if (dot > bestDot) {
                bestDot = dot;
                bestIdx = i;
            }
            if (hasCoverage && dot >= cosTheta) {
                if (objectOuterRadius > katamariIO$binMaxRadii[i]) {
                    katamariIO$binMaxRadii[i] = objectOuterRadius;
                }
            }
        }

        if (bestIdx >= 0 && bestIdx < katamariIO$binMaxRadii.length) {
            if (objectOuterRadius > katamariIO$binMaxRadii[bestIdx]) {
                katamariIO$binMaxRadii[bestIdx] = objectOuterRadius;
            }
        }
    }

    @Unique
    private void katamariIO$applyObjectRadiusIncremental(Player entity, Vec3 vec3, float extent) {
        katamariIO$ensureBinRadiiInitialized(entity);
        int numBins = com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().getCollisionBins();

        float baseRadius = katamariIO$computeBaseRadius(entity);

        for (int i = 0; i < katamariIO$binMaxRadii.length; i++) {
            if (katamariIO$binMaxRadii[i] < baseRadius) {
                katamariIO$binMaxRadii[i] = baseRadius;
            }
        }

        float distance = (float) vec3.length();
        if (distance >= 1.0E-6f) {
            float nx = (float) (vec3.x / distance);
            float ny = (float) (vec3.y / distance);
            float nz = (float) (vec3.z / distance);
            katamariIO$applyObjectToBins(nx, ny, nz, distance, extent, numBins);
        }

        float collisionRadius = katamariIO$binMaxRadii[0];
        for (int i = 1; i < katamariIO$binMaxRadii.length; i++) {
            if (katamariIO$binMaxRadii[i] < collisionRadius) {
                collisionRadius = katamariIO$binMaxRadii[i];
            }
        }

        float newCollisionSize = collisionRadius * 2.0f;
        float maxAllowed = com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().maxSize;
        if (maxAllowed > 0.0f && newCollisionSize > maxAllowed) {
            newCollisionSize = maxAllowed;
        }
        boolean collisionChanged = Math.abs(entity.getEntityData().get(sphericalPlayerMod$SIZE) - newCollisionSize) > 1.0E-4f;
        entity.getEntityData().set(sphericalPlayerMod$SIZE, newCollisionSize);
        if (collisionChanged) {
            entity.refreshDimensions();
        }
    }

    @Unique
    private void katamariIO$ensureBinRadiiInitialized(Player entity) {
        int numBins = com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().getCollisionBins();

        if (!katamariIO$binRadiiDirty && katamariIO$binMaxRadii != null && katamariIO$binMaxRadii.length == numBins) {
            return;
        }

        float baseRadius = katamariIO$computeBaseRadius(entity);
        katamariIO$binMaxRadii = new float[numBins];
        for (int i = 0; i < numBins; i++) {
            katamariIO$binMaxRadii[i] = baseRadius;
        }

        KatamariBlocks attached = entity.getEntityData().get(SPHERICAL_PLAYER_BLOCKS);
        if (attached != null) {
            for (int i = 0; i < attached.size(); i++) {
                float distance = (float) attached.length(i);
                if (distance < 1.0E-6f) {
                    continue;
                }

                float nx = attached.posX(i) / distance;
                float ny = attached.posY(i) / distance;
                float nz = attached.posZ(i) / distance;
                katamariIO$applyObjectToBins(nx, ny, nz, distance, KATAMARI_BLOCK_EXTENT, numBins);
            }
        }

        CompoundTag attachedTag = entity.getEntityData().get(SPHERICAL_PLAYER_ATTACHED_ENTITIES);
        if (attachedTag != null && !attachedTag.isEmpty()) {
            for (String key : attachedTag.keySet()) {
                CompoundTag entry = attachedTag.getCompoundOrEmpty(key);
                float x = entry.getFloatOr("x", 0f);
                float y = entry.getFloatOr("y", 0f);
                float z = entry.getFloatOr("z", 0f);
                float extent = entry.getFloatOr("extent", 0.35f);
                float distance = (float) Math.sqrt(x * x + y * y + z * z);
                if (distance < 1.0E-6f) {
                    continue;
                }
                float nx = x / distance;
                float ny = y / distance;
                float nz = z / distance;
                katamariIO$applyObjectToBins(nx, ny, nz, distance, extent, numBins);
            }
        }

        float collisionRadius = katamariIO$binMaxRadii[0];
        for (int i = 1; i < katamariIO$binMaxRadii.length; i++) {
            if (katamariIO$binMaxRadii[i] < collisionRadius) {
                collisionRadius = katamariIO$binMaxRadii[i];
            }
        }

        float newCollisionSize = collisionRadius * 2.0f;
        float maxAllowed = com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().maxSize;
        if (maxAllowed > 0.0f && newCollisionSize > maxAllowed) {
            newCollisionSize = maxAllowed;
        }
        boolean collisionChanged = Math.abs(entity.getEntityData().get(sphericalPlayerMod$SIZE) - newCollisionSize) > 1.0E-4f;
        entity.getEntityData().set(sphericalPlayerMod$SIZE, newCollisionSize);
        if (collisionChanged) {
            entity.refreshDimensions();
        }

        katamariIO$binRadiiDirty = false;
    }

    @Unique
    private void katamariIO$pruneDataParameterByRadius(Player entity) {
        katamariIO$addsSincePrune = 0;

        KatamariBlocks blocks = entity.getEntityData().get(SPHERICAL_PLAYER_BLOCKS);
        if (com.github.tacowasa059.katamariio.common.config.KatamariConfig.get().pruneInnerBlocks
                && blocks.pruneToSurface(KATAMARI_PRUNE_MIN_RADIUS, KATAMARI_PRUNE_MIN_LAT_BINS,
                KATAMARI_PRUNE_MAX_LAT_BINS, KATAMARI_PRUNE_SHELL_THICKNESS)) {
            katamariIO$blocksDirty = true;
        }
    }

    @Override
    public void katamariIO$setPassengerLocalPos(int entityId, org.joml.Vector3f localPos, org.joml.Quaternionf attachQuat, float extent, float volume) {
        Player entity = (Player) (Object) this;
        CompoundTag current = entity.getEntityData().get(SPHERICAL_PLAYER_ATTACHED_ENTITIES);
        CompoundTag copy = current.copy();
        CompoundTag entry = new CompoundTag();
        entry.putFloat("x", localPos.x);
        entry.putFloat("y", localPos.y);
        entry.putFloat("z", localPos.z);
        entry.putFloat("extent", extent);
        entry.putFloat("volume", volume);
        if (attachQuat != null) {
            entry.putFloat("qx", attachQuat.x);
            entry.putFloat("qy", attachQuat.y);
            entry.putFloat("qz", attachQuat.z);
            entry.putFloat("qw", attachQuat.w);
        }
        copy.put(String.valueOf(entityId), entry);
        entity.getEntityData().set(SPHERICAL_PLAYER_ATTACHED_ENTITIES, copy, true);
        katamariIO$applyObjectRadiusIncremental(entity, new Vec3(localPos.x, localPos.y, localPos.z), extent);
    }

    @Override
    public void katamariIO$setPassengerLocalPos(int entityId, org.joml.Vector3f localPos, org.joml.Quaternionf attachQuat, float extent) {
        katamariIO$setPassengerLocalPos(entityId, localPos, attachQuat, extent, (float) (extent * extent * extent * 0.5));
    }

    @Override
    public void katamariIO$setPassengerLocalPos(int entityId, org.joml.Vector3f localPos, org.joml.Quaternionf attachQuat) {
        katamariIO$setPassengerLocalPos(entityId, localPos, attachQuat, 0.35f);
    }

    @Override
    public org.joml.Vector3f katamariIO$getPassengerLocalPos(int entityId) {
        Player entity = (Player) (Object) this;
        CompoundTag tag = entity.getEntityData().get(SPHERICAL_PLAYER_ATTACHED_ENTITIES);
        String key = String.valueOf(entityId);
        if (tag.contains(key)) {
            CompoundTag entry = tag.getCompoundOrEmpty(key);
            return new org.joml.Vector3f(entry.getFloatOr("x", 0f), entry.getFloatOr("y", 0f), entry.getFloatOr("z", 0f));
        }
        return null;
    }

    @Override
    public org.joml.Quaternionf katamariIO$getPassengerAttachQuat(int entityId) {
        Player entity = (Player) (Object) this;
        CompoundTag tag = entity.getEntityData().get(SPHERICAL_PLAYER_ATTACHED_ENTITIES);
        String key = String.valueOf(entityId);
        if (tag.contains(key)) {
            CompoundTag entry = tag.getCompoundOrEmpty(key);
            if (entry.contains("qw")) {
                return new org.joml.Quaternionf(
                        entry.getFloatOr("qx", 0f),
                        entry.getFloatOr("qy", 0f),
                        entry.getFloatOr("qz", 0f),
                        entry.getFloatOr("qw", 1f)
                );
            }
        }
        return null;
    }

    @Override
    public void katamariIO$removePassengerLocalPos(int entityId) {
        Player entity = (Player) (Object) this;
        CompoundTag current = entity.getEntityData().get(SPHERICAL_PLAYER_ATTACHED_ENTITIES);
        if (current.contains(String.valueOf(entityId))) {
            CompoundTag copy = current.copy();
            copy.remove(String.valueOf(entityId));
            entity.getEntityData().set(SPHERICAL_PLAYER_ATTACHED_ENTITIES, copy, true);
            katamariIO$binRadiiDirty = true;
            katamariIO$binMaxRadii = null;
        }
    }

    @Override
    public void katamariIO$clearPassengerLocalPos() {
        Player entity = (Player) (Object) this;
        entity.getEntityData().set(SPHERICAL_PLAYER_ATTACHED_ENTITIES, new CompoundTag(), true);
        katamariIO$binRadiiDirty = true;
        katamariIO$binMaxRadii = null;
    }
}
