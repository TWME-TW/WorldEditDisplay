package dev.twme.worldeditdisplay.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private static Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public static PlayerData getPlayerData(UUID uuid) {
        // PlayerData needs a Player, not just UUID
        // This should not be used - use PlayerData.getPlayerData(Player) instead
        throw new UnsupportedOperationException("Use PlayerData.getPlayerData(Player) instead");
    }

    public static boolean removePlayerData(UUID uuid) {
        return playerDataMap.remove(uuid) != null;
    }
}
