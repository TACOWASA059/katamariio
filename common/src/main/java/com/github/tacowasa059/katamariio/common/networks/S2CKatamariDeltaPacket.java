package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import com.github.tacowasa059.katamariio.common.serializers.ModDataSerializers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Server -> client incremental update: the blocks a player's katamari gained since the last tick.
 * Only the newly added blocks are sent (not the whole store), which is what makes the katamari sync
 * scale to many players. The authoritative full store still rides on the entity data as an occasional
 * keyframe (and as the snapshot new trackers receive), so this stream only has to carry the deltas.
 */
public class S2CKatamariDeltaPacket {
    private final int entityId;
    private final KatamariBlocks added;

    public S2CKatamariDeltaPacket(int entityId, KatamariBlocks added) {
        this.entityId = entityId;
        this.added = added;
    }

    public static void encode(S2CKatamariDeltaPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        ModDataSerializers.KATAMARI_BLOCKS.write(buf, packet.added);
    }

    public static S2CKatamariDeltaPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        KatamariBlocks added = ModDataSerializers.KATAMARI_BLOCKS.read(buf);
        return new S2CKatamariDeltaPacket(entityId, added);
    }

    /** Applies the delta to the matching entity's block store on the client. */
    public void apply(Level clientLevel) {
        if (clientLevel == null) {
            return;
        }
        Entity entity = clientLevel.getEntity(entityId);
        if (!(entity instanceof ICustomPlayerData data)) {
            return;
        }
        KatamariBlocks store = data.katamariIO$getBlocks();
        for (int i = 0; i < added.size(); i++) {
            store.addRaw(added.block(i), added.quatX(i), added.quatY(i), added.quatZ(i), added.quatW(i),
                    added.posX(i), added.posY(i), added.posZ(i));
        }
    }
}
