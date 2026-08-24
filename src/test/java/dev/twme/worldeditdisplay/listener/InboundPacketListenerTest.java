package dev.twme.worldeditdisplay.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dev.twme.worldeditdisplay.common.Constants;

class InboundPacketListenerTest {

    @Test
    void detectsOfficialCuiVersionHandshake() {
        assertTrue(InboundPacketListener.isCuiHandshake(
                Constants.CUI_CHANNEL,
                "v|4".getBytes(StandardCharsets.UTF_8)
        ));
        assertTrue(InboundPacketListener.isCuiHandshake(
                Constants.CUI_CHANNEL,
                "v|5".getBytes(StandardCharsets.UTF_8)
        ));
    }

    @Test
    void ignoresNonHandshakeMessagesOnCuiChannel() {
        assertFalse(InboundPacketListener.isCuiHandshake(
                Constants.CUI_CHANNEL,
                "s|cuboid".getBytes(StandardCharsets.UTF_8)
        ));
        assertFalse(InboundPacketListener.isCuiHandshake(
                Constants.CUI_CHANNEL,
                "v|".getBytes(StandardCharsets.UTF_8)
        ));
    }

    @Test
    void detectsCuiInNullSeparatedRegistrationList() {
        assertTrue(InboundPacketListener.isCuiHandshake(
                Constants.REGISTER_CHANNEL,
                "example:first\0worldedit:cui\0example:last\0".getBytes(StandardCharsets.UTF_8)
        ));
    }

    @Test
    void detectsSingleChannelRegistrationWithoutTrailingNull() {
        assertTrue(InboundPacketListener.isCuiHandshake(
                Constants.REGISTER_CHANNEL,
                Constants.CUI_CHANNEL.getBytes(StandardCharsets.UTF_8)
        ));
    }

    @Test
    void registrationRequiresAnExactChannelName() {
        assertFalse(InboundPacketListener.isCuiHandshake(
                Constants.REGISTER_CHANNEL,
                "example:worldedit:cui-proxy".getBytes(StandardCharsets.UTF_8)
        ));
        assertFalse(InboundPacketListener.isCuiHandshake(
                Constants.REGISTER_CHANNEL,
                new byte[0]
        ));
    }
}
