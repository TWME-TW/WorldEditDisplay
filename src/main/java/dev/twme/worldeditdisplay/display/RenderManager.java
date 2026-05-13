package dev.twme.worldeditdisplay.display;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.SharedRenderSettings;
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
import dev.twme.worldeditdisplay.share.ShareManager;
import dev.twme.worldeditdisplay.util.MessageUtil;

/**
 * keeps track of player renderers
 * handles main and extra regions for players
 */
public class RenderManager {

    private final WorldEditDisplay plugin;

    private final Map<UUID, RegionRenderer> mainRenderers;
    private final Map<UUID, Map<UUID, RegionRenderer>> multiRenderers;
    /** viewer → (sharer → renderer): renderers for other players' shared selections */
    private final Map<UUID, Map<UUID, RegionRenderer>> sharedRenderers;
    private final Map<Class<? extends Region>, Class<? extends RegionRenderer>> rendererTypes;

    /**
     * Colour palette cycled through for each sharer slot.
     * ARGB: fully-opaque cyan, magenta, yellow, orange, lime, pink.
     */
    private static final List<Color> SHARED_COLORS = List.of(
            Color.fromARGB(204, 0,   200, 255),   // cyan
            Color.fromARGB(204, 255, 80,  255),   // magenta
            Color.fromARGB(204, 255, 220, 0  ),   // yellow
            Color.fromARGB(204, 255, 140, 0  ),   // orange
            Color.fromARGB(204, 80,  255, 80 ),   // lime
            Color.fromARGB(204, 255, 100, 150)    // pink
    );

    private BukkitTask rebaseTask;

    public RenderManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
        this.mainRenderers = new ConcurrentHashMap<>();
        this.multiRenderers = new ConcurrentHashMap<>();
        this.sharedRenderers = new ConcurrentHashMap<>();
        this.rendererTypes = new HashMap<>();

        registerRendererTypes();
        startRebaseTask();
        plugin.getLogger().info("RenderManager started");
    }

    private void registerRendererTypes() {
        rendererTypes.put(CuboidRegion.class, CuboidRenderer.class);
        rendererTypes.put(PolygonRegion.class, PolygonRenderer.class);
        rendererTypes.put(EllipsoidRegion.class, EllipsoidRenderer.class);
        rendererTypes.put(CylinderRegion.class, CylinderRenderer.class);
        rendererTypes.put(PolyhedronRegion.class, PolyhedronRenderer.class);

        plugin.getLogger().info("renderer types registered: " + rendererTypes.size());
    }

    /**
     * update renders for one player
     */
    public void updateRender(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = PlayerData.getPlayerData(player);

        if (playerData == null) {
            plugin.getLogger().warning("no player data: " + player.getName());
            return;
        }

        if (!playerData.isRenderingEnabled()) {
            clearRender(playerId);
            return;
        }

        updateMainSelection(player, playerId, playerData.getSelection());
        updateMultiSelections(player, playerId, playerData.getMultiRegions());
        updateSharedSelections(player, playerId);

        // When this player's own selection changes, update renderers for all viewers
        notifyViewersOfSharer(player);

        if (playerData.isDebugEnabled()) {
            int entityCount = getPlayerEntityCount(playerId);
            MessageUtil.sendTranslated(player, "command.wedisplay.debug.entity_count", entityCount);
        }
    }

    /**
     * Renders the selections of all players that {@code viewer} is watching.
     */
    private void updateSharedSelections(Player viewer, UUID viewerId) {
        ShareManager shareManager = plugin.getShareManager();
        if (shareManager == null) return;

        Set<UUID> sharers = shareManager.getActiveSharers(viewerId);
        Map<UUID, RegionRenderer> viewerSharedRenderers =
                sharedRenderers.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());

        // Remove renderers for sharers no longer in the active list
        viewerSharedRenderers.keySet().removeIf(sharerId -> {
            if (!sharers.contains(sharerId)) {
                RegionRenderer r = viewerSharedRenderers.remove(sharerId);
                if (r != null) r.clear();
                return true;
            }
            return false;
        });

        int slot = 0;
        for (UUID sharerId : sharers) {
            renderSharedForViewer(viewer, viewerSharedRenderers, sharerId, slot);
            slot++;
        }
    }

    /**
     * When {@code sharer}'s own selection changes, push updates to every active viewer.
     */
    private void notifyViewersOfSharer(Player sharer) {
        ShareManager shareManager = plugin.getShareManager();
        if (shareManager == null) return;

        Set<UUID> viewers = shareManager.getActiveViewers(sharer.getUniqueId());
        if (viewers.isEmpty()) return;

        // Build an ordered list so each viewer always gets the same colour slot for this sharer
        int slot = computeSharerSlot(sharer.getUniqueId());

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) continue;

            Map<UUID, RegionRenderer> viewerSharedRenderers =
                    sharedRenderers.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());
            renderSharedForViewer(viewer, viewerSharedRenderers, sharer.getUniqueId(), slot);
        }
    }

    /** Render the sharer's main selection for the viewer, always (no dirty-check needed here). */
    private void renderSharedForViewer(Player viewer, Map<UUID, RegionRenderer> viewerSharedRenderers,
                                       UUID sharerId, int colorSlot) {
        Player sharerPlayer = Bukkit.getPlayer(sharerId);
        if (sharerPlayer == null || !sharerPlayer.isOnline()) {
            RegionRenderer r = viewerSharedRenderers.remove(sharerId);
            if (r != null) r.clear();
            return;
        }

        PlayerData sharerData = PlayerData.getPlayerData(sharerPlayer);
        if (sharerData == null) return;

        Region sharerRegion = sharerData.getSelection();

        if (sharerRegion == null) {
            RegionRenderer r = viewerSharedRenderers.remove(sharerId);
            if (r != null) r.clear();
            return;
        }

        RegionRenderer renderer = viewerSharedRenderers.get(sharerId);

        // Replace renderer if region type changed
        if (renderer != null && !renderer.getRegionType().equals(sharerRegion.getClass())) {
            renderer.clear();
            viewerSharedRenderers.remove(sharerId);
            renderer = null;
        }

        Color color = SHARED_COLORS.get(colorSlot % SHARED_COLORS.size());
        SharedRenderSettings sharedSettings = new SharedRenderSettings(plugin, color);

        if (renderer == null) {
            renderer = createRenderer(viewer, sharerRegion, sharedSettings);
            if (renderer != null) viewerSharedRenderers.put(sharerId, renderer);
            else return;
        }

        try {
            renderer.render(sharerRegion);
            // Do NOT clear dirty here – the sharer's own updateRender handles that
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "shared render fail for viewer " + viewer.getName(), e);
        }
    }

    /**
     * Computes a stable colour-slot index for a sharer, based on UUID ordering among all
     * current active-share relationships. Falls back to 0 if ShareManager is unavailable.
     */
    private int computeSharerSlot(UUID sharerId) {
        ShareManager sm = plugin.getShareManager();
        if (sm == null) return 0;
        // Use a simple deterministic slot based on least-significant UUID bits
        return (int) Math.abs(sharerId.getLeastSignificantBits() % SHARED_COLORS.size());
    }

    /** Clear all shared renderers for a specific viewer. */
    public void clearSharedRenders(UUID viewerId) {
        Map<UUID, RegionRenderer> map = sharedRenderers.remove(viewerId);
        if (map != null) {
            map.values().forEach(RegionRenderer::clear);
            map.clear();
        }
    }

    /** Clear the shared renderer a specific viewer has for a specific sharer. */
    public void clearSharedRender(UUID viewerId, UUID sharerId) {
        Map<UUID, RegionRenderer> map = sharedRenderers.get(viewerId);
        if (map == null) return;
        RegionRenderer r = map.remove(sharerId);
        if (r != null) r.clear();
    }

    private void updateMainSelection(Player player, UUID playerId, Region mainSelection) {
        RegionRenderer currentRenderer = mainRenderers.get(playerId);

        if (mainSelection == null) {
            if (currentRenderer != null) {
                currentRenderer.clear();
                mainRenderers.remove(playerId);
            }
            return;
        }

        if (currentRenderer != null && !currentRenderer.getRegionType().equals(mainSelection.getClass())) {
            currentRenderer.clear();
            mainRenderers.remove(playerId);
            currentRenderer = null;
        }

        if (currentRenderer == null) {
            currentRenderer = createRenderer(player, mainSelection);
            if (currentRenderer != null) mainRenderers.put(playerId, currentRenderer);
            else {
                plugin.getLogger().warning("cannot make renderer: " + mainSelection.getClass().getSimpleName());
                return;
            }
        } else if (!mainSelection.isDirty()) {
            // renderer 已存在且 region 沒有變動，跳過
            return;
        }

        try {
            currentRenderer.render(mainSelection);
            mainSelection.clearDirty();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "main render fail: " + player.getName(), e);
        }
    }

    private void updateMultiSelections(Player player, UUID playerId, Map<UUID, Region> multiRegions) {
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        // remove old regions
        playerMultiRenderers.keySet().removeIf(regionId -> {
            if (!multiRegions.containsKey(regionId)) {
                RegionRenderer renderer = playerMultiRenderers.remove(regionId);
                if (renderer != null) renderer.clear();
                return true;
            }
            return false;
        });

        for (Map.Entry<UUID, Region> entry : multiRegions.entrySet()) {
            UUID regionId = entry.getKey();
            Region region = entry.getValue();
            if (region == null) continue;

            RegionRenderer renderer = playerMultiRenderers.get(regionId);

            if (renderer != null && !renderer.getRegionType().equals(region.getClass())) {
                renderer.clear();
                playerMultiRenderers.remove(regionId);
                renderer = null;
            }

            if (renderer == null) {
                renderer = createRenderer(player, region);
                if (renderer != null) playerMultiRenderers.put(regionId, renderer);
                else {
                    plugin.getLogger().warning("cannot make multi renderer: " + region.getClass().getSimpleName());
                    continue;
                }
            } else if (!region.isDirty()) {
                // renderer 已存在且 region 沒有變動，跳過
                continue;
            }

            try {
                renderer.render(region);
                region.clearDirty();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "multi render fail: " + player.getName(), e);
            }
        }
    }

    public void clearRender(UUID playerId) {
        RegionRenderer mainRenderer = mainRenderers.remove(playerId);
        if (mainRenderer != null) mainRenderer.clear();

        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.remove(playerId);
        if (playerMultiRenderers != null) {
            playerMultiRenderers.values().forEach(RegionRenderer::clear);
            playerMultiRenderers.clear();
        }

        // Also clear shared renderers this player holds (selections they were watching)
        clearSharedRenders(playerId);
    }

    /**
     * Only clear the main renderer for a player. Multi renderers are untouched.
     */
    public void clearMainRender(UUID playerId) {
        RegionRenderer mainRenderer = mainRenderers.remove(playerId);
        if (mainRenderer != null) mainRenderer.clear();
    }

    /**
     * Remove a specific multi renderer only. Does not touch other renderers.
     */
    public void removeMultiRenderer(UUID playerId, UUID regionId) {
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.get(playerId);
        if (playerMultiRenderers == null) return;
        RegionRenderer renderer = playerMultiRenderers.remove(regionId);
        if (renderer != null) renderer.clear();
    }

    /**
     * Remove all multi renderers for a player. Does not touch the main renderer.
     */
    public void clearAllMultiRenderers(UUID playerId) {
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.remove(playerId);
        if (playerMultiRenderers != null) {
            playerMultiRenderers.values().forEach(RegionRenderer::clear);
            playerMultiRenderers.clear();
        }
    }

    /**
     * Render (or re-render) a single multi region. Does not touch other renderers.
     */
    public void renderSingleMultiRegion(Player player, UUID regionId, Region region) {
        if (region == null) return;
        UUID playerId = player.getUniqueId();
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        RegionRenderer renderer = playerMultiRenderers.get(regionId);

        if (renderer != null && !renderer.getRegionType().equals(region.getClass())) {
            renderer.clear();
            playerMultiRenderers.remove(regionId);
            renderer = null;
        }

        if (renderer == null) {
            renderer = createRenderer(player, region);
            if (renderer != null) playerMultiRenderers.put(regionId, renderer);
            else {
                plugin.getLogger().warning("cannot make multi renderer: " + region.getClass().getSimpleName());
                return;
            }
        }

        try {
            renderer.render(region);
            region.clearDirty();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "multi render fail: " + player.getName(), e);
        }
    }

    public void clearAllRenders() {
        mainRenderers.values().forEach(RegionRenderer::clear);
        mainRenderers.clear();

        multiRenderers.values().forEach(playerRenderers -> {
            playerRenderers.values().forEach(RegionRenderer::clear);
            playerRenderers.clear();
        });
        multiRenderers.clear();

        sharedRenderers.values().forEach(playerRenderers -> {
            playerRenderers.values().forEach(RegionRenderer::clear);
            playerRenderers.clear();
        });
        sharedRenderers.clear();
    }

    private RegionRenderer createRenderer(Player player, Region region) {
        return createRenderer(player, region, plugin.getPlayerSettingsManager().getSettings(player.getUniqueId()));
    }

    private RegionRenderer createRenderer(Player player, Region region,
                                           dev.twme.worldeditdisplay.config.PlayerRenderSettings settings) {
        Class<? extends RegionRenderer> rendererClass = rendererTypes.get(region.getClass());
        if (rendererClass == null) {
            plugin.getLogger().warning("renderer not found: " + region.getClass().getSimpleName());
            return null;
        }

        try {
            return rendererClass
                    .getConstructor(WorldEditDisplay.class, Player.class, dev.twme.worldeditdisplay.config.PlayerRenderSettings.class)
                    .newInstance(plugin, player, settings);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "cannot create renderer: " + rendererClass.getSimpleName(), e);
            return null;
        }
    }

    public RegionRenderer getRenderer(UUID playerId) {
        return mainRenderers.get(playerId);
    }

    public boolean hasActiveRender(UUID playerId) {
        boolean hasMain = mainRenderers.containsKey(playerId);
        boolean hasMulti = multiRenderers.containsKey(playerId) && !multiRenderers.get(playerId).isEmpty();
        return hasMain || hasMulti;
    }

    public int getActiveRenderCount() {
        int mainCount = mainRenderers.size();
        int multiCount = multiRenderers.values().stream().mapToInt(Map::size).sum();
        return mainCount + multiCount;
    }

    public int getPlayerEntityCount(UUID playerId) {
        int count = 0;
        RegionRenderer mainRenderer = mainRenderers.get(playerId);
        if (mainRenderer != null) {
            count += mainRenderer.getEntityCount();
        }
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.get(playerId);
        if (playerMultiRenderers != null) {
            for (RegionRenderer renderer : playerMultiRenderers.values()) {
                count += renderer.getEntityCount();
            }
        }
        return count;
    }

    public void shutdown() {
        plugin.getLogger().info("shutdown render manager");
        if (rebaseTask != null) {
            rebaseTask.cancel();
            rebaseTask = null;
        }
        clearAllRenders();
    }

    /**
     * Starts a periodic task that checks if players have moved far enough
     * from the original shape spawn point to require rebasing entity origins.
     * Runs every 10 ticks (0.5 seconds).
     */
    private void startRebaseTask() {
        rebaseTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (RegionRenderer renderer : mainRenderers.values()) {
                try {
                    renderer.rebaseOriginIfNeeded();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "rebase main renderer fail", e);
                }
            }
            for (Map<UUID, RegionRenderer> playerMultiRenderers : multiRenderers.values()) {
                for (RegionRenderer renderer : playerMultiRenderers.values()) {
                    try {
                        renderer.rebaseOriginIfNeeded();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "rebase multi renderer fail", e);
                    }
                }
            }
            for (Map<UUID, RegionRenderer> playerSharedRenderers : sharedRenderers.values()) {
                for (RegionRenderer renderer : playerSharedRenderers.values()) {
                    try {
                        renderer.rebaseOriginIfNeeded();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "rebase shared renderer fail", e);
                    }
                }
            }
        }, 10L, 10L);
    }

    public void refreshPlayerRenderer(Player player) {
        UUID playerId = player.getUniqueId();
        clearRender(playerId);

        PlayerData playerData = PlayerData.getPlayerData(player);
        if (playerData != null) {
            Region main = playerData.getSelection();
            if (main != null) main.markDirty();
            for (Region r : playerData.getMultiRegions().values()) {
                if (r != null) r.markDirty();
            }
        }

        updateRender(player);
        plugin.getLogger().fine("refreshed renderer for " + player.getName());
    }
}
