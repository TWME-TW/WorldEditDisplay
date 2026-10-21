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
 * When a player changes worlds, their own and shared/viewall renders are cleared and refreshed
 * so that selections are spawned again in the new world.
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

        // The client drops every entity when it changes world, so retained renderers would only
        // move entities that no longer exist. Drop the player's own and shared renderers, then
        // re-render: cached selections (including multi regions from plugins such as ClipboardCUI)
        // are spawned again in the new world.
        FoliaScheduler.getEntityScheduler().execute(player, plugin, () -> {
            if (!player.isOnline()) return;
            renderManager.clearOwnRenders(player.getUniqueId());
            renderManager.clearSharedRenders(player.getUniqueId());
            renderManager.updateRender(player);
        }, null, 1L);
    }
}
