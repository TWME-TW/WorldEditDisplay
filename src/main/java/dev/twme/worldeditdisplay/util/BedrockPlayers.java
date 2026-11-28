package dev.twme.worldeditdisplay.util;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;

/**
 * Detects Bedrock clients through a local Geyser instance or Floodgate on the backend.
 */
public final class BedrockPlayers {
    private BedrockPlayers() {}

    public static boolean isBedrockPlayer(UUID playerId) {
        PluginManager plugins = Bukkit.getPluginManager();
        return (plugins.isPluginEnabled("Geyser-Spigot") && Geyser.isBedrockPlayer(playerId))
                || (plugins.isPluginEnabled("floodgate") && Floodgate.isBedrockPlayer(playerId));
    }

    public static boolean supportsPrimitiveText(UUID playerId) {
        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (geyser == null || !geyser.isEnabled() || !Geyser.isBedrockPlayer(playerId)) return false;
        // The Geyser API version is shared by builds with and without primitive text.
        try {
            Class.forName("org.geysermc.geyser.entity.type.TextDisplayEntity", false,
                    geyser.getClass().getClassLoader()).getMethod("updatePrimitiveText");
            return GeyserPrimitiveText.isAvailable(geyser);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static final class Geyser {
        private static boolean isBedrockPlayer(UUID playerId) {
            return GeyserApi.api().isBedrockPlayer(playerId);
        }
    }

    private static final class Floodgate {
        private static boolean isBedrockPlayer(UUID playerId) {
            return FloodgateApi.getInstance().isFloodgatePlayer(playerId);
        }
    }
}
