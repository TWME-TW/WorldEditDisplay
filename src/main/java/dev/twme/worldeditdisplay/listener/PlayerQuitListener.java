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
        // Clear any rendering data for the player
        if (plugin.getRenderManager() != null) {
            plugin.getRenderManager().clearRender(event.getPlayer().getUniqueId());
        }

        // Remove player language record
        if (plugin.getLanguageManager() != null) {
            plugin.getLanguageManager().removePlayerLanguage(event.getPlayer().getUniqueId());
        }

        // Finally, remove player data
        PlayerData.removePlayerData(event.getPlayer().getUniqueId());
    }
}
