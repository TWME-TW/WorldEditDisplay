package dev.twme.worldeditdisplay.listener;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.common.Constants;
import dev.twme.worldeditdisplay.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.nio.charset.StandardCharsets;

/**
 * Handles player join events.
 * Initializes language, PlayerData, rendering permissions, and CUI registration.
 */
public class PlayerJoinListener implements Listener {

    private final WorldEditDisplay plugin;

    public PlayerJoinListener(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Initialize player language
        plugin.getLanguageManager().getPlayerLanguage(player);

        // Set auto-rendering based on permissions
        PlayerData playerData = PlayerData.getPlayerData(player);
        playerData.setRenderingEnabled(player.hasPermission("worldeditdisplay.render.auto-enable"));

        // Delay one second to allow CUI registration first
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (!player.isOnline()) return; // player left

            String cuiVersionMessage = "v|4";

            // Register channels for CUI
            WrapperPlayClientPluginMessage registerPacket = new WrapperPlayClientPluginMessage(
                    Constants.REGISTER_CHANNEL,
                    Constants.CUI_CHANNEL.getBytes(StandardCharsets.UTF_8)
            );
            WrapperPlayClientPluginMessage cuiVersionPacket = new WrapperPlayClientPluginMessage(
                    Constants.CUI_CHANNEL,
                    cuiVersionMessage.getBytes(StandardCharsets.UTF_8)
            );

            PacketEvents.getAPI().getPlayerManager().receivePacketSilently(player, registerPacket);
            PacketEvents.getAPI().getPlayerManager().receivePacketSilently(player, cuiVersionPacket);
        }, 20L); // 20 ticks = 1 second
    }
}
