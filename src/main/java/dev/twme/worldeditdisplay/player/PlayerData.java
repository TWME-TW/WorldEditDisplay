package dev.twme.worldeditdisplay.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.event.CUIEventDispatcher;
import dev.twme.worldeditdisplay.region.Region;
import dev.twme.worldeditdisplay.region.RegionType;

public class PlayerData {

    private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    
    private final Player player;
    private final CUIEventDispatcher dispatcher;
    private boolean isCuiEnabled = false;
    private CUI_MODE mode;
    
    // Region data
    private Region currentRegion;
    private final Map<UUID, Region> multiRegions = new HashMap<>();
    
    // 追蹤當前正在操作的多重選區
    private UUID currentMultiRegionId;
    
    // Colour settings
    private String primaryColor;
    private String secondaryColor;
    private String gridColor;
    private String backgroundColor;
    private boolean gridEnabled = true;
    private boolean backgroundEnabled = true;

    public PlayerData(Player player) {
        this.player = player;
        this.dispatcher = new CUIEventDispatcher(this);
    }
    
    /**
     * Get or create PlayerData for a player
     */
    public static PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(player));
    }
    
    /**
     * Remove PlayerData when player leaves
     */
    public static void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public CUIEventDispatcher getDispatcher() {
        return dispatcher;
    }







    public CUI_MODE getMode(){
        return mode;
    }

    public void setMode(CUI_MODE mode){
        this.mode = mode;
    }

    public boolean isCuiEnabled(){
        return isCuiEnabled;
    }

    public void setCuiEnabled(boolean isCuiEnabled){
        this.isCuiEnabled = isCuiEnabled;
    }
    
    // Region management methods
    
    /**
     * Get the current selection region (for non-multi selections)
     */
    public Region getSelection() {
        return currentRegion;
    }
    
    /**
     * Get a specific multi-selection region by UUID
     */
    public Region getSelection(UUID id) {
        return multiRegions.get(id);
    }
    
    /**
     * Get selection based on multi flag
     * 修改：multi 模式時返回當前多重選區，而不是一般選區
     */
    public Region getSelection(boolean multi) {
        if (!multi) {
            return currentRegion;
        }
        
        // 返回當前正在操作的多重選區
        return getCurrentMultiRegion();
    }
    
    /**
     * 取得當前正在操作的多重選區
     */
    public Region getCurrentMultiRegion() {
        if (currentMultiRegionId == null) {
            return null;
        }
        return multiRegions.get(currentMultiRegionId);
    }
    
    /**
     * 設定當前正在操作的多重選區 ID
     */
    public void setCurrentMultiRegionId(UUID id) {
        this.currentMultiRegionId = id;
    }
    
    /**
     * 取得當前多重選區 ID
     */
    public UUID getCurrentMultiRegionId() {
        return currentMultiRegionId;
    }
    
    /**
     * Set the current selection region
     */
    public void setSelection(Region region) {
        this.currentRegion = region;
    }
    
    /**
     * Set a multi-selection region
     */
    public void setSelection(UUID id, Region region) {
        if (id == null) {
            this.currentRegion = region;
        } else {
            if (region == null) {
                multiRegions.remove(id);
            } else {
                multiRegions.put(id, region);
            }
        }
    }
    
    /**
     * Create a new region of the specified type
     */
    public Region createRegion(String typeKey) {
        RegionType type = RegionType.fromKey(typeKey);
        if (type == null) {
            return null;
        }
        return type.createRegion(this);
    }
    
    /**
     * Clear all regions
     * 修改：新增參數控制是否只清除多重選區
     */
    public void clearRegions(boolean multiOnly) {
        if (multiOnly) {
            // 只清除多重選區
            this.multiRegions.clear();
            this.currentMultiRegionId = null;
        } else {
            // 清除所有選區
            this.currentRegion = null;
            this.multiRegions.clear();
            this.currentMultiRegionId = null;
        }
    }
    
    /**
     * Clear all regions (保留舊方法以向後相容)
     */
    public void clearRegions() {
        clearRegions(false);
    }
    
    /**
     * Get all multi-selection regions
     */
    public Map<UUID, Region> getMultiRegions() {
        return new HashMap<>(multiRegions);
    }
    
    // Colour management methods
    
    /**
     * Set selection colours based on CUI colour event
     * @param primary 主要顏色 (必需)
     * @param secondary 次要顏色 (必需)
     * @param grid 網格顏色 (null 或空字串表示停用網格)
     * @param background 背景顏色 (null 或空字串表示停用背景)
     */
    public void setSelectionColors(String primary, String secondary, String grid, String background) {
        this.primaryColor = primary;
        this.secondaryColor = secondary;
        
        // 處理網格顏色 - 空值表示停用網格
        if (grid != null && !grid.trim().isEmpty()) {
            this.gridColor = grid;
            this.gridEnabled = true;
        } else {
            this.gridColor = null;
            this.gridEnabled = false;
        }
        
        // 處理背景顏色 - 空值表示停用背景
        if (background != null && !background.trim().isEmpty()) {
            this.backgroundColor = background;
            this.backgroundEnabled = true;
        } else {
            this.backgroundColor = null;
            this.backgroundEnabled = false;
        }
    }
    
    /**
     * Get primary selection colour
     */
    public String getPrimaryColor() {
        return primaryColor;
    }
    
    /**
     * Get secondary selection colour
     */
    public String getSecondaryColor() {
        return secondaryColor;
    }
    
    /**
     * Get grid colour (may be null if grid is disabled)
     */
    public String getGridColor() {
        return gridColor;
    }
    
    /**
     * Get background colour (may be null if background is disabled)
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }
    
    /**
     * Check if grid display is enabled
     */
    public boolean isGridEnabled() {
        return gridEnabled;
    }
    
    /**
     * Check if background display is enabled
     */
    public boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }
}
