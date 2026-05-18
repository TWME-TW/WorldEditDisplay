package dev.twme.worldeditdisplay.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.display.RenderManager;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;

/**
 * Handles players switching between worlds.
 * When a viewer changes worlds, their shared/viewall renders are cleared and refreshed
 * so that selections in the new world are properly displayed.
 */
public class PlayerChangedWorldListener implements Listener {

    private final WorldEditDisplay plugin;

    public PlayerChangedWorldListener(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        RenderManager renderManager = plugin.getRenderManager();
        if (renderManager == null) return;

        // Clear shared/viewall renders (entities from old world are gone client-side).
        // Then re-render: selections in the new world will re-appear if conditions are met.
        FoliaScheduler.getEntityScheduler().execute(player, plugin, () -> {
            if (!player.isOnline()) return;
            renderManager.clearSharedRenders(player.getUniqueId());
            renderManager.updateRender(player);
        }, null, 1L);
    }
}
