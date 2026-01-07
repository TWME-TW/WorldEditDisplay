package dev.twme.worldeditdisplay.listener;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

/**
 * Listens for players changing client language.
 * Updates the player's language in the plugin.
 */
public class PlayerLocaleChangeListener implements Listener {

    private final WorldEditDisplay plugin;

    public PlayerLocaleChangeListener(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        String newLocale = event.locale().toString().toLowerCase().replace("-", "_");

        // Update player language
        plugin.getLanguageManager().setPlayerLanguage(
                event.getPlayer().getUniqueId(),
                newLocale
        );
    }
}
