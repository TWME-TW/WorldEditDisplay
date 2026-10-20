package dev.twme.worldeditdisplay.display.renderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Abstract base class for rendering regions.
 *
 * Provides common rendering utilities:
 * - shape pool management
 * - player visibility handling
 * - cleanup
 *
 * @param <T> the region type this renderer handles
 */
public abstract class RegionRenderer<T extends Region> {

    protected final WorldEditDisplay plugin;
    protected final Player player;
    protected final UUID playerUUID;
    protected final PlayerRenderSettings settings;
    // pool of shapes
    protected final List<Shape> shapes;
    private final Map<LineKey, Shape> retainedLineShapes;
    private final Set<Shape> retainedLineShapeSet;
    /** Lines requested by the current retained pass; spawned, moved or reused when it finishes. */
    private Map<LineKey, PendingLine> retainedLinePass;
    private RetainedLineStats currentRetainedLineStats = RetainedLineStats.empty();
    private RetainedLineStats lastRetainedLineStats = RetainedLineStats.empty();

    protected RenderConfig config;

    // tracks where shapes were last spawned/rebased for distance checking
    private Location lastRebaseOrigin;
    /** Cached origin for the current render pass; reset to null by clear(). */
    private Location renderOrigin;

    private static final double REBASE_DISTANCE_SQUARED = 80.0 * 80.0;
    private static final float MIN_LINE_LENGTH_SQUARED = 1.0e-6f;
    /** Coordinate tolerance when matching a pass against the retained lines moved by one offset. */
    private static final float TRANSLATION_EPSILON = 1.0e-3f;
    private static final Comparator<LineKey> LINE_KEY_ORDER = Comparator
            .comparingInt(LineKey::color)
            .thenComparingInt(LineKey::thickness)
            .thenComparing(LineKey::seeThrough)
            .thenComparingDouble(LineKey::startX)
            .thenComparingDouble(LineKey::startY)
            .thenComparingDouble(LineKey::startZ)
            .thenComparingDouble(LineKey::endX)
            .thenComparingDouble(LineKey::endY)
            .thenComparingDouble(LineKey::endZ);

    public RegionRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        this.plugin = plugin;
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.settings = settings;
        this.shapes = new ArrayList<>();
        this.retainedLineShapes = new HashMap<>();
        this.retainedLineShapeSet = new HashSet<>();
        this.config = RenderConfig.getDefault();
    }

    /**
     * Render the given region
     */
    public abstract void render(T region);

    /**
     * Get the type of region this renderer supports
     */
    public abstract Class<T> getRegionType();

    /**
     * Remove all shapes and clear the pool
     */
    public void clear() {
        renderOrigin = null;
        for (Shape shape : shapes) {
            try {
                shape.remove();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove shape", e);
            }
        }
        shapes.clear();
        retainedLineShapes.clear();
        retainedLineShapeSet.clear();
        retainedLinePass = null;
        currentRetainedLineStats = RetainedLineStats.empty();
        lastRetainedLineStats = RetainedLineStats.empty();
        lastRebaseOrigin = null;
    }

    protected void beginRetainedLineRender() {
        renderOrigin = null;
        currentRetainedLineStats = RetainedLineStats.empty();
        removeNonRetainedShapes();
        retainedLinePass = new LinkedHashMap<>();
    }

    private void removeNonRetainedShapes() {
        Iterator<Shape> iterator = shapes.iterator();
        while (iterator.hasNext()) {
            Shape shape = iterator.next();
            if (retainedLineShapeSet.contains(shape)) continue;

            try {
                shape.remove();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove transient shape", e);
            }
            iterator.remove();
            currentRetainedLineStats = currentRetainedLineStats.withTransientRemoved();
        }
    }

    /**
     * Applies the lines requested by this pass. Unchanged lines are kept; when every line moved by
     * the same offset (a selection or preview that was only shifted), the existing shapes are
     * translated, which costs one teleport per line instead of a despawn and respawn.
     */
    protected void finishRetainedLineRender() {
        if (retainedLinePass == null) return;
        Map<LineKey, PendingLine> pass = retainedLinePass;
        retainedLinePass = null;

        boolean translated = false;
        if (!pass.keySet().equals(retainedLineShapes.keySet())) {
            translated = translateRetainedLines(pass.keySet());
            if (!translated) removeStaleLines(pass.keySet());
        }
        for (Map.Entry<LineKey, PendingLine> entry : pass.entrySet()) {
            if (retainedLineShapes.containsKey(entry.getKey())) {
                if (!translated) currentRetainedLineStats = currentRetainedLineStats.withReusedLine();
                continue;
            }
            PendingLine line = entry.getValue();
            Shape shape = createLineShape(line.line(), line.color(), line.thickness());
            retainedLineShapes.put(entry.getKey(), shape);
            retainedLineShapeSet.add(shape);
            shapes.add(shape);
            currentRetainedLineStats = currentRetainedLineStats.withSpawnedLine();
        }

        lastRetainedLineStats = currentRetainedLineStats;
    }

    private boolean translateRetainedLines(Collection<LineKey> requested) {
        List<LineKey> previous = sortedLineKeys(retainedLineShapes.keySet());
        List<LineKey> next = sortedLineKeys(requested);
        Vector3f offset = findTranslation(previous, next);
        if (offset == null) return false;

        Map<LineKey, Shape> moved = new HashMap<>();
        // One bundle for the whole move, so the client sees every line in its new place at once.
        plugin.getPacketShapeFactory().batch(() -> {
            for (int index = 0; index < previous.size(); index++) {
                Shape shape = retainedLineShapes.get(previous.get(index));
                try {
                    shape.translate(offset.x, offset.y, offset.z);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to translate line shape", e);
                    return;
                }
                moved.put(next.get(index), shape);
            }
        });
        // On failure the lines keep their old keys, so the respawn path replaces all of them.
        if (moved.size() != previous.size()) return false;
        retainedLineShapes.clear();
        retainedLineShapes.putAll(moved);
        currentRetainedLineStats = currentRetainedLineStats.withTranslatedLines(moved.size());
        return true;
    }

    private void removeStaleLines(Set<LineKey> requested) {
        Iterator<Map.Entry<LineKey, Shape>> iterator = retainedLineShapes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LineKey, Shape> entry = iterator.next();
            if (requested.contains(entry.getKey())) continue;

            Shape shape = entry.getValue();
            try {
                shape.remove();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove stale line shape", e);
            }
            shapes.remove(shape);
            iterator.remove();
            retainedLineShapeSet.remove(shape);
            currentRetainedLineStats = currentRetainedLineStats.withRemovedLine();
        }
    }

    static List<LineKey> sortedLineKeys(Collection<LineKey> keys) {
        List<LineKey> sorted = new ArrayList<>(keys);
        sorted.sort(LINE_KEY_ORDER);
        return sorted;
    }

    /**
     * Returns the single non-zero offset that maps every previous line onto the next lines, or
     * {@code null} when the two passes differ in anything but position. Both lists must be sorted
     * with {@link #sortedLineKeys}; a translation keeps that order, so lines pair up by index.
     */
    static Vector3f findTranslation(List<LineKey> previous, List<LineKey> next) {
        if (previous.isEmpty() || previous.size() != next.size()) return null;

        LineKey first = previous.get(0);
        LineKey firstNext = next.get(0);
        float dx = firstNext.startX() - first.startX();
        float dy = firstNext.startY() - first.startY();
        float dz = firstNext.startZ() - first.startZ();
        if (Math.abs(dx) < TRANSLATION_EPSILON && Math.abs(dy) < TRANSLATION_EPSILON
                && Math.abs(dz) < TRANSLATION_EPSILON) {
            return null;
        }

        for (int index = 0; index < previous.size(); index++) {
            if (!previous.get(index).isTranslationOf(next.get(index), dx, dy, dz)) return null;
        }
        return new Vector3f(dx, dy, dz);
    }

    protected Location toLocation(double x, double y, double z) {
        return new Location(player.getWorld(), x, y, z);
    }

    /**
     * Get the player's current location as the origin for shape creation.
     * Yaw and pitch are zeroed to prevent player rotation from skewing shapes.
     */
    protected Location getOrigin() {
        if (renderOrigin != null) return renderOrigin;
        Location loc = player.getLocation();
        loc.setYaw(0);
        loc.setPitch(0);
        if (lastRebaseOrigin == null) {
            lastRebaseOrigin = loc.clone();
        }
        renderOrigin = loc;
        return loc;
    }

    /**
     * Checks if the player has moved far enough from the original shape origin
     * and teleports all shape entities to the player's feet if so.
     * This prevents TextDisplay entities from exceeding their view range.
     */
    public int rebaseOriginIfNeeded() {
        if (shapes.isEmpty() || lastRebaseOrigin == null) return 0;

        Location playerLoc = player.getLocation();
        if (!isSameLoadedWorld(playerLoc, lastRebaseOrigin)) return 0;
        if (playerLoc.distanceSquared(lastRebaseOrigin) < REBASE_DISTANCE_SQUARED) return 0;

        Location newOrigin = playerLoc.clone();
        newOrigin.setYaw(0);
        newOrigin.setPitch(0);

        int rebasedShapes = 0;
        for (Shape shape : shapes) {
            try {
                shape.teleportOrigin(newOrigin.getX(), newOrigin.getY(), newOrigin.getZ());
                rebasedShapes++;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to rebase shape origin", e);
            }
        }

        lastRebaseOrigin = newOrigin.clone();
        return rebasedShapes;
    }

    static boolean isSameLoadedWorld(Location first, Location second) {
        return first != null
                && second != null
                && first.isWorldLoaded()
                && second.isWorldLoaded()
                && first.getWorld().equals(second.getWorld());
    }

    /**
     * Check if this shape should render through blocks
     * Must be implemented by subclasses with shape-specific settings
     */
    protected abstract boolean isSeeThrough();

    /** Whether fill surfaces should render through blocks. */
    protected abstract boolean isFillSeeThrough();

    /**
     * Render a line using TextDisplayShapes
     */
    protected void renderLine(Line line, Color color, float thickness) {
        if (!isRenderableLine(line) || isInvisible(color)) return;

        if (retainedLinePass != null) {
            retainedLinePass.putIfAbsent(LineKey.of(line, color, thickness, isSeeThrough()),
                    new PendingLine(line, color, thickness));
            return;
        }

        Shape shape = createLineShape(line, color, thickness);
        shapes.add(shape);
    }

    static boolean isRenderableLine(Line line) {
        return line != null
                && line.start() != null
                && line.end() != null
                && line.start().isFinite()
                && line.end().isFinite()
                && line.start().distanceSquared(line.end()) >= MIN_LINE_LENGTH_SQUARED;
    }

    /**
     * Fully transparent colors (for example a CUI colour override of {@code #00000000})
     * draw nothing, so their shapes are not spawned at all.
     */
    static boolean isInvisible(Color color) {
        return color != null && color.getAlpha() == 0;
    }

    private Shape createLineShape(Line line, Color color, float thickness) {
        Location origin = getOrigin();
        Shape shape = plugin.getPacketShapeFactory()
                .line(origin, line.start(), line.end(), thickness)
            .rootAnchor(true)
                .color(color)
                .seeThrough(isSeeThrough())
                .doubleSided(true)
                .brightness(15, 15)
                .viewRange(100f)
                .build();
        shape.addViewer(player.getUniqueId());
        shape.spawn();
        return shape;
    }

    /**
     * Renders multiple lines
     */
    protected void renderLines(Color color, float thickness, Line... lines) {
        for (Line line : lines) {
            renderLine(line, color, thickness);
        }
    }

    /**
     * Render a cube marker
     */
    protected void renderCube(Vector3f center, float size, Color color, float thickness) {
        float halfSize = size / 2.0f;
        renderBoxFrame(center.x - halfSize, center.y - halfSize, center.z - halfSize,
                center.x + halfSize, center.y + halfSize, center.z + halfSize,
                color, thickness);
    }

    /**
     * Render the edges of a box using 12 lines
     */
    protected void renderBoxFrame(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color color, float thickness) {
        Vector3f v000 = new Vector3f((float) minX, (float) minY, (float) minZ);
        Vector3f v001 = new Vector3f((float) minX, (float) minY, (float) maxZ);
        Vector3f v010 = new Vector3f((float) minX, (float) maxY, (float) minZ);
        Vector3f v011 = new Vector3f((float) minX, (float) maxY, (float) maxZ);
        Vector3f v100 = new Vector3f((float) maxX, (float) minY, (float) minZ);
        Vector3f v101 = new Vector3f((float) maxX, (float) minY, (float) maxZ);
        Vector3f v110 = new Vector3f((float) maxX, (float) maxY, (float) minZ);
        Vector3f v111 = new Vector3f((float) maxX, (float) maxY, (float) maxZ);

        renderLines(color, thickness,
                // Bottom face
                new Line(v000, v001), new Line(v000, v100),
                new Line(v001, v101), new Line(v100, v101),

                // Top face
                new Line(v010, v011), new Line(v010, v110),
                new Line(v011, v111), new Line(v110, v111),

                // Vertical edges
                new Line(v000, v010), new Line(v001, v011),
                new Line(v100, v110), new Line(v101, v111)
        );
    }

    /**
     * Render a point marker with a small padding cube
     */
    protected void renderPointMarker(dev.twme.worldeditdisplay.region.Vector3 point, Color color, float thickness) {
        final double PADDING = 0.03;
        renderBoxFrame(point.getX() - PADDING, point.getY() - PADDING, point.getZ() - PADDING,
                point.getX() + 1.0 + PADDING, point.getY() + 1.0 + PADDING, point.getZ() + 1.0 + PADDING,
                color, thickness);
    }

    /**
     * Render a parallelogram face using TextDisplayShapes.
     * p1 is the starting corner; p2 and p3 define the two edges.
     * The fourth corner is automatically p2 + p3 - p1.
     */
    protected void renderParallelogram(Vector3f p1, Vector3f p2, Vector3f p3, Color color) {
        if (isInvisible(color)) return;
        Location origin = getOrigin();
        Shape shape = plugin.getPacketShapeFactory()
                .parallelogram(origin, p1, p2, p3)
            .rootAnchor(true)
                .color(color)
                .seeThrough(isFillSeeThrough())
                .doubleSided(true)
                .brightness(15, 15)
                .viewRange(100f)
                .build();
        shape.addViewer(player.getUniqueId());
        shape.spawn();
        shapes.add(shape);
    }

    /**
     * Render a triangle face using TextDisplayShapes
     */
    protected void renderTriangle(Vector3f p1, Vector3f p2, Vector3f p3, Color color) {
        if (isInvisible(color)) return;
        Location origin = getOrigin();
        Shape shape = plugin.getPacketShapeFactory()
                .triangle(origin, p1, p2, p3)
            .rootAnchor(true)
                .color(color)
                .seeThrough(isFillSeeThrough())
                .doubleSided(true)
                .brightness(15, 15)
                .viewRange(100f)
                .build();
        shape.addViewer(player.getUniqueId());
        shape.spawn();
        shapes.add(shape);
    }

    public void setConfig(RenderConfig config) {
        this.config = config;
    }

    public int getEntityCount() {
        int count = 0;
        for (Shape shape : shapes) {
            count += shape.getEntityUUIDs().size();
        }
        return count;
    }

    public int getRetainedLineCount() {
        return retainedLineShapes.size();
    }

    public int getRetainedLineEntityCount() {
        int count = 0;
        for (Shape shape : retainedLineShapes.values()) {
            count += shape.getEntityUUIDs().size();
        }
        return count;
    }

    public RetainedLineStats getLastRetainedLineStats() {
        return lastRetainedLineStats;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Get the color for rendering, using CUI override for multi-selection
     */
    protected Color getColorWithOverride(Region region, int colorIndex,
                                         Color defaultColor, boolean isMultiSelection) {
        if (!isMultiSelection) return defaultColor;
        Color override = region.getOverrideColor(colorIndex);
        return override != null ? override : defaultColor;
    }

    /**
     * Get the fill face color, using CUI override for multi-selection.
     * When an override is active, alpha is reduced to 25% to match
     * WorldEditCUI's visual convention (hidden-line alpha × 0.25).
     */
    protected Color getFillColorWithOverride(Region region, int colorIndex,
                                              Color defaultColor, boolean isMultiSelection) {
        if (!isMultiSelection) return defaultColor;
        Color override = region.getOverrideColor(colorIndex);
        if (override == null) return defaultColor;
        return dev.twme.worldeditdisplay.util.ColorUtil.withAlphaFactor(override, 0.25f);
    }

    /**
     * Check if a region is part of a multi-selection
     */
    protected boolean isMultiSelection(Region region) {
        if (player == null) return false;
        dev.twme.worldeditdisplay.player.PlayerData playerData = dev.twme.worldeditdisplay.player.PlayerData.getPlayerData(player);
        if (playerData == null) return false;
        return playerData.getMultiRegions().containsValue(region);
    }

    protected record Line(Vector3f start, Vector3f end){};
    private record PendingLine(Line line, Color color, float thickness) {}
    public record RetainedLineStats(int reusedLines, int translatedLines, int spawnedLines, int removedLines,
                                    int removedTransientShapes) {
        public static RetainedLineStats empty() {
            return new RetainedLineStats(0, 0, 0, 0, 0);
        }

        private RetainedLineStats withReusedLine() {
            return new RetainedLineStats(reusedLines + 1, translatedLines, spawnedLines, removedLines, removedTransientShapes);
        }

        private RetainedLineStats withTranslatedLines(int count) {
            return new RetainedLineStats(reusedLines, translatedLines + count, spawnedLines, removedLines, removedTransientShapes);
        }

        private RetainedLineStats withSpawnedLine() {
            return new RetainedLineStats(reusedLines, translatedLines, spawnedLines + 1, removedLines, removedTransientShapes);
        }

        private RetainedLineStats withRemovedLine() {
            return new RetainedLineStats(reusedLines, translatedLines, spawnedLines, removedLines + 1, removedTransientShapes);
        }

        private RetainedLineStats withTransientRemoved() {
            return new RetainedLineStats(reusedLines, translatedLines, spawnedLines, removedLines, removedTransientShapes + 1);
        }
    }

    record LineKey(float startX, float startY, float startZ,
                   float endX, float endY, float endZ,
                   int color, int thickness, boolean seeThrough) {
        private boolean isTranslationOf(LineKey moved, float dx, float dy, float dz) {
            return color == moved.color && thickness == moved.thickness && seeThrough == moved.seeThrough
                    && Math.abs(startX + dx - moved.startX) < TRANSLATION_EPSILON
                    && Math.abs(startY + dy - moved.startY) < TRANSLATION_EPSILON
                    && Math.abs(startZ + dz - moved.startZ) < TRANSLATION_EPSILON
                    && Math.abs(endX + dx - moved.endX) < TRANSLATION_EPSILON
                    && Math.abs(endY + dy - moved.endY) < TRANSLATION_EPSILON
                    && Math.abs(endZ + dz - moved.endZ) < TRANSLATION_EPSILON;
        }

        static LineKey of(Line line, Color color, float thickness, boolean seeThrough) {
            Vector3f start = line.start();
            Vector3f end = line.end();
            return new LineKey(
                    start.x, start.y, start.z,
                    end.x, end.y, end.z,
                    color.asARGB(), Float.floatToIntBits(thickness), seeThrough);
        }
    }
}
