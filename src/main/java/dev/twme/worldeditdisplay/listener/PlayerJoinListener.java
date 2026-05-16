package dev.twme.worldeditdisplay.listener;

import java.nio.charset.StandardCharsets;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.common.Constants;
import dev.twme.worldeditdisplay.player.PlayerData;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;

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

        // Initialize viewall/label session defaults from permission nodes
        if (player.hasPermission("worldeditdisplay.use.view.defaultenable")) {
            playerData.setViewAllEnabled(true);
            plugin.getViewAllPlayers().add(player.getUniqueId());
        }
        if (player.hasPermission("worldeditdisplay.use.view.label.defaultenable")) {
            playerData.setShowLabels(true);
        }

        // Delay one second to allow CUI registration first
        FoliaScheduler.getEntityScheduler().execute(player, plugin, () -> {
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
        }, null, 20L); // 20 ticks = 1 second
    }
}
