package dev.twme.worldeditdisplay.display.particle;

import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.BoundingBox;
import dev.twme.worldeditdisplay.region.CuboidRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * Particle-based renderer for CuboidRegion.
 * <p>
 * Renders:
 * <ul>
 *   <li>12 box edges as FLAME particles (interpolated with edge density)</li>
 *   <li>Point1 / Point2 markers as REDSTONE DustOptions particles</li>
 * </ul>
 * No grid or fill faces are rendered.
 */
public class ParticleCuboidRenderer extends ParticleRenderer<CuboidRegion> {

    public ParticleCuboidRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public Class<CuboidRegion> getRegionType() {
        return CuboidRegion.class;
    }

    @Override
    public void render(CuboidRegion region) {
        clear();

        Vector3 point1 = region.getPoint1();
        Vector3 point2 = region.getPoint2();

        if (point1 == null && point2 == null) return;

        // ── Point markers (1×1×1 box frames with DUST, matching TextDisplay style) ───
        if (point1 != null) {
            renderPointMarkerBox(point1, settings.getCuboidPoint1Color());
        }
        if (point2 != null) {
            renderPointMarkerBox(point2, settings.getCuboidPoint2Color());
        }

        if (!region.isDefined()) return;

        BoundingBox regionBox = region.getBoundingBox();
        if (regionBox == null) return;

        Vector3 min = regionBox.getMin();
        Vector3 max = regionBox.getMax();

        // Match CuboidRenderer's offset: max coordinates + 1 to encompass full blocks
        double minX = min.getX();
        double minY = min.getY();
        double minZ = min.getZ();
        double maxX = max.getX() + 1.0;
        double maxY = max.getY() + 1.0;
        double maxZ = max.getZ() + 1.0;

        // ── 8 corners ────────────────────────────────────────────────────
        Vector3 v000 = Vector3.at(minX, minY, minZ);
        Vector3 v001 = Vector3.at(minX, minY, maxZ);
        Vector3 v010 = Vector3.at(minX, maxY, minZ);
        Vector3 v011 = Vector3.at(minX, maxY, maxZ);
        Vector3 v100 = Vector3.at(maxX, minY, minZ);
        Vector3 v101 = Vector3.at(maxX, minY, maxZ);
        Vector3 v110 = Vector3.at(maxX, maxY, minZ);
        Vector3 v111 = Vector3.at(maxX, maxY, maxZ);

        // ── 12 edges (interpolated) ──────────────────────────────────────
        // Bottom face
        edgePoints.addAll(interpolateLine(v000, v001, edgeDensity));
        edgePoints.addAll(interpolateLine(v000, v100, edgeDensity));
        edgePoints.addAll(interpolateLine(v001, v101, edgeDensity));
        edgePoints.addAll(interpolateLine(v100, v101, edgeDensity));

        // Top face
        edgePoints.addAll(interpolateLine(v010, v011, edgeDensity));
        edgePoints.addAll(interpolateLine(v010, v110, edgeDensity));
        edgePoints.addAll(interpolateLine(v011, v111, edgeDensity));
        edgePoints.addAll(interpolateLine(v110, v111, edgeDensity));

        // Vertical edges
        edgePoints.addAll(interpolateLine(v000, v010, edgeDensity));
        edgePoints.addAll(interpolateLine(v001, v011, edgeDensity));
        edgePoints.addAll(interpolateLine(v100, v110, edgeDensity));
        edgePoints.addAll(interpolateLine(v101, v111, edgeDensity));
    }
}
