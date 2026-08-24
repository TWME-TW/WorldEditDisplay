package dev.twme.worldeditdisplay.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import dev.twme.worldeditdisplay.player.PlayerData;

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

    @Test
    void clientCuiTakesPriorityWhenServerRenderingIsEnabled() {
        PlayerData playerData = new PlayerData((Player) null);
        playerData.setRenderingEnabled(true);
        playerData.setCuiEnabled(true);

        assertFalse(OutboundPacketListener.shouldUseServerRenderer(playerData));
    }

    @Test
    void clientCuiReceivesPacketsWhenServerRenderingDisabled() {
        PlayerData playerData = new PlayerData((Player) null);
        playerData.setRenderingEnabled(false);
        playerData.setCuiEnabled(true);

        assertFalse(OutboundPacketListener.shouldUseServerRenderer(playerData));
    }

    @Test
    void bedrockPlayersAlwaysUseServerRenderer() {
        PlayerData playerData = new PlayerData((Player) null);
        playerData.setRenderingEnabled(false);
        playerData.setCuiEnabled(true);
        playerData.setBedrockPlayer(true);

        assertTrue(OutboundPacketListener.shouldUseServerRenderer(playerData));
    }

    @Test
    void serverRendererConsumesPacketsForClientsWithoutCuiEvenWhenRenderingIsDisabled() {
        PlayerData playerData = new PlayerData((Player) null);
        playerData.setRenderingEnabled(false);

        assertTrue(OutboundPacketListener.shouldUseServerRenderer(playerData));
    }
}
