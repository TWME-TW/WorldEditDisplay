package dev.twme.worldeditdisplay.display.particle;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.PolygonRegion;
import dev.twme.worldeditdisplay.region.Vector2;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * Particle-based renderer for PolygonRegion.
 * <p>
 * Renders:
 * <ul>
 *   <li>2D polygon boundary at minY (bottom ring)</li>
 *   <li>2D polygon boundary at maxY (top ring)</li>
 *   <li>Vertical lines connecting corresponding vertices</li>
 * </ul>
 */
public class ParticlePolygonRenderer extends ParticleRenderer<PolygonRegion> {

    public ParticlePolygonRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public Class<PolygonRegion> getRegionType() {
        return PolygonRegion.class;
    }

    @Override
    public void render(PolygonRegion region) {
        clear();

        List<Vector2> points2d = region.getPoints();
        if (points2d.isEmpty()) return;

        int minY = region.getMinY();
        // Extend maxY by +1 to fully encompass the last block (same as CuboidRenderer)
        int topY = region.getMaxY() + 1;

        // Convert 2D points to 3D at bottom and top Y levels
        int n = points2d.size();

        // ── Build bottom and top edge rings (x+0.5/z+0.5 = block centre, matching TextDisplay) ──
        for (int i = 0; i < n; i++) {
            Vector2 p = points2d.get(i);
            if (p == null) continue;

            Vector2 next = points2d.get((i + 1) % n);
            if (next == null) continue;

            double cx = 0.5, cz = 0.5;

            // Bottom edge segment (block centre at minY)
            Vector3 bottomFrom = new Vector3(p.getX() + cx, minY, p.getZ() + cz);
            Vector3 bottomTo = new Vector3(next.getX() + cx, minY, next.getZ() + cz);
            edgePoints.addAll(interpolateLine(bottomFrom, bottomTo, edgeDensity));

            // Top edge segment (block centre at topY = maxY + 1)
            Vector3 topFrom = new Vector3(p.getX() + cx, topY, p.getZ() + cz);
            Vector3 topTo = new Vector3(next.getX() + cx, topY, next.getZ() + cz);
            edgePoints.addAll(interpolateLine(topFrom, topTo, edgeDensity));
        }

        // ── Vertical lines at each vertex centre (x+0.5/z+0.5, matching TextDisplay) ──
        for (int i = 0; i < n; i++) {
            Vector2 p = points2d.get(i);
            if (p == null) continue;

            double cx = 0.5, cz = 0.5;
            Vector3 bottom = new Vector3(p.getX() + cx, minY, p.getZ() + cz);
            Vector3 top = new Vector3(p.getX() + cx, topY, p.getZ() + cz);
            edgePoints.addAll(interpolateLine(bottom, top, edgeDensity));
        }

        // ── Vertex markers: full-height column at each vertex (matching TextDisplay PolygonRenderer.renderVertexMarkers) ──
        Color vertexColor = settings.getPolygonVertexColor();
        for (int i = 0; i < n; i++) {
            Vector2 p = points2d.get(i);
            if (p == null) continue;
            // Full-height column from block-min corner to block-max corner
            renderBoxFrame(
                    new Vector3(p.getX(), minY, p.getZ()),
                    new Vector3(p.getX() + 1.0, topY, p.getZ() + 1.0),
                    vertexColor);
        }
    }
}
