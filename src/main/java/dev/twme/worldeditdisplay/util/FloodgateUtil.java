package dev.twme.worldeditdisplay.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Utility for checking whether a player is a Bedrock client via Floodgate (GeyserMC).
 * Floodgate is optional; if the plugin is not present all checks return false.
 */
public final class FloodgateUtil {

    private static final boolean FLOODGATE_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            available = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        } catch (ClassNotFoundException ignored) {
        }
        FLOODGATE_AVAILABLE = available;
    }

    private FloodgateUtil() {
        // utility class
    }

    /**
     * Returns true if Floodgate is present on the server.
     */
    public static boolean isFloodgateAvailable() {
        return FLOODGATE_AVAILABLE;
    }

    /**
     * Returns true if the given player is a Bedrock (Floodgate) player.
     * Returns false if Floodgate is not installed or the player is a Java player.
     */
    public static boolean isBedrockPlayer(Player player) {
        if (!FLOODGATE_AVAILABLE || player == null) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }
}
