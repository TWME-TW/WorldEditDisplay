package dev.twme.worldeditdisplay.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class OutboundPacketListenerTest {

    @Test
    void parseCuiMessagePreservesTrailingEmptyParams() {
        OutboundPacketListener.ParsedCuiMessage parsed = OutboundPacketListener.parseCuiMessage("u|0||");

        assertFalse(parsed.multi());
        assertEquals("u", parsed.type());
        assertEquals(List.of("0", "", ""), parsed.params());
    }

    @Test
    void parseCuiMessageHandlesMultiSelectionPrefix() {
        OutboundPacketListener.ParsedCuiMessage parsed = OutboundPacketListener.parseCuiMessage("+p|1|2|3");

        assertTrue(parsed.multi());
        assertEquals("p", parsed.type());
        assertEquals(List.of("1", "2", "3"), parsed.params());
    }

    @Test
    void parseCuiMessageRejectsEmptyType() {
        assertNull(OutboundPacketListener.parseCuiMessage("|1"));
        assertNull(OutboundPacketListener.parseCuiMessage("+|1"));
    }
}