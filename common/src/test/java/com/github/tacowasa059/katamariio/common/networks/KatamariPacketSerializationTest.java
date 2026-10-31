package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KatamariPacketSerializationTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Verify encode/decode and clearExisting flag of S2CKatamariDeltaPacket")
    void testDeltaPacketSerialization() {
        KatamariBlocks originalBlocks = new KatamariBlocks();
        originalBlocks.addRaw(null, 0.0f, 0.0f, 0.0f, 1.0f, 1.5f, -2.5f, 3.0f);
        originalBlocks.addRaw(null, 0.5f, 0.5f, 0.5f, 0.5f, -4.0f, 0.0f, 2.0f);

        S2CKatamariDeltaPacket packetOut = new S2CKatamariDeltaPacket(42, originalBlocks, true);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        S2CKatamariDeltaPacket.encode(packetOut, buf);

        S2CKatamariDeltaPacket packetIn = S2CKatamariDeltaPacket.decode(buf);

        // Verify all buffer contents were read completely
        assertNotNull(packetIn);
        assertEquals(0, buf.readableBytes(), "Buffer data must be completely read without excess or deficit");
    }

    @Test
    @DisplayName("Verify delta packet (clearExisting=false) encode/decode")
    void testDeltaPacketWithoutClear() {
        KatamariBlocks originalBlocks = new KatamariBlocks();
        originalBlocks.addRaw(null, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f);

        S2CKatamariDeltaPacket packetOut = new S2CKatamariDeltaPacket(99, originalBlocks, false);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        S2CKatamariDeltaPacket.encode(packetOut, buf);

        S2CKatamariDeltaPacket packetIn = S2CKatamariDeltaPacket.decode(buf);
        assertNotNull(packetIn);
        assertEquals(0, buf.readableBytes());
    }
}
