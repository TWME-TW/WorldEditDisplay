package dev.twme.worldeditdisplay.display;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.config.SharedRenderSettings;
import dev.twme.worldeditdisplay.display.particle.ParticleCuboidRenderer;
import dev.twme.worldeditdisplay.display.particle.ParticleCylinderRenderer;
import dev.twme.worldeditdisplay.display.particle.ParticleEllipsoidRenderer;
import dev.twme.worldeditdisplay.display.particle.ParticlePolygonRenderer;
import dev.twme.worldeditdisplay.display.particle.ParticlePolyhedronRenderer;
import dev.twme.worldeditdisplay.display.particle.ParticleRenderer;
import dev.twme.worldeditdisplay.display.renderer.CuboidRenderer;
import dev.twme.worldeditdisplay.display.renderer.CylinderRenderer;
import dev.twme.worldeditdisplay.display.renderer.EllipsoidRenderer;
import dev.twme.worldeditdisplay.display.renderer.PolygonRenderer;
import dev.twme.worldeditdisplay.display.renderer.PolyhedronRenderer;
import dev.twme.worldeditdisplay.display.renderer.RegionRenderer;
import dev.twme.worldeditdisplay.display.renderer.RegionRenderer.RetainedLineStats;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.region.BoundingBox;
import dev.twme.worldeditdisplay.region.CuboidRegion;
import dev.twme.worldeditdisplay.region.CylinderRegion;
import dev.twme.worldeditdisplay.region.EllipsoidRegion;
import dev.twme.worldeditdisplay.region.PolygonRegion;
import dev.twme.worldeditdisplay.region.PolyhedronRegion;
import dev.twme.worldeditdisplay.region.Region;
import dev.twme.worldeditdisplay.region.Vector3;
import dev.twme.worldeditdisplay.share.ShareManager;
import dev.twme.worldeditdisplay.util.MessageUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import io.github.retrooper.packetevents.util.folia.TaskWrapper;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;

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
    /** viewer → (sharer → label entity): name-tag labels shown above shared selections */
    private final Map<UUID, Map<UUID, WrapperEntity>> labelEntities;
    /** sharer → colour: stable shared-selection colour assignments across all viewers */
    private final Map<UUID, Color> sharedColors;
    /**
     * sharerId → last Adventure Component used for label text.
     * Built from (sharer name + shared colour); reused across all viewers until the name changes.
     */
    private final Map<UUID, net.kyori.adventure.text.Component> labelComponentCache;
    /** sharerId → last player name used to build labelComponentCache entry (invalidation key). */
    private final Map<UUID, String> labelComponentNames;

    // ─── Particle fallback renderers (for clients < 1.19.4) ────────────────
    private final Map<UUID, ParticleRenderer> mainParticleRenderers;
    private final Map<UUID, Map<UUID, ParticleRenderer>> multiParticleRenderers;
    /** viewer → (sharer → renderer) */
    private final Map<UUID, Map<UUID, ParticleRenderer>> sharedParticleRenderers;
    private TaskWrapper particleTickTask;

    @FunctionalInterface
    private interface RendererFactory {
        RegionRenderer create(Player player, PlayerRenderSettings settings);
    }
    private final Map<Class<? extends Region>, RendererFactory> rendererFactories;

    @FunctionalInterface
    private interface ParticleRendererFactory {
        ParticleRenderer create(Player player, PlayerRenderSettings settings);
    }
    private final Map<Class<? extends Region>, ParticleRendererFactory> particleRendererFactories;

    private static final int SHARED_COLOR_ALPHA = 230;
    private static final float SHARED_MIN_HUE_DISTANCE = 0.12f;
    private static final float SHARED_HUE_STEP = 0.61803398875f;
    private static final int SHARED_COLOR_ATTEMPTS = 12;

    private TaskWrapper rebaseTask;

    public RenderManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
        this.mainRenderers = new ConcurrentHashMap<>();
        this.multiRenderers = new ConcurrentHashMap<>();
        this.sharedRenderers = new ConcurrentHashMap<>();
        this.labelEntities = new ConcurrentHashMap<>();
        this.sharedColors = new ConcurrentHashMap<>();
        this.labelComponentCache = new ConcurrentHashMap<>();
        this.labelComponentNames = new ConcurrentHashMap<>();
        this.rendererFactories = new HashMap<>();

        this.mainParticleRenderers = new ConcurrentHashMap<>();
        this.multiParticleRenderers = new ConcurrentHashMap<>();
        this.sharedParticleRenderers = new ConcurrentHashMap<>();
        this.particleRendererFactories = new HashMap<>();

        registerRendererFactories();
        registerParticleRendererFactories();
        startRebaseTask();
        startParticleTickTask();
        plugin.getLogger().info("RenderManager started");
    }

    private void registerRendererFactories() {
        rendererFactories.put(CuboidRegion.class,     (p, s) -> new CuboidRenderer(plugin, p, s));
        rendererFactories.put(PolygonRegion.class,    (p, s) -> new PolygonRenderer(plugin, p, s));
        rendererFactories.put(EllipsoidRegion.class,  (p, s) -> new EllipsoidRenderer(plugin, p, s));
        rendererFactories.put(CylinderRegion.class,   (p, s) -> new CylinderRenderer(plugin, p, s));
        rendererFactories.put(PolyhedronRegion.class, (p, s) -> new PolyhedronRenderer(plugin, p, s));

        plugin.getLogger().info("renderer types registered: " + rendererFactories.size());
    }

    private void registerParticleRendererFactories() {
        particleRendererFactories.put(CuboidRegion.class,     (p, s) -> new ParticleCuboidRenderer(plugin, p, s));
        particleRendererFactories.put(CylinderRegion.class,   (p, s) -> new ParticleCylinderRenderer(plugin, p, s));
        particleRendererFactories.put(EllipsoidRegion.class,  (p, s) -> new ParticleEllipsoidRenderer(plugin, p, s));
        particleRendererFactories.put(PolygonRegion.class,    (p, s) -> new ParticlePolygonRenderer(plugin, p, s));
        particleRendererFactories.put(PolyhedronRegion.class, (p, s) -> new ParticlePolyhedronRenderer(plugin, p, s));
        plugin.getLogger().info("particle renderer types registered: " + particleRendererFactories.size());
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

        if (playerData.isParticleFallback()) {
            updateParticleMainSelection(player, playerId, playerData.getSelection());
            updateParticleMultiSelections(player, playerId, playerData.getMultiRegions());
            // Shared selections for particle fallback viewers are handled in notifyViewersOfSharer
            // via updateSharedSelections, which already branches inside.
        } else {
            updateMainSelection(player, playerId, playerData.getSelection());
            updateMultiSelections(player, playerId, playerData.getMultiRegions());
        }
        updateSharedSelections(player, playerId);

        // When this player's own selection changes, update renderers for all viewers
        notifyViewersOfSharer(player);

        if (playerData.isDebugEnabled()) {
            if (playerData.isParticleFallback()) {
                int particleCount = getParticlePointCount(playerId);
                MessageUtil.sendTranslated(player, "command.wedisplay.debug.particle_count", particleCount);
            } else {
                int entityCount = getPlayerEntityCount(playerId);
                MessageUtil.sendTranslated(player, "command.wedisplay.debug.entity_count", entityCount);
                int retainedLineCount = getPlayerRetainedLineCount(playerId);
                int retainedLineEntityCount = getPlayerRetainedLineEntityCount(playerId);
                MessageUtil.sendTranslated(player, "command.wedisplay.debug.retained_line_count", retainedLineCount, retainedLineEntityCount);
                RetainedLineStats retainedLineStats = getPlayerRetainedLineStats(playerId);
                MessageUtil.sendTranslated(player, "command.wedisplay.debug.retained_line_pass",
                        retainedLineStats.reusedLines(), retainedLineStats.spawnedLines(), retainedLineStats.removedLines());
            }
        }
    }

    /**
     * Returns the total number of particle points currently cached for this player
     * across main and multi particle renderers.
     */
    public int getParticlePointCount(UUID playerId) {
        int count = 0;
        ParticleRenderer main = mainParticleRenderers.get(playerId);
        if (main != null) {
            count += main.getPointCount();
        }
        Map<UUID, ParticleRenderer> multi = multiParticleRenderers.get(playerId);
        if (multi != null) {
            for (ParticleRenderer r : multi.values()) {
                count += r.getPointCount();
            }
        }
        return count;
    }

    /**
     * Renders the selections of all players that {@code viewer} is watching.
     * Includes both active-share players and viewall-sourced players.
     */
    private void updateSharedSelections(Player viewer, UUID viewerId) {
        ShareManager shareManager = plugin.getShareManager();
        if (shareManager == null) return;

        PlayerData viewerData = PlayerData.getPlayerData(viewer);
        boolean viewerIsParticle = viewerData != null && viewerData.isParticleFallback();

        Set<UUID> sharers = resolveVisibleSharers(viewer, viewerId);

        if (viewerIsParticle) {
            updateSharedParticleSelections(viewer, viewerId, sharers, shareManager);
        } else {
            updateSharedTextDisplaySelections(viewer, viewerId, sharers, shareManager);
        }
    }

    /** Shared-selection path for TextDisplay viewers (>= 1.19.4). */
    private void updateSharedTextDisplaySelections(Player viewer, UUID viewerId,
                                                    Set<UUID> sharers, ShareManager shareManager) {
        Map<UUID, RegionRenderer> viewerSharedRenderers =
                sharedRenderers.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());

        viewerSharedRenderers.entrySet().removeIf(e -> {
            if (!sharers.contains(e.getKey())) {
                e.getValue().clear();
                releaseSharedColorIfUnused(e.getKey());
                return true;
            }
            return false;
        });

        Vector3 viewerPos = null;
        for (UUID sharerId : sharers) {
            if (!shareManager.isActiveShare(sharerId, viewerId)) {
                if (viewerPos == null) viewerPos = Vector3.from(viewer.getLocation());
                if (!shouldRenderForViewAll(viewer, sharerId, viewerPos)) continue;
            }
            Color sharedColor = getOrCreateSharedColor(sharerId);
            renderSharedForViewer(viewer, viewerSharedRenderers, sharerId, sharedColor, false);
        }
    }

    /** Shared-selection path for particle-fallback viewers (< 1.19.4). */
    private void updateSharedParticleSelections(Player viewer, UUID viewerId,
                                                 Set<UUID> sharers, ShareManager shareManager) {
        Map<UUID, ParticleRenderer> viewerShared =
                sharedParticleRenderers.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());

        viewerShared.entrySet().removeIf(e -> {
            if (!sharers.contains(e.getKey())) {
                e.getValue().clear();
                releaseSharedColorIfUnused(e.getKey());
                return true;
            }
            return false;
        });

        int maxParticleDist = plugin.getRenderSettings().getParticleMaxRenderDistance();

        Vector3 viewerPos = null;
        for (UUID sharerId : sharers) {
            Player sharer = Bukkit.getPlayer(sharerId);
            if (sharer == null || !sharer.isOnline()) continue;

            // Distance check: apply max_render_distance as an absolute cap for ALL particle shares
            if (viewerPos == null) viewerPos = Vector3.from(viewer.getLocation());
            PlayerData sharerData = PlayerData.getPlayerData(sharer);
            Region sharerRegion = sharerData != null ? sharerData.getSelection() : null;

            double distance;
            if (sharerRegion != null && sharerRegion.getBoundingBox() != null) {
                distance = sharerRegion.getBoundingBox().distanceTo(viewerPos);
            } else {
                distance = viewer.getLocation().distance(sharer.getLocation());
            }

            if (distance > maxParticleDist) continue;

            // For viewall-sourced players, also apply viewall distance-based loading
            if (!shareManager.isActiveShare(sharerId, viewerId)) {
                if (!shouldRenderForViewAll(viewer, sharerId, viewerPos)) continue;
            }

            Color sharedColor = getOrCreateSharedColor(sharerId);
            renderSharedParticleForViewer(viewer, viewerShared, sharerId, sharedColor, false);
        }
    }

    /**
     * Returns the set of sharers whose selection should be rendered for {@code viewer}.
     * Includes active-share sources always, plus viewall-sourced players when viewall is enabled.
     */
    private Set<UUID> resolveVisibleSharers(Player viewer, UUID viewerId) {
        ShareManager shareManager = plugin.getShareManager();
        // getActiveSharers already returns a defensive copy – reuse it directly
        Set<UUID> result = shareManager.getActiveSharers(viewerId);

        PlayerData viewerData = PlayerData.getPlayerData(viewer);
        if (viewerData != null && viewerData.isViewAllEnabled()
                && viewer.hasPermission("worldeditdisplay.use.view")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(viewer)) continue;
                if (viewerData.isViewAllHidden(online.getUniqueId())) continue;
                result.add(online.getUniqueId());
            }
        }
        return result;
    }

    /**
     * Applies world + distance-based loading filter for viewall renders.
     * Returns {@code true} if the viewer should see the sharer's selection.
     *
     * Logic:
     *  1. sharer must be online in the same world.
     *  2. Get the sharer's selection AABB.  If not available, fall back to player position.
     *  3. If the viewer is INSIDE the AABB → always show.
     *  4. Otherwise, compute the viewer's distance to the nearest surface of the AABB.
     *     Show if that distance ≤ effectiveDist,
     *     where effectiveDist = max(halfDiagonal × sizeMultiplier, minDistance).
     */
    private boolean shouldRenderForViewAll(Player viewer, UUID sharerId, Vector3 viewerPos) {
        dev.twme.worldeditdisplay.config.RenderSettings rs = plugin.getRenderSettings();
        if (!rs.isViewAllDistanceBasedEnabled()) return true;

        Player sharer = Bukkit.getPlayer(sharerId);
        if (sharer == null || !sharer.isOnline()) return false;
        if (!viewer.getWorld().equals(sharer.getWorld())) return false;

        double minDist = rs.getViewAllMinDistance();
        double multiplier = rs.getViewAllSizeMultiplier();

        // Try to get the selection bounding box
        PlayerData sharerData = PlayerData.getPlayerData(sharer);
        dev.twme.worldeditdisplay.region.BoundingBox box =
                (sharerData != null && sharerData.getSelection() != null)
                ? sharerData.getSelection().getBoundingBox()
                : null;

        if (box == null) {
            // Fallback: treat sharer position as a point
            return viewer.getLocation().distance(sharer.getLocation()) <= minDist;
        }

        // Inside the AABB → always visible
        if (box.contains(viewerPos)) return true;

        // effectiveDist: at least minDist, but scales with selection size
        double effectiveDist = Math.max(box.getHalfDiagonal() * multiplier, minDist);
        return box.distanceTo(viewerPos) <= effectiveDist;
    }

    /**
     * When {@code sharer}'s own selection changes, push updates to every active viewer
     * and to any viewall-enabled viewer who has not hidden this sharer.
     */
    private void notifyViewersOfSharer(Player sharer) {
        ShareManager shareManager = plugin.getShareManager();
        if (shareManager == null) return;

        // Active share viewers
        Set<UUID> viewers = shareManager.getActiveViewers(sharer.getUniqueId());

        // Viewall-enabled viewers (tracked in plugin)
        Set<UUID> viewAllSet = plugin.getViewAllPlayers();

        Set<UUID> combined = new java.util.HashSet<>(viewers);
        combined.addAll(viewAllSet);
        combined.remove(sharer.getUniqueId()); // 玩家不應透過 shared 渲染看到自己的選區

        if (combined.isEmpty()) return;

        for (UUID viewerId : combined) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) continue;

            // For viewall-only viewers (not active share), check the hidden list
            if (!shareManager.isActiveShare(sharer.getUniqueId(), viewerId)) {
                PlayerData viewerData = PlayerData.getPlayerData(viewer);
                if (viewerData != null && viewerData.isViewAllHidden(sharer.getUniqueId())) continue;
            }

            Color color = getOrCreateSharedColor(sharer.getUniqueId());
            PlayerData viewerData = PlayerData.getPlayerData(viewer);
            boolean viewerIsParticle = viewerData != null && viewerData.isParticleFallback();

            if (viewerIsParticle) {
                Map<UUID, ParticleRenderer> viewerShared =
                        sharedParticleRenderers.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());
                renderSharedParticleForViewer(viewer, viewerShared, sharer.getUniqueId(), color, true);
            } else {
                Map<UUID, RegionRenderer> viewerSharedRenderers =
                        sharedRenderers.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());
                renderSharedForViewer(viewer, viewerSharedRenderers, sharer.getUniqueId(), color, true);
            }
        }
    }

    /**
     * Render the sharer's main selection for the viewer.
     *
     * @param forceRender when {@code true} the region is rendered unconditionally (used when the
     *                    sharer's own selection just changed); when {@code false} the render is
     *                    skipped if the sharer's region is not dirty and a renderer already exists
     *                    (used when the viewer's own selection changed – the sharer's data is stale).
     */
    private void renderSharedForViewer(Player viewer, Map<UUID, RegionRenderer> viewerSharedRenderers,
                                       UUID sharerId, Color sharedColor, boolean forceRender) {
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

        // Skip rendering if the sharer's region hasn't changed and we already have a renderer
        if (!forceRender && renderer != null && !sharerRegion.isDirty()) {
            // Still refresh the label so toggling showLabels takes effect immediately
            updateSharedLabel(viewer, viewer.getUniqueId(), sharerId, sharedColor, sharerPlayer, sharerRegion);
            return;
        }

        SharedRenderSettings sharedSettings = new SharedRenderSettings(plugin, sharedColor);

        // Always refresh the label (handles toggle on/off and position updates)
        updateSharedLabel(viewer, viewer.getUniqueId(), sharerId, sharedColor, sharerPlayer, sharerRegion);

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
     * Particle-fallback equivalent of {@link #renderSharedForViewer}.
     * Renders the sharer's region as coloured particles for a low-version viewer.
     * All particles use REDSTONE DustOptions with the shared colour.
     */
    private void renderSharedParticleForViewer(Player viewer, Map<UUID, ParticleRenderer> viewerShared,
                                                UUID sharerId, Color sharedColor, boolean forceRender) {
        Player sharerPlayer = Bukkit.getPlayer(sharerId);
        if (sharerPlayer == null || !sharerPlayer.isOnline()) {
            ParticleRenderer r = viewerShared.remove(sharerId);
            if (r != null) r.clear();
            releaseSharedColorIfUnused(sharerId);
            return;
        }

        PlayerData sharerData = PlayerData.getPlayerData(sharerPlayer);
        if (sharerData == null) return;

        Region sharerRegion = sharerData.getSelection();
        if (sharerRegion == null) {
            ParticleRenderer r = viewerShared.remove(sharerId);
            if (r != null) r.clear();
            releaseSharedColorIfUnused(sharerId);
            return;
        }

        ParticleRenderer renderer = viewerShared.get(sharerId);

        // Replace renderer if region type changed
        if (renderer != null && !renderer.getRegionType().equals(sharerRegion.getClass())) {
            renderer.clear();
            viewerShared.remove(sharerId);
            renderer = null;
        }

        if (!forceRender && renderer != null && !sharerRegion.isDirty()) {
            return;
        }

        if (renderer == null) {
            renderer = createParticleRenderer(viewer, sharerRegion);
            if (renderer != null) {
                renderer.setSharedColor(sharedColor);
                renderer.addViewer(viewer.getUniqueId());
                viewerShared.put(sharerId, renderer);
            } else {
                return;
            }
        }

        try {
            renderer.render(sharerRegion);
            renderer.postRender();
            // Do NOT clear dirty here – the sharer's own updateRender handles that
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "shared particle render fail for viewer "
                    + viewer.getName(), e);
        }
    }

    /**
     * Generate a stable, high-variance shared-selection colour from a sharer's UUID.
     * The hash controls hue, saturation, and brightness so the output space is much larger
     * than the original fixed palette while remaining deterministic.
     */
    private Color getOrCreateSharedColor(UUID sharerId) {
        return sharedColors.computeIfAbsent(sharerId,
                key -> createSharedColor(key, sharedColors.values()));
    }

    private void releaseSharedColorIfUnused(UUID sharerId) {
        ShareManager shareManager = plugin.getShareManager();
        if (shareManager == null) return;
        if (!shareManager.getActiveViewers(sharerId).isEmpty()) return;
        sharedColors.remove(sharerId);
        labelComponentCache.remove(sharerId);
        labelComponentNames.remove(sharerId);
    }

    private Color createSharedColor(UUID sharerId, Collection<Color> existingColors) {
        long mixed = sharerId.getMostSignificantBits() ^ Long.rotateLeft(sharerId.getLeastSignificantBits(), 32);
        int hash = (int) (mixed ^ (mixed >>> 32));

        float baseHue = (hash & 0xFFFF) / 65536.0f;
        float saturation = 0.93f + (((hash >>> 16) & 0x07) / 100.0f);
        float brightness = 0.75f + (((hash >>> 19) & 0x07) / 200.0f);

        float hue = baseHue;
        Color bestColor = null;
        float bestDistance = -1.0f;

        for (int attempt = 0; attempt < SHARED_COLOR_ATTEMPTS; attempt++) {
            Color candidate = createSharedColor(hue, saturation, brightness);
            float distance = minimumHueDistance(candidate, existingColors);
            if (distance > bestDistance) {
                bestDistance = distance;
                bestColor = candidate;
            }
            if (existingColors.isEmpty() || distance >= SHARED_MIN_HUE_DISTANCE) {
                return candidate;
            }
            hue = wrapHue(hue + SHARED_HUE_STEP);
        }

        return bestColor != null ? bestColor : createSharedColor(baseHue, saturation, brightness);
    }

    private Color createSharedColor(float hue, float saturation, float brightness) {
        int rgb = java.awt.Color.HSBtoRGB(
                wrapHue(hue),
                Math.min(saturation, 1.0f),
                Math.min(brightness, 1.0f));
        return Color.fromARGB(SHARED_COLOR_ALPHA, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private float minimumHueDistance(Color candidate, Collection<Color> existingColors) {
        float candidateHue = hueOf(candidate);
        float minimumDistance = Float.MAX_VALUE;

        for (Color existing : existingColors) {
            float distance = hueDistance(candidateHue, hueOf(existing));
            if (distance < minimumDistance) {
                minimumDistance = distance;
            }
        }

        return minimumDistance == Float.MAX_VALUE ? 1.0f : minimumDistance;
    }

    private float hueOf(Color color) {
        return java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
    }

    private float hueDistance(float first, float second) {
        float distance = Math.abs(first - second);
        return Math.min(distance, 1.0f - distance);
    }

    private float wrapHue(float hue) {
        float wrapped = hue % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }

    // ─── Label management ─────────────────────────────────────────────────────

    /**
     * Creates or updates a floating name-tag label above {@code sharerRegion} visible only to
     * {@code viewer}.  If the viewer has {@code showLabels} disabled the label is removed.
     */
    private void updateSharedLabel(Player viewer, UUID viewerId, UUID sharerId,
                                   Color sharedColor, Player sharerPlayer, Region sharerRegion) {
        PlayerData viewerData = PlayerData.getPlayerData(viewer);
        if (viewerData == null || !viewerData.isShowLabels()) {
            clearSharedLabel(viewerId, sharerId);
            return;
        }

        BoundingBox box = sharerRegion.getBoundingBox();
        if (box == null) {
            clearSharedLabel(viewerId, sharerId);
            return;
        }
        Vector3 center = box.getCenter();
        Location labelLoc = new Location(sharerPlayer.getWorld(),
                center.getX(), center.getY(), center.getZ());

        net.kyori.adventure.text.Component nameText;
        boolean nameChanged;
        String sharerName = sharerPlayer.getName();
        // Re-use the cached component when the sharer's name hasn't changed.
        // sharedColor is stable per-sharer, so name is the only invalidation key.
        if (sharerName.equals(labelComponentNames.get(sharerId))
                && (nameText = labelComponentCache.get(sharerId)) != null) {
            nameChanged = false;
        } else {
            nameText = net.kyori.adventure.text.Component
                    .text(sharerName)
                    .color(net.kyori.adventure.text.format.TextColor.color(
                            sharedColor.getRed(), sharedColor.getGreen(), sharedColor.getBlue()));
            labelComponentNames.put(sharerId, sharerName);
            labelComponentCache.put(sharerId, nameText);
            nameChanged = true;
        }

        Map<UUID, WrapperEntity> viewerLabels =
                labelEntities.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());
        WrapperEntity existingLabel = viewerLabels.get(sharerId);

        if (existingLabel != null) {
            // Only send a metadata packet when the sharer's name was rebuilt this frame.
            if (nameChanged && existingLabel.getEntityMeta() instanceof TextDisplayMeta meta) {
                meta.setText(nameText);
            }
            existingLabel.teleport(SpigotConversionUtil.fromBukkitLocation(labelLoc));
            return;
        }

        WrapperEntity label = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        label.spawn(SpigotConversionUtil.fromBukkitLocation(labelLoc));
        if (label.getEntityMeta() instanceof TextDisplayMeta meta) {
            // Derive a dark, semi-opaque background from the sharer's shared colour
            int bgColor = (0xC0 << 24)
                    | ((int) (sharedColor.getRed()   * 0.25) << 16)
                    | ((int) (sharedColor.getGreen() * 0.25) <<  8)
                    |  (int) (sharedColor.getBlue()  * 0.25);
            meta.setText(nameText);
            meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
            meta.setScale(new Vector3f(2.0f, 2.0f, 2.0f));
            meta.setSeeThrough(true);
            meta.setViewRange(64.0f);
            meta.setBrightnessOverride(15 << 4 | 15 << 20);
            meta.setBackgroundColor(bgColor);
        }
        label.addViewer(viewerId);
        viewerLabels.put(sharerId, label);
    }

    /** Remove the label entity a specific viewer has for a specific sharer. */
    private void clearSharedLabel(UUID viewerId, UUID sharerId) {
        Map<UUID, WrapperEntity> viewerLabels = labelEntities.get(viewerId);
        if (viewerLabels == null) return;
        WrapperEntity label = viewerLabels.remove(sharerId);
        if (label != null) label.remove();
        if (viewerLabels.isEmpty()) labelEntities.remove(viewerId);
    }

    /** Remove all label entities for a specific viewer. */
    public void clearSharedLabels(UUID viewerId) {
        Map<UUID, WrapperEntity> viewerLabels = labelEntities.remove(viewerId);
        if (viewerLabels != null) {
            viewerLabels.values().forEach(WrapperEntity::remove);
            viewerLabels.clear();
        }
    }

    /** Clear all shared renderers for a specific viewer. */
    public void clearSharedRenders(UUID viewerId) {
        // TextDisplay shared renderers
        Map<UUID, RegionRenderer> map = sharedRenderers.remove(viewerId);
        if (map != null) {
            Set<UUID> sharerIds = Set.copyOf(map.keySet());
            map.values().forEach(RegionRenderer::clear);
            map.clear();
            for (UUID sharerId : sharerIds) {
                releaseSharedColorIfUnused(sharerId);
            }
        }
        // Particle shared renderers
        Map<UUID, ParticleRenderer> particleMap = sharedParticleRenderers.remove(viewerId);
        if (particleMap != null) {
            Set<UUID> sharerIds = Set.copyOf(particleMap.keySet());
            particleMap.values().forEach(ParticleRenderer::clear);
            particleMap.clear();
            for (UUID sharerId : sharerIds) {
                releaseSharedColorIfUnused(sharerId);
            }
        }
        clearSharedLabels(viewerId);
    }

    /**
     * Called when a sharer goes offline. Removes their selection rendering from ALL viewers
     * and releases associated colour / component cache state.
     */
    public void clearSharerRenders(UUID sharerId) {
        // TextDisplay shared renderers
        for (Map.Entry<UUID, Map<UUID, RegionRenderer>> entry : sharedRenderers.entrySet()) {
            UUID viewerId = entry.getKey();
            Map<UUID, RegionRenderer> viewerMap = entry.getValue();
            RegionRenderer renderer = viewerMap.remove(sharerId);
            if (renderer != null) renderer.clear();
            if (viewerMap.isEmpty()) sharedRenderers.remove(viewerId);
            clearSharedLabel(viewerId, sharerId);
        }
        // Particle shared renderers
        for (Map.Entry<UUID, Map<UUID, ParticleRenderer>> entry : sharedParticleRenderers.entrySet()) {
            UUID viewerId = entry.getKey();
            Map<UUID, ParticleRenderer> viewerMap = entry.getValue();
            ParticleRenderer renderer = viewerMap.remove(sharerId);
            if (renderer != null) renderer.clear();
            if (viewerMap.isEmpty()) sharedParticleRenderers.remove(viewerId);
        }
        // The sharer is offline – release colour and component cache unconditionally.
        sharedColors.remove(sharerId);
        labelComponentCache.remove(sharerId);
        labelComponentNames.remove(sharerId);
    }

    /** Clear the shared renderer a specific viewer has for a specific sharer. */
    public void clearSharedRender(UUID viewerId, UUID sharerId) {
        // TextDisplay
        Map<UUID, RegionRenderer> map = sharedRenderers.get(viewerId);
        if (map != null) {
            RegionRenderer r = map.remove(sharerId);
            if (r != null) r.clear();
            if (map.isEmpty()) {
                sharedRenderers.remove(viewerId);
            }
        }
        // Particle
        Map<UUID, ParticleRenderer> particleMap = sharedParticleRenderers.get(viewerId);
        if (particleMap != null) {
            ParticleRenderer r = particleMap.remove(sharerId);
            if (r != null) r.clear();
            if (particleMap.isEmpty()) {
                sharedParticleRenderers.remove(viewerId);
            }
        }
        releaseSharedColorIfUnused(sharerId);
        clearSharedLabel(viewerId, sharerId);
    }

    /**
     * Clear all viewall-only renders for a viewer (i.e. renderers that were added via viewall
     * mode but NOT backed by an active share relationship).
     */
    public void clearViewAllRenders(UUID viewerId) {
        ShareManager shareManager = plugin.getShareManager();
        // TextDisplay
        Map<UUID, RegionRenderer> map = sharedRenderers.get(viewerId);
        if (map != null) {
            map.entrySet().removeIf(e -> {
                UUID sharerId = e.getKey();
                if (shareManager != null && shareManager.isActiveShare(sharerId, viewerId)) return false;
                e.getValue().clear();
                releaseSharedColorIfUnused(sharerId);
                clearSharedLabel(viewerId, sharerId);
                return true;
            });
            if (map.isEmpty()) sharedRenderers.remove(viewerId);
        }
        // Particle
        Map<UUID, ParticleRenderer> particleMap = sharedParticleRenderers.get(viewerId);
        if (particleMap != null) {
            particleMap.entrySet().removeIf(e -> {
                UUID sharerId = e.getKey();
                if (shareManager != null && shareManager.isActiveShare(sharerId, viewerId)) return false;
                e.getValue().clear();
                releaseSharedColorIfUnused(sharerId);
                return true;
            });
            if (particleMap.isEmpty()) sharedParticleRenderers.remove(viewerId);
        }
    }

    /**
     * Clear a single viewall-sourced render. Only clears if NOT an active share.
     */
    public void clearViewAllRender(UUID viewerId, UUID sharerId) {
        ShareManager shareManager = plugin.getShareManager();
        if (shareManager != null && shareManager.isActiveShare(sharerId, viewerId)) return;
        clearSharedRender(viewerId, sharerId);
        // Also clear particle shared render if present
        Map<UUID, ParticleRenderer> particleMap = sharedParticleRenderers.get(viewerId);
        if (particleMap != null) {
            ParticleRenderer r = particleMap.remove(sharerId);
            if (r != null) r.clear();
            if (particleMap.isEmpty()) sharedParticleRenderers.remove(viewerId);
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

    private void updateParticleMainSelection(Player player, UUID playerId, Region mainSelection) {
        // If the player switched from particle to TextDisplay (e.g. ViaVersion now reports >=1.19.4),
        // ensure any stale particle renderer is cleaned up.
        if (mainSelection == null || !player.isOnline()) {
            clearParticleMainRender(playerId);
            return;
        }

        ParticleRenderer currentRenderer = mainParticleRenderers.get(playerId);

        if (currentRenderer != null && !currentRenderer.getRegionType().equals(mainSelection.getClass())) {
            currentRenderer.clear();
            mainParticleRenderers.remove(playerId);
            currentRenderer = null;
        }

        if (currentRenderer == null) {
            currentRenderer = createParticleRenderer(player, mainSelection);
            if (currentRenderer != null) mainParticleRenderers.put(playerId, currentRenderer);
            else {
                plugin.getLogger().warning("cannot make particle renderer: " + mainSelection.getClass().getSimpleName());
                return;
            }
        } else if (!mainSelection.isDirty()) {
            return;
        }

        try {
            currentRenderer.render(mainSelection);
            currentRenderer.postRender();
            mainSelection.clearDirty();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "particle main render fail: " + player.getName(), e);
        }
    }

    /**
     * Remove the main particle renderer for a player (if any).
     */
    private void clearParticleMainRender(UUID playerId) {
        ParticleRenderer renderer = mainParticleRenderers.remove(playerId);
        if (renderer != null) renderer.clear();
    }

    // ─── Particle fallback: multi selections ─────────────────────────────

    private void updateParticleMultiSelections(Player player, UUID playerId, Map<UUID, Region> multiRegions) {
        Map<UUID, ParticleRenderer> playerMulti =
                multiParticleRenderers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        // remove stale regions
        playerMulti.keySet().removeIf(regionId -> {
            if (!multiRegions.containsKey(regionId)) {
                ParticleRenderer r = playerMulti.remove(regionId);
                if (r != null) r.clear();
                return true;
            }
            return false;
        });

        for (Map.Entry<UUID, Region> entry : multiRegions.entrySet()) {
            UUID regionId = entry.getKey();
            Region region = entry.getValue();
            if (region == null) continue;

            ParticleRenderer renderer = playerMulti.get(regionId);

            if (renderer != null && !renderer.getRegionType().equals(region.getClass())) {
                renderer.clear();
                playerMulti.remove(regionId);
                renderer = null;
            }

            if (renderer == null) {
                renderer = createParticleRenderer(player, region);
                if (renderer != null) playerMulti.put(regionId, renderer);
                else {
                    plugin.getLogger().warning("cannot make particle multi renderer: " + region.getClass().getSimpleName());
                    continue;
                }
            } else if (!region.isDirty()) {
                // renderer 已存在且 region 沒有變動，跳過
                continue;
            }

            try {
                renderer.render(region);
                renderer.postRender();
                region.clearDirty();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "multi render fail: " + player.getName(), e);
            }
        }
    }

    public void clearRender(UUID playerId) {
        // TextDisplay renderers
        RegionRenderer mainRenderer = mainRenderers.remove(playerId);
        if (mainRenderer != null) mainRenderer.clear();

        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.remove(playerId);
        if (playerMultiRenderers != null) {
            playerMultiRenderers.values().forEach(RegionRenderer::clear);
            playerMultiRenderers.clear();
        }

        // Particle fallback renderers
        clearParticleMainRender(playerId);
        Map<UUID, ParticleRenderer> playerMultiParticle = multiParticleRenderers.remove(playerId);
        if (playerMultiParticle != null) {
            playerMultiParticle.values().forEach(ParticleRenderer::clear);
            playerMultiParticle.clear();
        }

        // Clear shared renderers this player holds (selections they were watching as a viewer)
        clearSharedRenders(playerId);

        // Clear this player's own selection from all viewers who were watching it
        clearSharerRenders(playerId);
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
        // TextDisplay renderers
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
        sharedColors.clear();

        labelEntities.values().forEach(m -> m.values().forEach(WrapperEntity::remove));
        labelEntities.clear();

        // Particle fallback renderers
        mainParticleRenderers.values().forEach(ParticleRenderer::clear);
        mainParticleRenderers.clear();

        multiParticleRenderers.values().forEach(m -> {
            m.values().forEach(ParticleRenderer::clear);
            m.clear();
        });
        multiParticleRenderers.clear();

        sharedParticleRenderers.values().forEach(m -> {
            m.values().forEach(ParticleRenderer::clear);
            m.clear();
        });
        sharedParticleRenderers.clear();
    }

    private RegionRenderer createRenderer(Player player, Region region) {
        return createRenderer(player, region, plugin.getPlayerSettingsManager().getSettings(player.getUniqueId()));
    }

    private RegionRenderer createRenderer(Player player, Region region,
                                           PlayerRenderSettings settings) {
        RendererFactory factory = rendererFactories.get(region.getClass());
        if (factory == null) {
            plugin.getLogger().warning("renderer not found: " + region.getClass().getSimpleName());
            return null;
        }
        try {
            return factory.create(player, settings);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "cannot create renderer: " + region.getClass().getSimpleName(), e);
            return null;
        }
    }

    private ParticleRenderer createParticleRenderer(Player player, Region region) {
        return createParticleRenderer(player, region,
                plugin.getPlayerSettingsManager().getSettings(player.getUniqueId()));
    }

    private ParticleRenderer createParticleRenderer(Player player, Region region,
                                                     PlayerRenderSettings settings) {
        ParticleRendererFactory factory = particleRendererFactories.get(region.getClass());
        if (factory == null) {
            plugin.getLogger().warning("particle renderer not found: " + region.getClass().getSimpleName());
            return null;
        }
        try {
            ParticleRenderer renderer = factory.create(player, settings);
            if (renderer != null) {
                renderer.applyServerSettings(plugin.getRenderSettings());
            }
            return renderer;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "cannot create particle renderer: "
                    + region.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * Starts a periodic task that sends particle points to all active particle-fallback viewers.
     * Interval is read from the server config (particle_fallback.update_interval).
     */
    private void startParticleTickTask() {
        int interval = plugin.getRenderSettings().getParticleUpdateInterval();
        if (interval < 1) interval = 5;
        plugin.getLogger().info("Particle fallback tick interval: " + interval + " ticks");
        particleTickTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(plugin, ignored -> {
            // Main particle renderers
            for (ParticleRenderer renderer : mainParticleRenderers.values()) {
                try {
                    renderer.tick();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "particle main tick fail", e);
                }
            }
            // Multi particle renderers
            for (Map<UUID, ParticleRenderer> map : multiParticleRenderers.values()) {
                for (ParticleRenderer renderer : map.values()) {
                    try {
                        renderer.tick();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "particle multi tick fail", e);
                    }
                }
            }
            // Shared particle renderers
            for (Map<UUID, ParticleRenderer> map : sharedParticleRenderers.values()) {
                for (ParticleRenderer renderer : map.values()) {
                    try {
                        renderer.tick();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "particle shared tick fail", e);
                    }
                }
            }
        }, interval, interval); // initial delay, repeat interval
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

    /**
     * Returns a read-only view of the main (primary-selection) renderer map.
     * The underlying map is a {@link java.util.concurrent.ConcurrentHashMap}, so
     * iterating its {@code values()} is safe from any thread (e.g. the bStats
     * async scheduler) without additional locking.
     */
    public java.util.Map<UUID, RegionRenderer> getMainRenderers() {
        return java.util.Collections.unmodifiableMap(mainRenderers);
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

    public int getPlayerRetainedLineCount(UUID playerId) {
        int count = 0;
        RegionRenderer mainRenderer = mainRenderers.get(playerId);
        if (mainRenderer != null) {
            count += mainRenderer.getRetainedLineCount();
        }
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.get(playerId);
        if (playerMultiRenderers != null) {
            for (RegionRenderer renderer : playerMultiRenderers.values()) {
                count += renderer.getRetainedLineCount();
            }
        }
        return count;
    }

    public int getPlayerRetainedLineEntityCount(UUID playerId) {
        int count = 0;
        RegionRenderer mainRenderer = mainRenderers.get(playerId);
        if (mainRenderer != null) {
            count += mainRenderer.getRetainedLineEntityCount();
        }
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.get(playerId);
        if (playerMultiRenderers != null) {
            for (RegionRenderer renderer : playerMultiRenderers.values()) {
                count += renderer.getRetainedLineEntityCount();
            }
        }
        return count;
    }

    public RetainedLineStats getPlayerRetainedLineStats(UUID playerId) {
        RetainedLineStats total = RetainedLineStats.empty();
        RegionRenderer mainRenderer = mainRenderers.get(playerId);
        if (mainRenderer != null) {
            total = addRetainedLineStats(total, mainRenderer.getLastRetainedLineStats());
        }
        Map<UUID, RegionRenderer> playerMultiRenderers = multiRenderers.get(playerId);
        if (playerMultiRenderers != null) {
            for (RegionRenderer renderer : playerMultiRenderers.values()) {
                total = addRetainedLineStats(total, renderer.getLastRetainedLineStats());
            }
        }
        return total;
    }

    private RetainedLineStats addRetainedLineStats(RetainedLineStats first, RetainedLineStats second) {
        return new RetainedLineStats(
                first.reusedLines() + second.reusedLines(),
                first.spawnedLines() + second.spawnedLines(),
                first.removedLines() + second.removedLines(),
                first.removedTransientShapes() + second.removedTransientShapes());
    }

    public void shutdown() {
        plugin.getLogger().info("shutdown render manager");
        if (rebaseTask != null) {
            rebaseTask.cancel();
            rebaseTask = null;
        }
        if (particleTickTask != null) {
            particleTickTask.cancel();
            particleTickTask = null;
        }
        clearAllRenders();
    }

    /**
     * Starts a periodic task that checks if players have moved far enough
     * from the original shape spawn point to require rebasing entity origins.
     * Runs every 10 ticks (0.5 seconds).
     */
    private void startRebaseTask() {
        rebaseTask = FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(plugin, ignored -> {
            for (RegionRenderer renderer : mainRenderers.values()) {
                try {
                    reportRebaseIfDebug(renderer, renderer.rebaseOriginIfNeeded());
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "rebase main renderer fail", e);
                }
            }
            for (Map<UUID, RegionRenderer> playerMultiRenderers : multiRenderers.values()) {
                for (RegionRenderer renderer : playerMultiRenderers.values()) {
                    try {
                        reportRebaseIfDebug(renderer, renderer.rebaseOriginIfNeeded());
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "rebase multi renderer fail", e);
                    }
                }
            }
            for (Map<UUID, RegionRenderer> playerSharedRenderers : sharedRenderers.values()) {
                for (RegionRenderer renderer : playerSharedRenderers.values()) {
                    try {
                        reportRebaseIfDebug(renderer, renderer.rebaseOriginIfNeeded());
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "rebase shared renderer fail", e);
                    }
                }
            }
        }, 10L, 10L); // initial delay 10 ticks, period 10 ticks
    }

    private void reportRebaseIfDebug(RegionRenderer renderer, int rebasedShapes) {
        if (rebasedShapes <= 0) return;
        Player player = renderer.getPlayer();
        if (player == null || !player.isOnline()) return;
        PlayerData playerData = PlayerData.getPlayerData(player);
        if (playerData == null || !playerData.isDebugEnabled()) return;
        MessageUtil.sendTranslated(player, "command.wedisplay.debug.rebased_shape_count", rebasedShapes);
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
