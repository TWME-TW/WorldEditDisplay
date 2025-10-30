package dev.twme.worldeditdisplay.config;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.entity.Player;

/**
 * 玩家設定管理器
 * 
 * 負責管理所有玩家的個人渲染設定
 * - 快取機制：在記憶體中快取玩家設定
 * - 自動載入/卸載：玩家加入時載入，離開時卸載
 */
public class PlayerSettingsManager {
    
    private final WorldEditDisplay plugin;
    private final Map<UUID, PlayerRenderSettings> settingsCache;
    
    public PlayerSettingsManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
        this.settingsCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 取得玩家的渲染設定
     * 如果尚未載入，會自動載入
     * 
     * @param player 玩家
     * @return 玩家的渲染設定
     */
    public PlayerRenderSettings getSettings(Player player) {
        return getSettings(player.getUniqueId());
    }
    
    /**
     * 取得玩家的渲染設定
     * 如果尚未載入，會自動載入
     * 
     * @param uuid 玩家 UUID
     * @return 玩家的渲染設定
     */
    public PlayerRenderSettings getSettings(UUID uuid) {
        return settingsCache.computeIfAbsent(uuid, 
            key -> new PlayerRenderSettings(plugin, key));
    }
    
    /**
     * 載入玩家設定到快取
     * 
     * @param player 玩家
     */
    public void loadSettings(Player player) {
        loadSettings(player.getUniqueId());
    }
    
    /**
     * 載入玩家設定到快取
     * 
     * @param uuid 玩家 UUID
     */
    public void loadSettings(UUID uuid) {
        if (!settingsCache.containsKey(uuid)) {
            PlayerRenderSettings settings = new PlayerRenderSettings(plugin, uuid);
            settingsCache.put(uuid, settings);
        }
    }
    
    /**
     * 卸載玩家設定（從快取中移除）
     * 
     * @param player 玩家
     */
    public void unloadSettings(Player player) {
        unloadSettings(player.getUniqueId());
    }
    
    /**
     * 卸載玩家設定（從快取中移除）
     * 
     * @param uuid 玩家 UUID
     */
    public void unloadSettings(UUID uuid) {
        settingsCache.remove(uuid);
    }
    
    /**
     * 重新載入玩家設定
     * 
     * @param player 玩家
     */
    public void reloadSettings(Player player) {
        reloadSettings(player.getUniqueId());
    }
    
    /**
     * 重新載入玩家設定
     * 
     * @param uuid 玩家 UUID
     */
    public void reloadSettings(UUID uuid) {
        PlayerRenderSettings settings = settingsCache.get(uuid);
        if (settings != null) {
            settings.load();
        }
    }
    
    /**
     * 重新載入所有玩家設定
     */
    public void reloadAllSettings() {
        for (PlayerRenderSettings settings : settingsCache.values()) {
            settings.load();
        }
    }
    
    /**
     * 清除所有快取
     */
    public void clearCache() {
        settingsCache.clear();
    }
    
    /**
     * 取得快取中的玩家數量
     * 
     * @return 快取中的玩家數量
     */
    public int getCachedPlayerCount() {
        return settingsCache.size();
    }
}
