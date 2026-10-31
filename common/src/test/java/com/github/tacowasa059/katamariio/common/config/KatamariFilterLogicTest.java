package com.github.tacowasa059.katamariio.common.config;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KatamariFilterLogicTest {

    private KatamariConfig config;

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        config = new KatamariConfig();
    }

    @Test
    @DisplayName("Verify air blocks are always disallowed")
    void testAirDisallowed() {
        assertFalse(config.isBlockAllowed(Blocks.AIR.defaultBlockState()));
        assertFalse(config.isBlockAllowed(Blocks.CAVE_AIR.defaultBlockState()));
        assertFalse(config.isBlockAllowed(Blocks.VOID_AIR.defaultBlockState()));
    }

    @Test
    @DisplayName("Verify indestructible blocks (hardness < 0) like bedrock are protected")
    void testIndestructibleBlocksProtection() {
        config.protectIndestructibleBlocks = true;
        assertFalse(config.isBlockAllowed(Blocks.BEDROCK.defaultBlockState()), "Bedrock must not be breakable");
        assertFalse(config.isBlockAllowed(Blocks.BARRIER.defaultBlockState()), "Barrier must not be breakable");
        assertFalse(config.isBlockAllowed(Blocks.END_PORTAL_FRAME.defaultBlockState()), "End portal frame must not be breakable");

        // Normal stone and dirt should be allowed
        assertTrue(config.isBlockAllowed(Blocks.STONE.defaultBlockState()));
        assertTrue(config.isBlockAllowed(Blocks.DIRT.defaultBlockState()));
    }

    @Test
    @DisplayName("Verify block blacklist functionality")
    void testBlockBlacklist() {
        config.blockFilterMode = KatamariConfig.FilterMode.BLACKLIST;
        config.blockFilterList.clear();
        config.blockFilterList.add("minecraft:diamond_block");

        assertFalse(config.isBlockAllowed(Blocks.DIAMOND_BLOCK.defaultBlockState()), "Blacklisted blocks must be rejected");
        assertTrue(config.isBlockAllowed(Blocks.GOLD_BLOCK.defaultBlockState()), "Non-blacklisted blocks must be allowed");
    }

    @Test
    @DisplayName("Verify block whitelist functionality")
    void testBlockWhitelist() {
        config.blockFilterMode = KatamariConfig.FilterMode.WHITELIST;
        config.blockFilterList.clear();
        config.blockFilterList.add("minecraft:gold_block");

        assertTrue(config.isBlockAllowed(Blocks.GOLD_BLOCK.defaultBlockState()), "Whitelisted blocks must be allowed");
        assertFalse(config.isBlockAllowed(Blocks.DIAMOND_BLOCK.defaultBlockState()), "Non-whitelisted blocks must be rejected");
        assertFalse(config.isBlockAllowed(Blocks.STONE.defaultBlockState()), "Non-whitelisted blocks must be rejected");
    }

    private static void bindTag(net.minecraft.world.level.block.Block block, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag) {
        try {
            java.lang.reflect.Method m = net.minecraft.core.Holder.Reference.class.getDeclaredMethod("bindTags", java.util.Collection.class);
            m.setAccessible(true);
            m.invoke(block.builtInRegistryHolder(), java.util.List.of(tag));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Verify tag-based blacklist functionality (e.g. #minecraft:leaves)")
    void testTagBasedBlacklist() {
        net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> leavesTag =
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "leaves"));
        bindTag(Blocks.OAK_LEAVES, leavesTag);
        bindTag(Blocks.BIRCH_LEAVES, leavesTag);
        bindTag(Blocks.SPRUCE_LEAVES, leavesTag);

        config.blockFilterMode = KatamariConfig.FilterMode.BLACKLIST;
        config.blockFilterList.clear();
        config.blockFilterList.add("#minecraft:leaves");

        // All blocks in leaves tag must be disallowed
        assertFalse(config.isBlockAllowed(Blocks.OAK_LEAVES.defaultBlockState()), "Oak leaves must be rejected by tag");
        assertFalse(config.isBlockAllowed(Blocks.BIRCH_LEAVES.defaultBlockState()), "Birch leaves must be rejected by tag");
        assertFalse(config.isBlockAllowed(Blocks.SPRUCE_LEAVES.defaultBlockState()), "Spruce leaves must be rejected by tag");

        // Non-leaf blocks must still be allowed
        assertTrue(config.isBlockAllowed(Blocks.OAK_LOG.defaultBlockState()), "Logs must be allowed");
        assertTrue(config.isBlockAllowed(Blocks.STONE.defaultBlockState()), "Stone must be allowed");
    }

    @Test
    @DisplayName("Verify tag-based whitelist functionality (e.g. #minecraft:logs)")
    void testTagBasedWhitelist() {
        net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> logsTag =
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "logs"));
        bindTag(Blocks.OAK_LOG, logsTag);
        bindTag(Blocks.BIRCH_LOG, logsTag);

        config.blockFilterMode = KatamariConfig.FilterMode.WHITELIST;
        config.blockFilterList.clear();
        config.blockFilterList.add("#minecraft:logs");

        // Only blocks in logs tag must be allowed
        assertTrue(config.isBlockAllowed(Blocks.OAK_LOG.defaultBlockState()), "Oak log must be allowed");
        assertTrue(config.isBlockAllowed(Blocks.BIRCH_LOG.defaultBlockState()), "Birch log must be allowed");

        // Blocks outside logs tag must be disallowed
        assertFalse(config.isBlockAllowed(Blocks.OAK_LEAVES.defaultBlockState()), "Leaves must be rejected");
        assertFalse(config.isBlockAllowed(Blocks.STONE.defaultBlockState()), "Stone must be rejected");
    }

    @Test
    @DisplayName("Verify all normal blocks allowed in filter NONE mode")
    void testFilterNoneMode() {
        config.blockFilterMode = KatamariConfig.FilterMode.NONE;
        config.protectIndestructibleBlocks = false; // Indestructible block protection also OFF

        assertTrue(config.isBlockAllowed(Blocks.BEDROCK.defaultBlockState()));
        assertTrue(config.isBlockAllowed(Blocks.STONE.defaultBlockState()));
        assertTrue(config.isBlockAllowed(Blocks.OBSIDIAN.defaultBlockState()));
    }

    @Test
    @DisplayName("Verify default entity filter blacklist settings")
    void testEntityFilterDefaults() {
        assertEquals(KatamariConfig.FilterMode.BLACKLIST, config.entityFilterMode);
        assertTrue(config.entityFilterList.contains("minecraft:item"), "Items must be blacklisted by default");
        assertTrue(config.entityFilterList.contains("minecraft:experience_orb"), "Experience orbs must be blacklisted by default");
    }
}
