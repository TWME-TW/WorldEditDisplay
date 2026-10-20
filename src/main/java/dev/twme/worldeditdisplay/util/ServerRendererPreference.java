package dev.twme.worldeditdisplay.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Remembers, across sessions, that a player chose WorldEditDisplay rendering over a
 * detected WorldEditCUI client via {@code /wedisplay toggle}.
 * Stored in the player's persistent data container, so it is saved with the player data
 * of each backend server. Must be accessed on the player's entity thread.
 */
public final class ServerRendererPreference {
    private static final String KEY = "force_server_renderer";

    private ServerRendererPreference() {
    }

    public static boolean isForced(Plugin plugin, Player player) {
        Byte value = player.getPersistentDataContainer().get(key(plugin), PersistentDataType.BYTE);
        return value != null && value != 0;
    }

    public static void setForced(Plugin plugin, Player player, boolean forced) {
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (forced) {
            container.set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        } else {
            container.remove(key(plugin));
        }
    }

    private static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, KEY);
    }
}
