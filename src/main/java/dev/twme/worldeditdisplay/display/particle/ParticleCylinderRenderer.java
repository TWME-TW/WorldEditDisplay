package dev.twme.worldeditdisplay.display.particle;

import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.CylinderRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * Particle-based renderer for CylinderRegion.
 * <p>
 * Renders:
 * <ul>
 *   <li>Upper ring and lower ring (circle particles with edge density)</li>
 *   <li>8 vertical lines connecting the rings at 0/45/90/135/180/225/270/315 degrees</li>
 *   <li>Center marker point as DUST</li>
 * </ul>
 */
public class ParticleCylinderRenderer extends ParticleRenderer<CylinderRegion> {

    public ParticleCylinderRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public Class<CylinderRegion> getRegionType() {
        return CylinderRegion.class;
    }

    @Override
    public void render(CylinderRegion region) {
        clear();

        Vector3 center = region.getCenter();
        if (center == null) return;

        double radiusX = region.getRadiusX();
        double radiusZ = region.getRadiusZ();
        int minY = region.getMinY();
        int maxY = region.getMaxY();

        double cx = center.getX() + 0.5;
        double cz = center.getZ() + 0.5;
        // Extend radii by +0.5 so the ring passes through the outer face of outermost blocks
        double ringRadiusX = radiusX + 0.5;
        double ringRadiusZ = radiusZ + 0.5;
        // Extend maxY by +1 to fully encompass the last block (same as CuboidRenderer)
        double topY = maxY + 1.0;

        // If both radii are zero, just show the center point as a marker box
        if (radiusX <= 0 || radiusZ <= 0) {
            renderPointMarkerBox(Vector3.at(cx, (minY + topY) / 2.0, cz), settings.getCylinderCenterColor());
            return;
        }

        // ── Upper, middle, and lower rings ─────────────────────────────────
        double midY = (minY + topY) / 2.0;
        java.util.List<Vector3> upperRing = computeRingPoints(cx, 0, cz, ringRadiusX, ringRadiusZ, topY);
        java.util.List<Vector3> middleRing = computeRingPoints(cx, 0, cz, ringRadiusX, ringRadiusZ, midY);
        java.util.List<Vector3> lowerRing = computeRingPoints(cx, 0, cz, ringRadiusX, ringRadiusZ, minY);

        edgePoints.addAll(upperRing);
        edgePoints.addAll(middleRing);
        edgePoints.addAll(lowerRing);

        // ── 8 vertical lines connecting rings + top/bottom lid 米字 spokes ──
        Vector3 centerTop = new Vector3(cx, topY, cz);
        Vector3 centerBottom = new Vector3(cx, minY, cz);

        int[] angleIndices = new int[8];
        int segments = upperRing.size();
        for (int i = 0; i < 8; i++) {
            angleIndices[i] = (int) Math.round((double) i / 8.0 * segments) % segments;
        }

        for (int idx : angleIndices) {
            Vector3 top = upperRing.get(idx);
            Vector3 bottom = lowerRing.get(idx);

            // Side vertical line (connecting top ring ↔ bottom ring)
            edgePoints.addAll(interpolateLine(bottom, top, edgeDensity));

            // Top lid spoke (center → ring)
            edgePoints.addAll(interpolateLine(centerTop, top, edgeDensity));

            // Bottom lid spoke (center → ring)
            edgePoints.addAll(interpolateLine(centerBottom, bottom, edgeDensity));
        }

        // ── Center marker (1×1×1 box matching TextDisplay CylinderRenderer's renderCube with size 1.03) ──
        renderPointMarkerBox(
                Vector3.at(cx, (minY + topY) / 2.0, cz),
                settings.getCylinderCenterColor());
    }
}
