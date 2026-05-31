package dev.twme.worldeditdisplay.display.particle;

import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.EllipsoidRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * Particle-based renderer for EllipsoidRegion (spheres and ovals).
 * <p>
 * Uses a 5-great-circle (armillary sphere) strategy:
 * <ul>
 *   <li>1 horizontal XZ ring (equator)</li>
 *   <li>4 vertical rings around Y axis at 0°, 45°, 90°, 135°</li>
 * </ul>
 * From above this forms a 米-shape pattern with a circle.
 * Each ring uses the edge particle (FLAME) with density-based segment count.
 */
public class ParticleEllipsoidRenderer extends ParticleRenderer<EllipsoidRegion> {

    public ParticleEllipsoidRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public Class<EllipsoidRegion> getRegionType() {
        return EllipsoidRegion.class;
    }

    @Override
    public void render(EllipsoidRegion region) {
        clear();

        Vector3 center = region.getCenter();
        Vector3 radii = region.getRadii();

        if (center == null || radii == null) return;

        double cx = center.getX();
        double cy = center.getY();
        double cz = center.getZ();
        // Use block-centre coordinates (matching TextDisplay EllipsoidRenderer's centrePos)
        double centreX = cx + 0.5;
        double centreY = cy + 0.5;
        double centreZ = cz + 0.5;
        // Use raw radii from the region (TextDisplay does NOT add any offset to radii)
        double rx = radii.getX();
        double ry = radii.getY();
        double rz = radii.getZ();

        // Compute segment count from average radius
        double avgRadius = (rx + ry + rz) / 3.0;
        int segments = (int) Math.max(MIN_SEGMENTS,
                Math.min(Math.ceil(Math.PI * avgRadius * edgeDensity), MAX_SEGMENTS));

        // ── 1. Horizontal ring (XZ plane at centre Y) ─────────────────────
        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double x = centreX + rx * Math.cos(angle);
            double z = centreZ + rz * Math.sin(angle);
            edgePoints.add(new Vector3(x, centreY, z));
        }

        // ── 4 Vertical rings (rotated around Y axis) ───────────────────────
        double[] ringAngles = {0.0, Math.PI / 4, Math.PI / 2, 3 * Math.PI / 4};

        for (double theta : ringAngles) {
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            for (int i = 0; i < segments; i++) {
                double alpha = 2.0 * Math.PI * i / segments;
                double cosA = Math.cos(alpha);
                double sinA = Math.sin(alpha);

                double x = centreX + rx * cosA * cosT;
                double y = centreY + ry * sinA;
                double z = centreZ + rz * cosA * sinT;

                edgePoints.add(new Vector3(x, y, z));
            }
        }

        // ── Center marker (1×1×1 box at the block-min corner, matching TextDisplay EllipsoidRenderer) ──
        // TextDisplay: renderCube(centerPos=(cx+0.5, cy+0.5, cz+0.5), size=1.0)
        //   → box from (cx, cy, cz) to (cx+1, cy+1, cz+1)
        // Particle: point = block-min corner = (cx, cy, cz)
        //   → renderPointMarkerBox draws from (cx-0.03, cy-0.03, cz-0.03) to (cx+1.03, cy+1.03, cz+1.03) ✅
        renderPointMarkerBox(new Vector3(cx, cy, cz),
                settings.getEllipsoidCenterColor());
    }
}
