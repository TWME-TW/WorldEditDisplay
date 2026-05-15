package dev.twme.worldeditdisplay.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Listens for players disconnecting.
 * Cleans up their PlayerData, renders, and language records to prevent memory leaks.
 */
public class PlayerQuitListener implements Listener {

    private final WorldEditDisplay plugin;

    public PlayerQuitListener(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID playerId = event.getPlayer().getUniqueId();

        // Remove from viewall set
        plugin.getViewAllPlayers().remove(playerId);

        // Clear any rendering data for the player
        if (plugin.getRenderManager() != null) {
            plugin.getRenderManager().clearRender(playerId);
        }

        // Clean up share-related pending invites for this player
        if (plugin.getShareManager() != null) {
            plugin.getShareManager().onPlayerQuit(playerId);
        }

        // Save and unload player render settings asynchronously
        if (plugin.getPlayerSettingsManager() != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getPlayerSettingsManager().saveAndUnload(playerId));
        }

        // Remove player language record
        if (plugin.getLanguageManager() != null) {
            plugin.getLanguageManager().removePlayerLanguage(playerId);
        }

        // Finally, remove player data
        PlayerData.removePlayerData(playerId);
    }
}
