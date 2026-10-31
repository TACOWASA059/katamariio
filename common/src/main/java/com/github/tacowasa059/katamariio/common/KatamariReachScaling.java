package com.github.tacowasa059.katamariio.common;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Scales the player's interaction reach (block and entity) with the katamari's size, so a large
 * ball can still break/place blocks and attack entities near its surface. The reach is recomputed
 * from the current size every server tick, so it grows as the ball grows and returns to vanilla the
 * moment the size is reset (resetBlocks / turning the ball off both restore DEFAULT_BALL_SIZE).
 * <p>
 * Since 1.20.5 the block/entity interaction ranges are vanilla attributes that are synced to the
 * client and used by both the client pick ray and the server-side distance checks, so a single
 * server-side modifier covers every loader.
 */
public final class KatamariReachScaling {
    private static final ResourceLocation BLOCK_REACH_ID = ResourceLocation.fromNamespaceAndPath(KatamariIO.MODID, "block_reach");
    private static final ResourceLocation ENTITY_REACH_ID = ResourceLocation.fromNamespaceAndPath(KatamariIO.MODID, "entity_reach");

    private KatamariReachScaling() {
    }

    public static void tick(Player player) {
        ICustomPlayerData data = (ICustomPlayerData) player;
        boolean isBall = data.katamariIO$getFlag();
        double targetReach = isBall ? KatamariReach.reachFor(data.katamariIO$getSize()) : 0.0;

        applyReach(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID, isBall, targetReach);
        applyReach(player, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH_ID, isBall, targetReach);
    }

    private static void applyReach(Player player, Holder<Attribute> attribute, ResourceLocation id, boolean isBall, double targetReach) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        double bonus = isBall ? Math.max(0.0, targetReach - instance.getBaseValue()) : 0.0;
        AttributeModifier existing = instance.getModifier(id);
        if (bonus <= 0.0) {
            if (existing != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (existing != null) {
            if (Math.abs(existing.amount() - bonus) < 1.0E-4) {
                return;
            }
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_VALUE));
    }
}
