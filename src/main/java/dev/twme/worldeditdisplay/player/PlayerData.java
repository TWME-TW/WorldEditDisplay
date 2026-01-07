package dev.twme.worldeditdisplay.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.event.CUIEventDispatcher;
import dev.twme.worldeditdisplay.region.Region;
import dev.twme.worldeditdisplay.region.RegionType;

/**
 * Stores all per-player CUI / WorldEditDisplay state.
 * Tracks current region selection(s), colors, rendering, and mode.
 */
public class PlayerData {
    private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    private final Player player;
    private final CUIEventDispatcher dispatcher;
    private boolean isCuiEnabled = false;
    private boolean renderingEnabled = false; // default off; will enable on login if player has permission

    // Current single selection
    private Region currentRegion;

    // Multi-selection regions
    private final Map<UUID, Region> multiRegions = new HashMap<>();
    private UUID currentMultiRegionId; // tracks which multi-region the player is currently editing

    // Color settings
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
     * Remove PlayerData for a player when they leave
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

    public boolean isCuiEnabled() {
        return isCuiEnabled;
    }

    public void setCuiEnabled(boolean enabled) {
        this.isCuiEnabled = enabled;
    }

    /**
     * Check if rendering is enabled for this player
     */
    public boolean isRenderingEnabled() {
        return renderingEnabled;
    }

    /**
     * Enable or disable rendering for this player
     */
    public void setRenderingEnabled(boolean enabled) {
        this.renderingEnabled = enabled;
    }

    /**
     * Get the current selection (for non-multi selections)
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
     * Get selection depending on multi-mode
     * If multi = true, returns the current multi-selection region
     */
    public Region getSelection(boolean multi) {
        if (!multi) return currentRegion;
        return getCurrentMultiRegion();
    }

    /**
     * Get the current multi-selection region being edited
     */
    public Region getCurrentMultiRegion() {
        if (currentMultiRegionId == null) return null;
        return multiRegions.get(currentMultiRegionId);
    }

    /**
     * Set the ID of the current multi-selection region
     */
    public void setCurrentMultiRegionId(UUID id) {
        this.currentMultiRegionId = id;
    }

    /**
     * Get the ID of the current multi-selection region
     */
    public UUID getCurrentMultiRegionId() {
        return currentMultiRegionId;
    }

    /**
     * Set the current single selection region
     */
    public void setSelection(Region region) {
        this.currentRegion = region;
    }

    /**
     * Set a multi-selection region by ID
     * If id = null, overwrites the single selection
     */
    public void setSelection(UUID id, Region region) {
        if (id == null) {
            this.currentRegion = region;
        } else {
            if (region == null) multiRegions.remove(id);
            else multiRegions.put(id, region);
        }
    }

    /**
     * Create a new region by type key
     */
    public Region createRegion(String typeKey) {
        RegionType type = RegionType.fromKey(typeKey);
        return type == null ? null : type.createRegion(this);
    }

    /**
     * Clear regions
     * @param multiOnly if true, clears only multi-selection regions
     */
    public void clearRegions(boolean multiOnly) {
        if (multiOnly) {
            multiRegions.clear();
            currentMultiRegionId = null;
        } else {
            currentRegion = null;
            multiRegions.clear();
            currentMultiRegionId = null;
        }
    }

    /**
     * Clear all regions (legacy method)
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

    /**
     * Set selection colors based on CUI event
     * Empty or null grid/background disables them
     */
    public void setSelectionColors(String primary, String secondary, String grid, String background) {
        this.primaryColor = primary;
        this.secondaryColor = secondary;

        this.gridEnabled = grid != null && !grid.trim().isEmpty();
        this.gridColor = gridEnabled ? grid : null;

        this.backgroundEnabled = background != null && !background.trim().isEmpty();
        this.backgroundColor = backgroundEnabled ? background : null;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public String getGridColor() {
        return gridColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public boolean isGridEnabled() {
        return gridEnabled;
    }

    public boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }
}