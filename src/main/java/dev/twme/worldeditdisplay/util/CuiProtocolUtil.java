package dev.twme.worldeditdisplay.util;

import java.nio.charset.StandardCharsets;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import dev.twme.worldeditdisplay.common.Constants;

/**
 * Helpers for the client-to-server side of the WorldEdit CUI protocol.
 */
public final class CuiProtocolUtil {
    private static final String CUI_VERSION_MESSAGE = "v|4";

    private CuiProtocolUtil() {
    }

    /**
     * Registers the CUI channel and sends the protocol version as if it came from
     * the player. WorldEdit responds by sending the player's current selection,
     * which lets WorldEditDisplay rebuild its server-side selection state.
     */
    public static void requestSelectionRefresh(Player player) {
        WrapperPlayClientPluginMessage registerPacket = new WrapperPlayClientPluginMessage(
                Constants.REGISTER_CHANNEL,
                Constants.CUI_CHANNEL.getBytes(StandardCharsets.UTF_8)
        );
        WrapperPlayClientPluginMessage cuiVersionPacket = new WrapperPlayClientPluginMessage(
                Constants.CUI_CHANNEL,
                CUI_VERSION_MESSAGE.getBytes(StandardCharsets.UTF_8)
        );

        PacketEvents.getAPI().getPlayerManager().receivePacketSilently(player, registerPacket);
        PacketEvents.getAPI().getPlayerManager().receivePacketSilently(player, cuiVersionPacket);
    }
}
