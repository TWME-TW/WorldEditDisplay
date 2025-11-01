package dev.twme.worldeditdisplay.display;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.display.renderer.CuboidRenderer;
import dev.twme.worldeditdisplay.display.renderer.CylinderRenderer;
import dev.twme.worldeditdisplay.display.renderer.EllipsoidRenderer;
import dev.twme.worldeditdisplay.display.renderer.PolygonRenderer;
import dev.twme.worldeditdisplay.display.renderer.PolyhedronRenderer;
import dev.twme.worldeditdisplay.display.renderer.RegionRenderer;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.region.CuboidRegion;
import dev.twme.worldeditdisplay.region.CylinderRegion;
import dev.twme.worldeditdisplay.region.EllipsoidRegion;
import dev.twme.worldeditdisplay.region.PolygonRegion;
import dev.twme.worldeditdisplay.region.PolyhedronRegion;
import dev.twme.worldeditdisplay.region.Region;

/**
 * 渲染管理器 - 管理所有玩家的選區視覺化
 * 
 * 職責:
 * 1. 為每個玩家維護渲染器實例
 * 2. 根據選區類型選擇正確的渲染器
 * 3. 處理渲染器的生命週期(創建/更新/清理)
 * 4. 提供統一的渲染 API
 */
public class RenderManager {
    
    private final WorldEditDisplay plugin;
    
    // 玩家 UUID -> 主選區渲染器
    private final Map<UUID, RegionRenderer> mainRenderers;
    
    // 玩家 UUID -> (選區 UUID -> 多選區渲染器)
    private final Map<UUID, Map<UUID, RegionRenderer>> multiRenderers;
    
    // 渲染器類型映射
    private final Map<Class<? extends Region>, Class<? extends RegionRenderer>> rendererTypes;
    
    public RenderManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
        this.mainRenderers = new ConcurrentHashMap<>();
        this.multiRenderers = new ConcurrentHashMap<>();
        this.rendererTypes = new HashMap<>();
        
        // 註冊渲染器類型
        registerRendererTypes();
        
        plugin.getLogger().info("RenderManager initialized successfully");
    }
    
    /**
     * 註冊各種選區類型對應的渲染器
     */
    private void registerRendererTypes() {
        rendererTypes.put(CuboidRegion.class, CuboidRenderer.class);
        rendererTypes.put(PolygonRegion.class, PolygonRenderer.class);
        rendererTypes.put(EllipsoidRegion.class, EllipsoidRenderer.class);
        rendererTypes.put(CylinderRegion.class, CylinderRenderer.class);
        rendererTypes.put(PolyhedronRegion.class, PolyhedronRenderer.class);
        
        plugin.getLogger().info("Registered " + rendererTypes.size() + " renderer types");
    }
    
    /**
     * 更新玩家的選區渲染
     * 依照 WorldEditCUI 的邏輯：
     * 1. 渲染主選區 (this.selection)
     * 2. 渲染所有多選區 (this.regions)
     * 
     * @param player 目標玩家
     */
    public void updateRender(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 獲取玩家數據
        PlayerData playerData = PlayerData.getPlayerData(player);
        if (playerData == null) {
            plugin.getLogger().warning("Failed to get player data: " + player.getName());
            return;
        }
        
        // 檢查玩家是否啟用渲染
        if (!playerData.isRenderingEnabled()) {
            // 如果渲染被停用，清除所有現有渲染
            clearRender(playerId);
            return;
        }
        
        // 1. 渲染主選區
        Region mainSelection = playerData.getSelection();
        updateMainSelection(player, playerId, mainSelection);
        
        // 2. 渲染所有多選區
        Map<UUID, Region> multiRegions = playerData.getMultiRegions();
        updateMultiSelections(player, playerId, multiRegions);
    }
    
    /**
     * 更新主選區渲染
     */
    private void updateMainSelection(Player player, UUID playerId, Region mainSelection) {
        RegionRenderer currentRenderer = mainRenderers.get(playerId);
        
        // 如果沒有主選區，清除主選區渲染
        if (mainSelection == null) {
            if (currentRenderer != null) {
                currentRenderer.clear();
                mainRenderers.remove(playerId);
            }
            return;
        }
        
        // 檢查是否需要切換渲染器類型
        if (currentRenderer != null) {
            if (!currentRenderer.getRegionType().equals(mainSelection.getClass())) {
                currentRenderer.clear();
                mainRenderers.remove(playerId);
                currentRenderer = null;
            }
        }
        
        // 創建或更新渲染器
        if (currentRenderer == null) {
            currentRenderer = createRenderer(player, mainSelection);
            if (currentRenderer != null) {
                mainRenderers.put(playerId, currentRenderer);
            } else {
                plugin.getLogger().warning("Failed to create renderer for region type: " + mainSelection.getClass().getSimpleName());
                return;
            }
        }
        
        // 執行渲染
        try {
            currentRenderer.render(mainSelection);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Main selection rendering failed: " + player.getName(), e);
        }
    }
    
    /**
     * 更新多選區渲染
     */
    private void updateMultiSelections(Player player, UUID playerId, Map<UUID, Region> multiRegions) {
        // 獲取或創建玩家的多選區渲染器映射
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.computeIfAbsent(
            playerId, k -> new ConcurrentHashMap<>()
        );
        
        // 找出需要移除的渲染器（不再存在的選區）
        playerMultiRenderers.keySet().removeIf(regionId -> {
            if (!multiRegions.containsKey(regionId)) {
                RegionRenderer renderer = playerMultiRenderers.remove(regionId);
                if (renderer != null) {
                    renderer.clear();
                }
                return true;
            }
            return false;
        });
        
        // 更新或創建每個多選區的渲染
        for (Map.Entry<UUID, Region> entry : multiRegions.entrySet()) {
            UUID regionId = entry.getKey();
            Region region = entry.getValue();
            
            if (region == null) {
                continue;
            }
            
            RegionRenderer renderer = playerMultiRenderers.get(regionId);
            
            // 檢查是否需要切換渲染器類型
            if (renderer != null) {
                if (!renderer.getRegionType().equals(region.getClass())) {
                    renderer.clear();
                    playerMultiRenderers.remove(regionId);
                    renderer = null;
                }
            }
            
            // 創建新渲染器
            if (renderer == null) {
                renderer = createRenderer(player, region);
                if (renderer != null) {
                    playerMultiRenderers.put(regionId, renderer);
                } else {
                    plugin.getLogger().warning("Failed to create renderer for multi-selection: " + region.getClass().getSimpleName());
                    continue;
                }
            }
            
            // 執行渲染
            try {
                renderer.render(region);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Multi-selection rendering failed: " + player.getName(), e);
            }
        }
    }
    
    /**
     * 清除玩家的所有渲染實體
     * 
     * @param playerId 玩家 UUID
     */
    public void clearRender(UUID playerId) {
        // 清除主選區渲染
        RegionRenderer mainRenderer = mainRenderers.remove(playerId);
        if (mainRenderer != null) {
            mainRenderer.clear();
        }
        
        // 清除所有多選區渲染
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.remove(playerId);
        if (playerMultiRenderers != null) {
            playerMultiRenderers.values().forEach(RegionRenderer::clear);
            playerMultiRenderers.clear();
        }
    }
    
    /**
     * 清除所有玩家的渲染
     */
    public void clearAllRenders() {
        // 清除主選區
        mainRenderers.values().forEach(RegionRenderer::clear);
        mainRenderers.clear();
        
        // 清除多選區
        multiRenderers.values().forEach(playerRenderers -> {
            playerRenderers.values().forEach(RegionRenderer::clear);
            playerRenderers.clear();
        });
        multiRenderers.clear();
    }
    
    /**
     * 根據選區類型創建對應的渲染器
     * 
     * @param player 玩家
     * @param region 選區
     * @return 渲染器實例,如果無法創建則返回 null
     */
    private RegionRenderer createRenderer(Player player, Region region) {
        Class<? extends RegionRenderer> rendererClass = rendererTypes.get(region.getClass());
        
        if (rendererClass == null) {
            plugin.getLogger().warning("Renderer type not found: " + region.getClass().getSimpleName());
            return null;
        }
        
        try {
            // 獲取玩家設定
            var playerSettings = plugin.getPlayerSettingsManager().getSettings(player.getUniqueId());
            
            // 使用反射創建渲染器實例，並傳入玩家設定
            return rendererClass
                .getConstructor(WorldEditDisplay.class, Player.class, dev.twme.worldeditdisplay.config.PlayerRenderSettings.class)
                .newInstance(plugin, player, playerSettings);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create renderer: " + rendererClass.getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 獲取玩家當前的主選區渲染器
     * 
     * @param playerId 玩家 UUID
     * @return 渲染器實例,如果不存在則返回 null
     */
    public RegionRenderer getRenderer(UUID playerId) {
        return mainRenderers.get(playerId);
    }
    
    /**
     * 檢查玩家是否有活動的渲染
     * 
     * @param playerId 玩家 UUID
     * @return 如果有活動渲染則返回 true
     */
    public boolean hasActiveRender(UUID playerId) {
        boolean hasMain = mainRenderers.containsKey(playerId);
        boolean hasMulti = multiRenderers.containsKey(playerId) && 
                          !multiRenderers.get(playerId).isEmpty();
        return hasMain || hasMulti;
    }
    
    /**
     * 獲取當前活動渲染的數量
     * 
     * @return 活動渲染數量
     */
    public int getActiveRenderCount() {
        int mainCount = mainRenderers.size();
        int multiCount = multiRenderers.values().stream()
            .mapToInt(Map::size)
            .sum();
        return mainCount + multiCount;
    }
    
    /**
     * 關閉渲染管理器,清理所有資源
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down RenderManager...");
        clearAllRenders();
    }
    
    /**
     * 重新渲染玩家的選區（用於設定變更後立即生效）
     * 
     * @param player 玩家
     */
    public void refreshPlayerRenderer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 清除當前渲染
        clearRender(playerId);
        
        // 重新渲染
        updateRender(player);
        
        plugin.getLogger().fine(String.format("Refreshed renderer for player %s", player.getName()));
    }
}
