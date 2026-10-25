package com.github.tacowasa059.katamariio.forge;

import com.github.tacowasa059.katamariio.common.KatamariReach;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.UUID;

/**
 * Scales the player's interaction reach (block and entity) with the katamari's size, so a large
 * ball can still break/place blocks and attack entities near its surface. The reach is recomputed
 * from the current size every server tick, so it grows as the ball grows and returns to vanilla the
 * moment the size is reset (resetBlocks / turning the ball off both restore DEFAULT_BALL_SIZE).
 * <p>
 * Forge's block/entity reach attributes are syncable, so updating them on the server propagates to
 * the client's pick ray automatically. This is Forge-only; the Fabric loader needs its own path.
 */
public final class ForgeReachScaling {
    private static final UUID BLOCK_REACH_UUID = UUID.fromString("b1a7e0c2-4f3d-4a1e-9c2b-1f2e3d4c5b6a");
    private static final UUID ENTITY_REACH_UUID = UUID.fromString("e2b8f1d3-5a4e-4b2f-8d3c-2a3b4c5d6e7f");

    private ForgeReachScaling() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }
        Player player = event.player;
        ICustomPlayerData data = (ICustomPlayerData) player;
        boolean isBall = data.katamariIO$getFlag();
        double targetReach = isBall ? KatamariReach.reachFor(data.katamariIO$getSize()) : 0.0;

        katamariIO$applyReach(player, ForgeMod.BLOCK_REACH.get(), BLOCK_REACH_UUID, isBall, targetReach);
        katamariIO$applyReach(player, ForgeMod.ENTITY_REACH.get(), ENTITY_REACH_UUID, isBall, targetReach);
    }

    private static void katamariIO$applyReach(Player player, Attribute attribute, UUID uuid, boolean isBall, double targetReach) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        double bonus = isBall ? Math.max(0.0, targetReach - instance.getBaseValue()) : 0.0;
        AttributeModifier existing = instance.getModifier(uuid);
        if (bonus <= 0.0) {
            if (existing != null) {
                instance.removeModifier(uuid);
            }
            return;
        }
        if (existing != null) {
            if (Math.abs(existing.getAmount() - bonus) < 1.0E-4) {
                return;
            }
            instance.removeModifier(uuid);
        }
        instance.addTransientModifier(new AttributeModifier(uuid, "KatamariIO reach", bonus, AttributeModifier.Operation.ADDITION));
    }
}
