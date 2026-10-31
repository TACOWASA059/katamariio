package com.github.tacowasa059.katamariio.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KatamariConfigTest {

    private KatamariConfig config;

    @BeforeEach
    void setUp() {
        config = new KatamariConfig();
    }

    @Test
    @DisplayName("Verify default configuration values")
    void testDefaults() {
        assertTrue(config.attachBlocks, "Block attachment is enabled by default");
        assertTrue(config.attachEntities, "Entity attachment is enabled by default");
        assertTrue(config.renderAttachedBlocks, "Attached block rendering is enabled by default");
        assertEquals(-1.0f, config.maxSize, 1e-4f, "Size is unlimited by default (-1.0f)");
        assertEquals(48, config.maxRemovalsPerTick);
        assertEquals(-1, config.maxAttachedEntities, "Default entity attach cap is unlimited (-1)");
        assertTrue(config.pruneInnerBlocks);
        assertTrue(config.showPlayerIndicator);
        assertTrue(config.disableFlyCheck, "Disable fly check is enabled by default");
        assertFalse(config.attachBalls, "attachBalls is disabled by default");
        assertFalse(config.attachWhileRiding, "attachWhileRiding is disabled by default");
        assertFalse(config.allowSelfDismount, "allowSelfDismount is disabled by default");
        assertTrue(config.protectIndestructibleBlocks);
        assertTrue(config.protectBosses);
        assertTrue(config.protectNamedEntities);
        assertTrue(config.protectTamedEntities);
        assertTrue(config.protectPlayers);
        assertEquals(KatamariConfig.FilterMode.BLACKLIST, config.blockFilterMode);
        assertEquals(KatamariConfig.FilterMode.BLACKLIST, config.entityFilterMode);
        assertTrue(config.entityFilterList.contains("minecraft:item"), "Entity filter contains item by default");
        assertTrue(config.entityFilterList.contains("minecraft:experience_orb"), "Entity filter contains experience_orb by default");
    }

    @Test
    @DisplayName("Verify FAST preset application")
    void testApplyPresetFast() {
        config.applyPreset(KatamariConfig.Preset.FAST);
        assertTrue(config.attachBlocks, "Block attachment is enabled in FAST");
        assertFalse(config.attachEntities, "Entity attachment is disabled in FAST");
        assertFalse(config.renderAttachedBlocks, "Block rendering is disabled in FAST");
        assertEquals(24, config.maxRemovalsPerTick);
        assertEquals(32, config.getCollisionBins());
        assertTrue(config.pruneInnerBlocks);
    }

    @Test
    @DisplayName("Verify BALANCED preset application")
    void testApplyPresetBalanced() {
        config.applyPreset(KatamariConfig.Preset.BALANCED);
        assertTrue(config.attachBlocks, "Block attachment is enabled in BALANCED");
        assertFalse(config.attachEntities, "Entity attachment is disabled in BALANCED");
        assertTrue(config.renderAttachedBlocks, "Block rendering is enabled in BALANCED");
        assertEquals(48, config.maxRemovalsPerTick);
        assertEquals(64, config.getCollisionBins());
        assertTrue(config.pruneInnerBlocks);
    }

    @Test
    @DisplayName("Verify STRICT preset application")
    void testApplyPresetStrict() {
        config.applyPreset(KatamariConfig.Preset.STRICT);
        assertTrue(config.attachBlocks, "Block attachment is enabled in STRICT");
        assertTrue(config.attachEntities, "Entity attachment is enabled in STRICT");
        assertTrue(config.renderAttachedBlocks, "Block rendering is enabled in STRICT");
        assertEquals(-1, config.maxRemovalsPerTick, "Removal per tick is unlimited in STRICT");
        assertEquals(128, config.getCollisionBins());
        assertFalse(config.pruneInnerBlocks, "All blocks retained in STRICT");
    }

    @Test
    @DisplayName("Verify filter list blacklist/whitelist switching")
    void testFilterListLogic() {
        config.blockFilterList.clear();
        config.blockFilterList.add("minecraft:obsidian");

        // Blacklist mode
        config.blockFilterMode = KatamariConfig.FilterMode.BLACKLIST;
        assertTrue(config.blockFilterList.contains("minecraft:obsidian"));
        assertFalse(config.blockFilterList.contains("minecraft:dirt"));

        // Whitelist mode
        config.blockFilterMode = KatamariConfig.FilterMode.WHITELIST;
        assertTrue(config.blockFilterList.contains("minecraft:obsidian"));
    }

    @Test
    @DisplayName("Verify isEntityAllowed basic check (reject null or invalid entity)")
    void testIsEntityAllowedBasic() {
        assertFalse(config.isEntityAllowed(null), "null entity must not be attachable");
    }

    @Test
    @DisplayName("Verify maxAttachedEntities and hasCapacity behavior")
    void testHasCapacity() {
        int original = KatamariConfig.get().maxAttachedEntities;
        try {
            KatamariConfig.get().maxAttachedEntities = 16;
            assertTrue(com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.hasCapacity(0));
            assertTrue(com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.hasCapacity(15));
            assertFalse(com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.hasCapacity(16));

            // -1 represents unlimited
            KatamariConfig.get().maxAttachedEntities = -1;
            assertTrue(com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.hasCapacity(16));
            assertTrue(com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher.hasCapacity(100));
        } finally {
            KatamariConfig.get().maxAttachedEntities = original;
        }
    }
}
