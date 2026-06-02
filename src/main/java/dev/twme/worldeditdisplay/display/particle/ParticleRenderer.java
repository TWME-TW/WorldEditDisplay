package dev.twme.worldeditdisplay.display.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.Region;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * Abstract base class for rendering region selections using Bukkit Particle API.
 * <p>
 * Designed for players whose client version is below 1.19.4 (no TextDisplay support).
 * Uses {@link Player#spawnParticle(Particle, Location, int, double, double, double, double)}
 * so that particles are sent to exactly one viewer, preserving per-player visibility.
 * <p>
 * Subclasses implement {@link #render(Region)} to discretise the region geometry
 * into point clouds, and {@link #tick()} sends them to the owning player (or to
 * shared viewers).
 *
 * @param <T> the region type this renderer handles
 */
public abstract class ParticleRenderer<T extends Region> {

    protected final WorldEditDisplay plugin;
    protected final Player player;
    protected final UUID playerUUID;
    protected final PlayerRenderSettings settings;

    // Edge points → sent as FLAME particles (or DUST in shared mode)
    protected List<Vector3> edgePoints;
    // Colored edge points → sent as DUST with their specific colour (for point markers rendered as box frames)
    protected List<ColoredEdgePoint> coloredEdgePoints;
    // Marker points (point1/point2) → single DUST dot (simple fallback)
    protected List<MarkerPoint> markerPoints;
    // Viewers that should receive particles (only the owner by default)
    protected final List<UUID> viewers;

    // When non-null, ALL particles use this colour (shared selections via DUST).
    // When null, edge points use FLAME and marker points use the configured DustOptions color.
    protected Color sharedColor;

    // Per-frame cache to avoid recreating lists on every tick
    private boolean pointsDirty = true;

    // Configuration (will be loaded from ParticleFallbackSettings in Phase 3)
    protected double edgeDensity = 1.0;
    protected int particleInterval = 5; // ticks between particle sends

    protected static final double MIN_SEGMENTS = 12;
    protected static final double MAX_SEGMENTS = 60;

    protected ParticleRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        this.plugin = plugin;
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.settings = settings;
        this.edgePoints = new ArrayList<>();
        this.coloredEdgePoints = new ArrayList<>();
        this.markerPoints = new ArrayList<>();
        this.viewers = new ArrayList<>();
        this.viewers.add(playerUUID);
    }

    // ─── Abstract methods ────────────────────────────────────────────────────

    /**
     * Compute the point cloud for this region.
     * Subclasses must populate {@link #edgePoints} and {@link #markerPoints}.
     */
    public abstract void render(T region);

    /**
     * @return the concrete region type this renderer handles
     */
    public abstract Class<T> getRegionType();

    // ─── Tick / send ─────────────────────────────────────────────────────────

    /**
     * Send all cached points as particles to every viewer.
     * Call this periodically (e.g. every 5 ticks) via a shared scheduler task.
     */
    public void tick() {
        if (edgePoints.isEmpty() && coloredEdgePoints.isEmpty() && markerPoints.isEmpty()) return;
        if (viewers.isEmpty()) return;

        if (sharedColor != null) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(
                    Color.fromRGB(sharedColor.getRed(), sharedColor.getGreen(), sharedColor.getBlue()),
                    1.0f);
            for (UUID viewerId : viewers) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline()) continue;
                sendPointsAsDust(viewer, dustOptions);
            }
        } else {
            for (UUID viewerId : viewers) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline()) continue;
                sendFlameParticles(viewer);
                sendColoredEdgeParticles(viewer);
                sendMarkerParticles(viewer);
            }
        }
    }

    private void sendFlameParticles(Player viewer) {
        for (Vector3 point : edgePoints) {
            viewer.spawnParticle(Particle.FLAME,
                    point.getX(), point.getY(), point.getZ(),
                    0, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** All point types rendered as DUST with the same colour (shared / viewall mode). */
    private void sendPointsAsDust(Player viewer, Particle.DustOptions dustOptions) {
        for (Vector3 point : edgePoints) {
            viewer.spawnParticle(Particle.DUST,
                    point.getX(), point.getY(), point.getZ(),
                    0, 0.0, 0.0, 0.0, 0.0, dustOptions);
        }
        for (ColoredEdgePoint cp : coloredEdgePoints) {
            viewer.spawnParticle(Particle.DUST,
                    cp.position.getX(), cp.position.getY(), cp.position.getZ(),
                    0, 0.0, 0.0, 0.0, 0.0, dustOptions);
        }
        for (MarkerPoint mp : markerPoints) {
            viewer.spawnParticle(Particle.DUST,
                    mp.position.getX(), mp.position.getY(), mp.position.getZ(),
                    0, 0.0, 0.0, 0.0, 0.0, dustOptions);
        }
    }

    /** Colored edge points (point-marker box frames) → DUST with their own colour. */
    private void sendColoredEdgeParticles(Player viewer) {
        for (ColoredEdgePoint cp : coloredEdgePoints) {
            Color color = cp.color;
            if (color == null) color = Color.RED;
            Particle.DustOptions dustOptions = new Particle.DustOptions(
                    Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()),
                    1.0f);
            viewer.spawnParticle(Particle.DUST,
                    cp.position.getX(), cp.position.getY(), cp.position.getZ(),
                    0, 0.0, 0.0, 0.0, 0.0, dustOptions);
        }
    }

    /** Single-point markers → DUST with their own colour. */
    private void sendMarkerParticles(Player viewer) {
        for (MarkerPoint mp : markerPoints) {
            Color color = mp.color;
            if (color == null) color = Color.RED;
            Particle.DustOptions dustOptions = new Particle.DustOptions(
                    Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()),
                    1.0f);
            viewer.spawnParticle(Particle.DUST,
                    mp.position.getX(), mp.position.getY(), mp.position.getZ(),
                    0, 0.0, 0.0, 0.0, 0.0, dustOptions);
        }
    }

    // ─── Cleanup ─────────────────────────────────────────────────────────────

    /**
     * Clear all cached points and reset state.
     */
    public void clear() {
        edgePoints.clear();
        coloredEdgePoints.clear();
        markerPoints.clear();
        viewers.clear();
        pointsDirty = true;
    }

    /**
     * Mark points as needing recomputation on the next tick.
     */
    protected void markDirty() {
        pointsDirty = true;
    }

    // ─── Viewer management ───────────────────────────────────────────────────

    public void addViewer(UUID viewerId) {
        if (!viewers.contains(viewerId)) {
            viewers.add(viewerId);
        }
    }

    public void removeViewer(UUID viewerId) {
        viewers.remove(viewerId);
    }

    public List<UUID> getViewers() {
        return viewers;
    }

    // ─── Shared colour ───────────────────────────────────────────────────────

    /**
     * When set, all particles (edges + markers) use REDSTONE with DUST colour.
     * Used for share / viewall modes so each sharer can be distinguished by colour.
     */
    public void setSharedColor(Color color) {
        this.sharedColor = color;
    }

    public Color getSharedColor() {
        return sharedColor;
    }

    // ─── Configuration ───────────────────────────────────────────────────────

    /**
     * Apply global particle fallback settings from the server config.
     * Called once after the renderer is created.
     */
    public void applyServerSettings(dev.twme.worldeditdisplay.config.RenderSettings renderSettings) {
        this.edgeDensity = Math.max(0.25, Math.min(renderSettings.getParticleEdgeDensity(), 4.0));
    }

    public void setEdgeDensity(double edgeDensity) {
        this.edgeDensity = Math.max(0.25, Math.min(edgeDensity, 4.0));
    }

    public void setParticleInterval(int intervalTicks) {
        this.particleInterval = Math.max(1, intervalTicks);
    }

    public double getEdgeDensity() {
        return edgeDensity;
    }

    // ─── Helper: compute circle ring points (for cylinder, ellipsoid) ────────

    /**
     * Compute points along a circle/ellipse ring.
     *
     * @param centerX  centre X
     * @param centerY  centre Y
     * @param centerZ  centre Z
     * @param radiusX  radius along X axis
     * @param radiusZ  radius along Z axis
     * @param yOffset  offset to add to Y (for vertical positioning)
     * @return list of points around the ring
     */
    protected List<Vector3> computeRingPoints(double centerX, double centerY, double centerZ,
                                               double radiusX, double radiusZ, double yOffset) {
        double avgRadius = (radiusX + radiusZ) / 2.0;
        int segments = (int) Math.max(MIN_SEGMENTS,
                Math.min(Math.ceil(Math.PI * avgRadius * edgeDensity), MAX_SEGMENTS));

        List<Vector3> points = new ArrayList<>(segments);
        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double dx = radiusX * Math.cos(angle);
            double dz = radiusZ * Math.sin(angle);
            points.add(new Vector3(centerX + dx, centerY + yOffset, centerZ + dz));
        }
        return points;
    }

    // ─── Helper: interpolate between two points ──────────────────────────────

    /**
     * Linearly interpolate between two 3D points with the given density.
     * Includes both endpoints so the entire edge is covered.
     *
     * @return list of points along the line segment, inclusive of both {@code from} and {@code to}
     */
    protected List<Vector3> interpolateLine(Vector3 from, Vector3 to, double density) {
        double distance = from.distance(to);
        if (distance < 0.001) {
            List<Vector3> result = new ArrayList<>(1);
            result.add(from);
            return result;
        }
        // Number of segments = ceil(distance * density); number of points = segments + 1
        int segments = Math.max(1, (int) Math.ceil(distance * density));
        List<Vector3> points = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double x = from.getX() + (to.getX() - from.getX()) * t;
            double y = from.getY() + (to.getY() - from.getY()) * t;
            double z = from.getZ() + (to.getZ() - from.getZ()) * t;
            points.add(new Vector3(x, y, z));
        }
        return points;
    }

    // ─── Helper data classes ────────────────────────────────────────────────

    /**
     * An edge point with an associated colour, rendered as DUST.
     * Used for point-marker box frames (1×1×1 block outlines).
     */
    protected static class ColoredEdgePoint {
        final Vector3 position;
        final Color color;

        ColoredEdgePoint(Vector3 position, Color color) {
            this.position = position;
            this.color = color;
        }
    }

    /**
     * A single-dot marker point with an associated colour, rendered as DUST.
     */
    protected static class MarkerPoint {
        final Vector3 position;
        final Color color;

        MarkerPoint(Vector3 position, Color color) {
            this.position = position;
            this.color = color;
        }
    }

    // ─── Helper: render a box frame between two corners ─────────────────────

    /**
     * Renders the 12 edges of an axis-aligned box from {@code min} to {@code max}
     * as coloured edge points (DUST). Matches TextDisplay's {@code renderBoxFrame}.
     */
    protected void renderBoxFrame(Vector3 min, Vector3 max, Color color) {
        double minX = min.getX(), minY = min.getY(), minZ = min.getZ();
        double maxX = max.getX(), maxY = max.getY(), maxZ = max.getZ();

        Vector3 v000 = new Vector3(minX, minY, minZ);
        Vector3 v001 = new Vector3(minX, minY, maxZ);
        Vector3 v010 = new Vector3(minX, maxY, minZ);
        Vector3 v011 = new Vector3(minX, maxY, maxZ);
        Vector3 v100 = new Vector3(maxX, minY, minZ);
        Vector3 v101 = new Vector3(maxX, minY, maxZ);
        Vector3 v110 = new Vector3(maxX, maxY, minZ);
        Vector3 v111 = new Vector3(maxX, maxY, maxZ);

        double density = Math.max(edgeDensity, 3.0);
        for (Vector3 v : interpolateLine(v000, v001, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v000, v100, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v001, v101, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v100, v101, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v010, v011, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v010, v110, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v011, v111, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v110, v111, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v000, v010, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v001, v011, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v100, v110, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
        for (Vector3 v : interpolateLine(v101, v111, density)) coloredEdgePoints.add(new ColoredEdgePoint(v, color));
    }

    // ─── Helper: render a point marker as a 1×1×1 box frame ─────────────────

    /**
     * Adds coloured edge points forming a 1×1×1 box around the given block coordinate,
     * matching the visual style of {@code CuboidRenderer.renderPointMarker()}.
     * The box spans from {@code (x-0.03, y-0.03, z-0.03)} to {@code (x+1.03, y+1.03, z+1.03)}.
     */
    protected void renderPointMarkerBox(Vector3 point, Color color) {
        if (color == null) color = Color.RED;
        double padding = 0.03;
        double minX = point.getX() - padding;
        double minY = point.getY() - padding;
        double minZ = point.getZ() - padding;
        double maxX = point.getX() + 1.0 + padding;
        double maxY = point.getY() + 1.0 + padding;
        double maxZ = point.getZ() + 1.0 + padding;
        renderBoxFrame(new Vector3(minX, minY, minZ), new Vector3(maxX, maxY, maxZ), color);
    }

    // ─── Helper: render a small cube marker ─────────────────────────────────

    /**
     * Renders a small cube centred at {@code center} with the given {@code size}.
     * Used for Polyhedron and Cylinder centre markers (matching TextDisplay's
     * {@code renderCube}), which are small — not full 1×1×1 blocks.
     */
    protected void renderSmallCube(Vector3 center, Color color, double size) {
        if (color == null) color = Color.RED;
        double half = Math.max(size, 0.02) / 2.0;
        renderBoxFrame(
                new Vector3(center.getX() - half, center.getY() - half, center.getZ() - half),
                new Vector3(center.getX() + half, center.getY() + half, center.getZ() + half),
                color);
    }

    // ─── Post-render processing ──────────────────────────────────────────────

    private static final int LARGE_SELECTION_THRESHOLD = 800;
    private static final double MIN_DOWNGRADE_SCALE = 0.25;

    /**
     * Called automatically by RenderManager after {@link #render(Region)}.
     * If the total particle count exceeds the threshold, edge points are
     * sub-sampled to keep packet count manageable.
     */
    public void postRender() {
        int total = edgePoints.size() + coloredEdgePoints.size() + markerPoints.size();
        if (total <= LARGE_SELECTION_THRESHOLD) return;

        double scale = Math.max(MIN_DOWNGRADE_SCALE, (double) LARGE_SELECTION_THRESHOLD / total);
        int targetEdge = Math.max(8, (int) (edgePoints.size() * scale));

        if (targetEdge < edgePoints.size()) {
            List<Vector3> reduced = new ArrayList<>(targetEdge);
            int step = Math.max(1, edgePoints.size() / targetEdge);
            for (int i = 0; i < edgePoints.size(); i += step) {
                reduced.add(edgePoints.get(i));
            }
            edgePoints = reduced;
        }
    }

    // ─── Point count ─────────────────────────────────────────────────────────

    /**
     * @return the total number of edge + marker points currently in the cache
     */
    public int getPointCount() {
        return edgePoints.size() + coloredEdgePoints.size() + markerPoints.size();
    }

    // ─── Viewers ────────────────────────────────────────────────────────────
}
