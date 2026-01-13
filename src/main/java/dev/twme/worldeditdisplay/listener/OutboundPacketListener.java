package dev.twme.worldeditdisplay.listener;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;

import dev.twme.worldeditdisplay.common.Constants;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Listens to outgoing plugin messages.
 * Captures CUI messages and dispatches CUI events for the plugin.
 */
public class OutboundPacketListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PLUGIN_MESSAGE) return;

        WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage(event);
        String channel = packet.getChannelName();

        if (!Constants.CUI_CHANNEL.equals(channel)) return;

        byte[] data = packet.getData();
        String message = new String(data, StandardCharsets.UTF_8);
        Player player = event.getPlayer();

        // Skip if player lacks permission
        if (!player.hasPermission("worldeditdisplay.use")) return;

        PlayerData playerData = PlayerData.getPlayerData(player);

        // If CUI already enabled, let the packet go through
        if (playerData.isCuiEnabled()) return;

        event.setCancelled(true); // cancel packet sending

        // Parse CUI message
        String[] split = message.split("\\|", -1); // preserve trailing empty strings
        boolean multi = split[0].startsWith("+");
        String type = split[0].substring(multi ? 1 : 0);
        List<String> params = split.length > 1
                ? Arrays.asList(Arrays.copyOfRange(split, 1, split.length))
                : List.of();

        // Dispatch CUI event
        CUIEventArgs eventArgs = new CUIEventArgs(playerData, multi, type, params);
        playerData.getDispatcher().raiseEvent(eventArgs);
    }
}
