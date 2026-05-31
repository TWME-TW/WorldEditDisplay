package dev.twme.worldeditdisplay.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

/**
 * Utility for checking player client version via ViaVersion API.
 * ViaVersion is optional; if the plugin is not present or API is unavailable,
 * all checks gracefully fall back to conservative assumptions.
 */
public final class VersionChecker {

    private static final boolean VIAVERSION_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("com.viaversion.viaversion.api.Via");
            available = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
        } catch (ClassNotFoundException ignored) {
        }
        VIAVERSION_AVAILABLE = available;
    }

    private VersionChecker() {
        // utility class
    }

    /**
     * Returns true if ViaVersion is present on the server.
     */
    public static boolean isViaVersionAvailable() {
        return VIAVERSION_AVAILABLE;
    }

    /**
     * Checks if the player's client supports TextDisplay entities (1.19.4+).
     *
     * @param player the player to check
     * @return true if the player's client version is >= 1.19.4;
     *         false if ViaVersion is not installed, API fails, or player is < 1.19.4.
     */
    public static boolean isTextDisplaySupported(Player player) {
        if (!VIAVERSION_AVAILABLE || player == null) {
            return false;
        }
        try {
            ProtocolVersion version = Via.getAPI().getPlayerProtocolVersion(player);
            return version.newerThanOrEqualTo(ProtocolVersion.v1_19_4);
        } catch (Exception e) {
            // API unavailable or player not tracked → conservative fallback
            return false;
        }
    }

    /**
     * Inverse of {@link #isTextDisplaySupported(Player)}.
     * Returns true if the player should use particle fallback rendering.
     */
    public static boolean needsParticleFallback(Player player) {
        return !isTextDisplaySupported(player);
    }
}
