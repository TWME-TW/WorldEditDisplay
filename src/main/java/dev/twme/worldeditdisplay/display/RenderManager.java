package dev.twme.worldeditdisplay.display;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
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
import dev.twme.worldeditdisplay.util.MessageUtil;

/**
 * keeps track of player renderers
 * handles main and extra regions for players
 */
public class RenderManager {

    private final WorldEditDisplay plugin;

    private final Map<UUID, RegionRenderer> mainRenderers;
    private final Map<UUID, Map<UUID, RegionRenderer>> multiRenderers;
    private final Map<Class<? extends Region>, Class<? extends RegionRenderer>> rendererTypes;

    public RenderManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
        this.mainRenderers = new ConcurrentHashMap<>();
        this.multiRenderers = new ConcurrentHashMap<>();
        this.rendererTypes = new HashMap<>();

        registerRendererTypes();
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

        if (playerData.isDebugEnabled()) {
            int entityCount = getPlayerEntityCount(playerId);
            MessageUtil.sendTranslated(player, "command.wedisplay.debug.entity_count", entityCount);
        }
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
    }

    private RegionRenderer createRenderer(Player player, Region region) {
        Class<? extends RegionRenderer> rendererClass = rendererTypes.get(region.getClass());
        if (rendererClass == null) {
            plugin.getLogger().warning("renderer not found: " + region.getClass().getSimpleName());
            return null;
        }

        try {
            var playerSettings = plugin.getPlayerSettingsManager().getSettings(player.getUniqueId());
            return rendererClass
                    .getConstructor(WorldEditDisplay.class, Player.class, dev.twme.worldeditdisplay.config.PlayerRenderSettings.class)
                    .newInstance(plugin, player, playerSettings);
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
        clearAllRenders();
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
