package dev.twme.worldeditdisplay.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Listener for player disconnect events
 * Cleans up PlayerData and renders to prevent memory leaks
 */
public class PlayerQuitListener implements Listener {
    
    private final WorldEditDisplay plugin;
    
    public PlayerQuitListener(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player renders first
        if (plugin.getRenderManager() != null) {
            plugin.getRenderManager().clearRender(event.getPlayer().getUniqueId());
        }
        
        // Clean up player language record
        if (plugin.getLanguageManager() != null) {
            plugin.getLanguageManager().removePlayerLanguage(event.getPlayer().getUniqueId());
        }
        
        // Clean up player data when player disconnects
        PlayerData.removePlayerData(event.getPlayer().getUniqueId());
    }
}
