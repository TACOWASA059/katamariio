package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.KatamariIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 1.20.5+ {@link CustomPacketPayload} wrappers around the loader-independent packet classes.
 * Shared by the Fabric and NeoForge network handlers; the wire format is each packet's own
 * encode/decode, so it stays identical to the 1.20.1 implementation.
 */
public final class KatamariPayloads {
    private KatamariPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, path));
    }

    public record RemoveBlock(RemoveBlockPacket packet) implements CustomPacketPayload {
        public static final Type<RemoveBlock> TYPE = createType("remove_block");
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveBlock> CODEC = StreamCodec.of(
                (buf, payload) -> payload.packet.encode(buf),
                buf -> new RemoveBlock(RemoveBlockPacket.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Rotation(C2SPlayerRotationPacket packet) implements CustomPacketPayload {
        public static final Type<Rotation> TYPE = createType("player_rotation");
        public static final StreamCodec<RegistryFriendlyByteBuf, Rotation> CODEC = StreamCodec.of(
                (buf, payload) -> C2SPlayerRotationPacket.encode(payload.packet, buf),
                buf -> new Rotation(C2SPlayerRotationPacket.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlayerSync(S2CPlayerPacket packet) implements CustomPacketPayload {
        public static final Type<PlayerSync> TYPE = createType("player_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSync> CODEC = StreamCodec.of(
                (buf, payload) -> S2CPlayerPacket.encode(payload.packet, buf),
                buf -> new PlayerSync(S2CPlayerPacket.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Delta(S2CKatamariDeltaPacket packet) implements CustomPacketPayload {
        public static final Type<Delta> TYPE = createType("katamari_delta");
        public static final StreamCodec<RegistryFriendlyByteBuf, Delta> CODEC = StreamCodec.of(
                (buf, payload) -> S2CKatamariDeltaPacket.encode(payload.packet, buf),
                buf -> new Delta(S2CKatamariDeltaPacket.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ConfigSync(S2CConfigSyncPacket packet) implements CustomPacketPayload {
        public static final Type<ConfigSync> TYPE = createType("config_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSync> CODEC = StreamCodec.of(
                (buf, payload) -> S2CConfigSyncPacket.encode(payload.packet, buf),
                buf -> new ConfigSync(S2CConfigSyncPacket.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
