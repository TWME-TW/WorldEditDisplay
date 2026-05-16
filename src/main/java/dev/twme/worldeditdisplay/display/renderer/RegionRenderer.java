package dev.twme.worldeditdisplay.display.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.packet.PacketShapeFactory;
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

    protected RenderConfig config;

    // tracks where shapes were last spawned/rebased for distance checking
    private Location lastRebaseOrigin;
    /** Cached origin for the current render pass; reset to null by clear(). */
    private Location renderOrigin;

    private static final double REBASE_DISTANCE_SQUARED = 80.0 * 80.0;

    public RegionRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        this.plugin = plugin;
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.settings = settings;
        this.shapes = new ArrayList<>();
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
        lastRebaseOrigin = null;
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
    public void rebaseOriginIfNeeded() {
        if (shapes.isEmpty() || lastRebaseOrigin == null) return;

        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().equals(lastRebaseOrigin.getWorld())) return;
        if (playerLoc.distanceSquared(lastRebaseOrigin) < REBASE_DISTANCE_SQUARED) return;

        Location newOrigin = playerLoc.clone();
        newOrigin.setYaw(0);
        newOrigin.setPitch(0);

        for (Shape shape : shapes) {
            try {
                shape.teleportOrigin(newOrigin.getX(), newOrigin.getY(), newOrigin.getZ());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to rebase shape origin", e);
            }
        }

        lastRebaseOrigin = newOrigin.clone();
    }

    /**
     * Check if this shape should render through blocks
     * Must be implemented by subclasses with shape-specific settings
     */
    protected abstract boolean isSeeThrough();

    /**
     * Render a line using TextDisplayShapes
     */
    protected void renderLine(Line line, Color color, float thickness) {
        Location origin = getOrigin();
        Shape shape = new PacketShapeFactory()
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
        shapes.add(shape);
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
        Location origin = getOrigin();
        Shape shape = new PacketShapeFactory()
                .parallelogram(origin, p1, p2, p3)
            .rootAnchor(true)
                .color(color)
                .seeThrough(isSeeThrough())
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
        Location origin = getOrigin();
        Shape shape = new PacketShapeFactory()
                .triangle(origin, p1, p2, p3)
            .rootAnchor(true)
                .color(color)
                .seeThrough(isSeeThrough())
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
        return shapes.stream().mapToInt(s -> s.getEntityUUIDs().size()).sum();
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
}