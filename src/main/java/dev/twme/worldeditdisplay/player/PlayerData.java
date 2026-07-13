package dev.twme.worldeditdisplay.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.event.CUIEventDispatcher;
import dev.twme.worldeditdisplay.region.Region;
import dev.twme.worldeditdisplay.region.RegionType;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import io.github.retrooper.packetevents.util.folia.TaskWrapper;

/**
 * Stores all per-player CUI / WorldEditDisplay state.
 * Tracks current region selection(s), colors, rendering, and mode.
 */
public class PlayerData {
    private static final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    /**
     * Render mode preference for the particle-fallback feature.
     * <ul>
     *   <li>{@link #AUTO} — uses ViaVersion client detection (< 1.19.4 → particles)</li>
     *   <li>{@link #TEXT_DISPLAY} — always use TextDisplay entities</li>
     *   <li>{@link #PARTICLE} — always use particle fallback</li>
     * </ul>
     */
    public enum RenderMode {
        AUTO,
        TEXT_DISPLAY,
        PARTICLE
    }

    private final Player player;
    private final CUIEventDispatcher dispatcher;
    private boolean isCuiEnabled = false;
    private boolean renderingEnabled = false; // default off; will enable on login if player has permission
    private boolean debugEnabled = false;
    private boolean bedrockPlayer = false; // true if joined via Floodgate (GeyserMC)
    private boolean particleFallback = false; // true if player's client is < 1.19.4 (TextDisplay unsupported)
    private RenderMode renderMode = RenderMode.AUTO; // user preference, defaults to AUTO

    // Current single selection
    private Region currentRegion;

    // Multi-selection regions
    private final Map<UUID, Region> multiRegions = new HashMap<>();
    private UUID currentMultiRegionId; // tracks which multi-region the player is currently editing

    // Debounced render task
    private TaskWrapper pendingRenderTask;

    // Color settings
    private String primaryColor;
    private String secondaryColor;
    private String gridColor;
    private String backgroundColor;
    private boolean gridEnabled = true;
    private boolean backgroundEnabled = true;

    // ─── Session state (not persisted, reset on each login) ─────────────────
    /** Whether viewall mode is currently active for this player (requires use.view permission). */
    private boolean viewAllEnabled = false;
    /** Set of player UUIDs that are excluded from viewall rendering this session. */
    private final Set<UUID> viewAllHidden = new HashSet<>();
    /** Whether name-label display is enabled for watched selections this session. */
    private boolean showLabels = false;

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
        PlayerData data = playerDataMap.remove(uuid);
        if (data != null) {
            data.cancelPendingRender();
        }
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
     * Check if this player is a Bedrock (Floodgate) player.
     */
    public boolean isBedrockPlayer() {
        return bedrockPlayer;
    }

    /**
     * Set whether this player is a Bedrock (Floodgate) player.
     */
    public void setBedrockPlayer(boolean bedrockPlayer) {
        this.bedrockPlayer = bedrockPlayer;
    }

    /**
     * Check if this player needs particle fallback (client < 1.19.4, no TextDisplay support).
     * <p>
     * Considers the player's {@link RenderMode} preference:
     * <ul>
     *   <li>{@link RenderMode#TEXT_DISPLAY} — always returns {@code false}</li>
     *   <li>{@link RenderMode#PARTICLE} — always returns {@code true}</li>
     *   <li>{@link RenderMode#AUTO} — returns the auto-detected value</li>
     * </ul>
     * <p>
     * Bedrock (Floodgate) players are always forced to particle fallback
     * since they do not support TextDisplay entities.
     */
    public boolean isParticleFallback() {
        if (isBedrockPlayer()) return true;
        return switch (renderMode) {
            case TEXT_DISPLAY -> false;
            case PARTICLE -> true;
            case AUTO -> particleFallback;
        };
    }

    /**
     * Set the auto-detected fallback flag (called by PlayerJoinListener).
     */
    public void setParticleFallback(boolean particleFallback) {
        this.particleFallback = particleFallback;
    }

    /**
     * Get the current render mode preference.
     */
    public RenderMode getRenderMode() {
        return renderMode;
    }

    /**
     * Set the render mode preference.
     *
     * @param mode the desired mode ({@link RenderMode#AUTO}, {@link RenderMode#TEXT_DISPLAY},
     *             or {@link RenderMode#PARTICLE})
     */
    public void setRenderMode(RenderMode mode) {
        this.renderMode = mode != null ? mode : RenderMode.AUTO;
    }

    /**
     * Check if debug mode is enabled for this player
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Enable or disable debug mode for this player
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
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
        return Collections.unmodifiableMap(new HashMap<>(multiRegions));
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

    /**
     * 延遲渲染更新（防抖機制）
     * 每次呼叫時重置計時器，等待 2 ticks（約 100ms）後才執行渲染
     */
    public synchronized void scheduleRenderUpdate() {
        if (pendingRenderTask != null) {
            pendingRenderTask.cancel();
        }
        WorldEditDisplay plugin = WorldEditDisplay.getPlugin();
        if (plugin == null || plugin.getRenderManager() == null) return;

        pendingRenderTask = FoliaScheduler.getEntityScheduler().runDelayed(player, plugin,
                ignored -> {
                    synchronized (this) {
                        pendingRenderTask = null;
                    }
                    plugin.getRenderManager().updateRender(player);
                }, null, 2L);
    }

    /**
     * 取消待處理的渲染任務（用於玩家離開時清理）
     */
    public synchronized void cancelPendingRender() {
        if (pendingRenderTask != null) {
            pendingRenderTask.cancel();
            pendingRenderTask = null;
        }
    }

    // ─── Session state accessors ─────────────────────────────────────────────

    public boolean isViewAllEnabled() { return viewAllEnabled; }
    public void setViewAllEnabled(boolean enabled) { this.viewAllEnabled = enabled; }

    public boolean isShowLabels() { return showLabels; }
    public void setShowLabels(boolean showLabels) { this.showLabels = showLabels; }

    public boolean isViewAllHidden(UUID targetId) { return viewAllHidden.contains(targetId); }

    public void addViewAllHidden(UUID targetId) { viewAllHidden.add(targetId); }

    public void removeViewAllHidden(UUID targetId) { viewAllHidden.remove(targetId); }

    public void hideAllOnline() {
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                viewAllHidden.add(p.getUniqueId());
            }
        }
    }

    public Set<UUID> getViewAllHidden() { return java.util.Collections.unmodifiableSet(viewAllHidden); }
}