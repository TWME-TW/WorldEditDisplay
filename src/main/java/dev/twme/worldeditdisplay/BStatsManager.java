package dev.twme.worldeditdisplay;

import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.config.RenderSettings;
import dev.twme.worldeditdisplay.display.RenderManager;
import dev.twme.worldeditdisplay.display.renderer.CuboidRenderer;
import dev.twme.worldeditdisplay.display.renderer.CylinderRenderer;
import dev.twme.worldeditdisplay.display.renderer.EllipsoidRenderer;
import dev.twme.worldeditdisplay.display.renderer.PolygonRenderer;
import dev.twme.worldeditdisplay.display.renderer.PolyhedronRenderer;
import dev.twme.worldeditdisplay.display.renderer.RegionRenderer;
import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Manages bStats custom chart registration for WorldEditDisplay.
 *
 * <p>Performance notes:
 * <ul>
 *   <li>bStats collects data at most every 30 minutes on an async task.</li>
 *   <li>Charts that read {@link RenderManager#getMainRenderers()} iterate a
 *       {@link java.util.concurrent.ConcurrentHashMap}, which is safe from any
 *       thread without additional synchronisation.</li>
 *   <li>Charts that read {@link RenderSettings} only ever access immutable
 *       server-config values – reload replaces the whole settings object, so
 *       old callbacks may briefly see stale values, but correctness is not a
 *       concern for statistics.</li>
 * </ul>
 */
public final class BStatsManager {

    private static final int PLUGIN_ID = 31356;

    private final WorldEditDisplay plugin;

    public BStatsManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
        Metrics metrics = new Metrics(plugin, PLUGIN_ID);
        registerCharts(metrics);
    }

    private void registerCharts(Metrics metrics) {
        registerRegionTypeUsage(metrics);
        registerWorldEditVariant(metrics);
        registerFillEnabledPerType(metrics);
        registerSeeThroughAllowed(metrics);
        registerDisplayMethodDistribution(metrics);
        registerRenderModePreference(metrics);
    }

    // -------------------------------------------------------------------------
    // Chart 1 – Region type distribution of currently active renders
    // -------------------------------------------------------------------------

    /**
     * AdvancedPie: "region_type_usage"
     *
     * <p>Reports how many player selections of each region type are actively being
     * rendered on this server instance at the moment bStats collects data.
     * Iterating {@link RenderManager#getMainRenderers()} is safe from the bStats
     * async thread because the underlying map is a {@link java.util.concurrent.ConcurrentHashMap}.
     * Only the per-renderer {@code instanceof} check happens – no Bukkit API is called.
     */
    private void registerRegionTypeUsage(Metrics metrics) {
        metrics.addCustomChart(new AdvancedPie("region_type_usage", () -> {
            RenderManager rm = plugin.getRenderManager();
            if (rm == null) return null;

            Map<String, Integer> counts = new HashMap<>();
            for (RegionRenderer<?> renderer : rm.getMainRenderers().values()) {
                String type;
                if (renderer instanceof CuboidRenderer)      type = "Cuboid";
                else if (renderer instanceof CylinderRenderer)    type = "Cylinder";
                else if (renderer instanceof EllipsoidRenderer)   type = "Ellipsoid";
                else if (renderer instanceof PolygonRenderer)     type = "Polygon";
                else if (renderer instanceof PolyhedronRenderer)  type = "Polyhedron";
                else                                               type = "Unknown";

                counts.merge(type, 1, Integer::sum);
            }

            // Return null when there are no active renderers so bStats skips the
            // submission rather than recording an empty data-point.
            return counts.isEmpty() ? null : counts;
        }));
    }

    // -------------------------------------------------------------------------
    // Chart 2 – WorldEdit variant installed on the server
    // -------------------------------------------------------------------------

    /**
     * SimplePie: "worldedit_variant"
     *
     * <p>Detects whether FastAsyncWorldEdit (FAWE) or the standard WorldEdit
     * distribution is installed. Plugin-manager lookups are safe from any thread.
     */
    private void registerWorldEditVariant(Metrics metrics) {
        metrics.addCustomChart(new SimplePie("worldedit_variant", () -> {
            if (Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
                return "FastAsyncWorldEdit";
            }
            if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
                return "WorldEdit";
            }
            return "Unknown";
        }));
    }

    // -------------------------------------------------------------------------
    // Chart 3 – Which region types have fill rendering enabled (server config)
    // -------------------------------------------------------------------------

    /**
     * AdvancedPie: "fill_enabled_per_type"
     *
     * <p>Shows the administrator's fill-rendering configuration per region type.
     * Only types with fill actually enabled are included so that the chart cleanly
     * shows the adoption rate of the fill feature rather than a crowded 50/50 split.
     */
    private void registerFillEnabledPerType(Metrics metrics) {
        metrics.addCustomChart(new AdvancedPie("fill_enabled_per_type", () -> {
            RenderSettings rs = plugin.getRenderSettings();
            if (rs == null) return null;

            Map<String, Integer> enabled = new HashMap<>();
            if (rs.isCuboidFillEnabled())     enabled.put("Cuboid",     1);
            if (rs.isCylinderFillEnabled())   enabled.put("Cylinder",   1);
            if (rs.isEllipsoidFillEnabled())  enabled.put("Ellipsoid",  1);
            if (rs.isPolygonFillEnabled())    enabled.put("Polygon",    1);
            if (rs.isPolyhedronFillEnabled()) enabled.put("Polyhedron", 1);

            return enabled.isEmpty() ? null : enabled;
        }));
    }

    // -------------------------------------------------------------------------
    // Chart 4 – Whether the server allows players to use see-through rendering
    // -------------------------------------------------------------------------

    /**
     * SimplePie: "see_through_allowed"
     *
     * <p>Reflects the {@code player_limits.see_through_allowed} server config value.
     */
    private void registerSeeThroughAllowed(Metrics metrics) {
        metrics.addCustomChart(new SimplePie("see_through_allowed", () -> {
            RenderSettings rs = plugin.getRenderSettings();
            if (rs == null) return "Unknown";
            return rs.isSeeThroughAllowed() ? "Allowed" : "Disabled";
        }));
    }

    // -------------------------------------------------------------------------
    // Chart 5 – How many online players are using CUI vs built-in display
    // -------------------------------------------------------------------------

    /**
     * AdvancedPie: "display_method_distribution"
     *
     * <p>Reports how many online players are currently receiving selections
     * through WorldEditCUI (client mod) versus the built-in WorldEditDisplay
     * renderer, further split into TextDisplay and particle fallback.
     * Players with both CUI disabled and rendering disabled are counted as
     * "Disabled".
     */
    private void registerDisplayMethodDistribution(Metrics metrics) {
        metrics.addCustomChart(new AdvancedPie("display_method_distribution", () -> {
            Map<String, Integer> counts = new HashMap<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = PlayerData.getPlayerData(player);
                if (data.isNativeCuiActive()) {
                    counts.merge("WorldEditCUI", 1, Integer::sum);
                } else if (data.isRenderingEnabled()) {
                    if (data.isParticleFallback()) {
                        counts.merge("Particle", 1, Integer::sum);
                    } else {
                        counts.merge("TextDisplay", 1, Integer::sum);
                    }
                } else {
                    counts.merge("Disabled", 1, Integer::sum);
                }
            }
            return counts.isEmpty() ? null : counts;
        }));
    }

    // -------------------------------------------------------------------------
    // Chart 6 – Render mode preference distribution
    // -------------------------------------------------------------------------

    /**
     * AdvancedPie: "render_mode_preference"
     *
     * <p>Reports the distribution of player render mode preferences:
     * AUTO, TEXT_DISPLAY, or PARTICLE.
     */
    private void registerRenderModePreference(Metrics metrics) {
        metrics.addCustomChart(new AdvancedPie("render_mode_preference", () -> {
            Map<String, Integer> counts = new HashMap<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = PlayerData.getPlayerData(player);
                counts.merge(data.getRenderMode().name(), 1, Integer::sum);
            }
            return counts.isEmpty() ? null : counts;
        }));
    }
}
