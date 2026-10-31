package com.github.tacowasa059.katamariio.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class KatamariConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config", "katamariio.json");

    private static KatamariConfig instance = new KatamariConfig();

    public enum Preset {
        FAST,
        BALANCED,
        STRICT
    }

    public enum FilterMode {
        NONE,
        BLACKLIST,
        WHITELIST
    }

    public boolean attachBlocks = true;     // true: pick up & attach blocks / false: do not destroy or attach blocks (terrain protection)
    public boolean attachEntities = true;    // true: living entities stick and rotate on katamari / false: entities do not attach
    public boolean renderAttachedBlocks = true; // true: render attached blocks / false: render skin ball only
    public float maxSize = -1.0f;           // -1.0f or <= 0 for unlimited
    public int maxRemovalsPerTick = 48;     // -1 for unlimited
    public boolean pruneInnerBlocks = true; // prune invisible inner blocks to optimize rendering
    public boolean showPlayerIndicator = true; // show head UI + arrow marker above players in multiplayer
    public boolean disableFlyCheck = true;     // disable server fly-kick check for rolling players
    public int maxAttachedEntities = -1;       // maximum simultaneous attached entities (-1 for unlimited)
    public boolean attachBalls = false;        // true: allow ball-mode players to attach each other / false: balls do not attach
    public boolean attachWhileRiding = false;  // true: allow attaching blocks/entities while riding / false: prevent attachment while riding
    public boolean allowSelfDismount = false;  // true: sneaking dismounts and pops passenger outward from katamari / false: cannot self-dismount
    public int collisionBins = 64;             // number of fibonacci sphere bins for collision grid (recommended: 32-128, default: 64)

    public int getCollisionBins() {
        return Math.max(8, Math.min(512, collisionBins));
    }

    // Block filter settings
    public FilterMode blockFilterMode = FilterMode.BLACKLIST;
    public java.util.List<String> blockFilterList = new java.util.ArrayList<>(java.util.List.of(
            "minecraft:bedrock",
            "minecraft:barrier",
            "minecraft:end_portal_frame",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:jigsaw"
    ));
    public boolean protectIndestructibleBlocks = true; // protect blocks with destroy speed < 0 (e.g. bedrock)

    // Entity filter settings
    public FilterMode entityFilterMode = FilterMode.BLACKLIST;
    public java.util.List<String> entityFilterList = new java.util.ArrayList<>(java.util.List.of(
            "minecraft:item",
            "minecraft:experience_orb"
    ));
    public boolean protectBosses = true;            // protect bosses (dragon, wither, warden, etc.)
    public boolean protectNamedEntities = true;     // protect named mobs (with name tag)
    public boolean protectTamedEntities = true;      // protect tamed pets
    public boolean protectPlayers = true;            // protect regular players

    public static KatamariConfig get() {
        return instance;
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            KatamariConfig loaded = GSON.fromJson(reader, KatamariConfig.class);
            if (loaded != null) {
                if (loaded.blockFilterList == null) loaded.blockFilterList = new java.util.ArrayList<>();
                if (loaded.entityFilterList == null) loaded.entityFilterList = new java.util.ArrayList<>();
                instance = loaded;
                LOGGER.info("[KatamariIO] Config loaded successfully.");
            }
        } catch (Exception e) {
            LOGGER.error("[KatamariIO] Failed to load config file; using defaults.", e);
        }
    }

    public static String toJson() {
        return GSON.toJson(instance);
    }

    public static void applySyncedJson(String json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            KatamariConfig loaded = GSON.fromJson(json, KatamariConfig.class);
            if (loaded != null) {
                if (loaded.blockFilterList == null) loaded.blockFilterList = new java.util.ArrayList<>();
                if (loaded.entityFilterList == null) loaded.entityFilterList = new java.util.ArrayList<>();
                instance = loaded;
                LOGGER.info("[KatamariIO] Synced config applied from server.");
            }
        } catch (Exception e) {
            LOGGER.error("[KatamariIO] Failed to apply synced config.", e);
        }
    }

    public static void save() {
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(instance, writer);
                LOGGER.info("[KatamariIO] Config saved successfully.");
            }
        } catch (IOException e) {
            LOGGER.error("[KatamariIO] Failed to save config file.", e);
        }
    }

    public void applyPreset(Preset preset) {
        switch (preset) {
            case FAST -> {
                this.attachBlocks = true;
                this.attachEntities = false;
                this.renderAttachedBlocks = false;
                this.maxRemovalsPerTick = 24;
                this.collisionBins = 32;
                this.pruneInnerBlocks = true;
            }
            case BALANCED -> {
                this.attachBlocks = true;
                this.attachEntities = false;
                this.renderAttachedBlocks = true;
                this.maxRemovalsPerTick = 48;
                this.collisionBins = 64;
                this.pruneInnerBlocks = true;
            }
            case STRICT -> {
                this.attachBlocks = true;
                this.attachEntities = true;
                this.renderAttachedBlocks = true;
                this.maxRemovalsPerTick = -1;
                this.collisionBins = 128;
                this.pruneInnerBlocks = false;
            }
        }
        save();
    }

    // Cache fields (transient, excluded from Gson serialization)
    private transient int cachedBlockFilterHash = -1;
    private transient java.util.Set<net.minecraft.world.level.block.Block> cachedFilterBlocks = null;
    private transient java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block>> cachedFilterBlockTags = null;

    private transient int cachedEntityFilterHash = -1;
    private transient java.util.Set<net.minecraft.world.entity.EntityType<?>> cachedFilterEntities = null;
    private transient java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>>> cachedFilterEntityTags = null;

    private synchronized void updateBlockFilterCache() {
        int currentHash = blockFilterList.hashCode();
        if (cachedFilterBlocks != null && cachedBlockFilterHash == currentHash) {
            return;
        }
        java.util.Set<net.minecraft.world.level.block.Block> blocks = new java.util.HashSet<>();
        java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block>> tags = new java.util.ArrayList<>();
        for (String entry : blockFilterList) {
            if (entry.startsWith("#")) {
                net.minecraft.resources.ResourceLocation tagId = net.minecraft.resources.ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null) {
                    tags.add(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagId));
                }
            } else {
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(entry);
                if (id != null) {
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(id).ifPresent(blocks::add);
                }
            }
        }
        this.cachedFilterBlocks = blocks;
        this.cachedFilterBlockTags = tags;
        this.cachedBlockFilterHash = currentHash;
    }

    private synchronized void updateEntityFilterCache() {
        int currentHash = entityFilterList.hashCode();
        if (cachedFilterEntities != null && cachedEntityFilterHash == currentHash) {
            return;
        }
        java.util.Set<net.minecraft.world.entity.EntityType<?>> entities = new java.util.HashSet<>();
        java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>>> tags = new java.util.ArrayList<>();
        for (String entry : entityFilterList) {
            if (entry.startsWith("#")) {
                net.minecraft.resources.ResourceLocation tagId = net.minecraft.resources.ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null) {
                    tags.add(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, tagId));
                }
            } else {
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(entry);
                if (id != null) {
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(entities::add);
                }
            }
        }
        this.cachedFilterEntities = entities;
        this.cachedFilterEntityTags = tags;
        this.cachedEntityFilterHash = currentHash;
    }

    /**
     * Determines whether the specified block is allowed to be attached.
     */
    public boolean isBlockAllowed(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (protectIndestructibleBlocks && state.getBlock().defaultDestroyTime() < 0.0f) {
            return false;
        }
        if (blockFilterMode == FilterMode.NONE) {
            return true;
        }
        updateBlockFilterCache();
        boolean inList = cachedFilterBlocks.contains(state.getBlock());
        if (!inList && !cachedFilterBlockTags.isEmpty()) {
            for (net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tagKey : cachedFilterBlockTags) {
                if (state.is(tagKey)) {
                    inList = true;
                    break;
                }
            }
        }
        return (blockFilterMode == FilterMode.WHITELIST) == inList;
    }

    /**
     * Determines whether the specified entity is allowed to be attached.
     */
    public boolean isEntityAllowed(net.minecraft.world.entity.Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            boolean isOtherBall = ((com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData) player).katamariIO$getFlag();
            if (isOtherBall) {
                if (!attachBalls) {
                    return false;
                }
            } else if (protectPlayers) {
                return false;
            }
        }
        if (protectNamedEntities && entity.hasCustomName()) {
            return false;
        }
        if (protectTamedEntities) {
            if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame()) {
                return false;
            }
            if (entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()) {
                return false;
            }
        }
        if (protectBosses) {
            net.minecraft.world.entity.EntityType<?> type = entity.getType();
            if (type == net.minecraft.world.entity.EntityType.WITHER
                    || type == net.minecraft.world.entity.EntityType.ENDER_DRAGON
                    || type == net.minecraft.world.entity.EntityType.WARDEN
                    || type == net.minecraft.world.entity.EntityType.ELDER_GUARDIAN
                    || !entity.canUsePortal(false)) {
                return false;
            }
        }
        if (entityFilterMode == FilterMode.NONE) {
            return true;
        }
        updateEntityFilterCache();
        boolean inList = cachedFilterEntities.contains(entity.getType());
        if (!inList && !cachedFilterEntityTags.isEmpty()) {
            for (net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> tagKey : cachedFilterEntityTags) {
                if (entity.getType().is(tagKey)) {
                    inList = true;
                    break;
                }
            }
        }
        return (entityFilterMode == FilterMode.WHITELIST) == inList;
    }
}
