package dev.twme.worldeditdisplay.display.particle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.PolyhedronRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * Particle-based renderer for PolyhedronRegion.
 * <p>
 * Renders the edges of all triangular faces.
 * Shared edges between adjacent faces are de-duplicated to avoid
 * overlapping particle points.
 */
public class ParticlePolyhedronRenderer extends ParticleRenderer<PolyhedronRegion> {

    public ParticlePolyhedronRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public Class<PolyhedronRegion> getRegionType() {
        return PolyhedronRegion.class;
    }

    @Override
    public void render(PolyhedronRegion region) {
        clear();

        List<Vector3> vertices = region.getVertices();
        List<int[]> faces = region.getFaces();

        if (vertices.isEmpty() || faces.isEmpty()) return;

        // De-duplicate edges using a set of ordered pairs (smaller index, larger index)
        Set<String> edgeSet = new HashSet<>();

        for (int[] face : faces) {
            if (face == null || face.length < 3) continue;

            for (int i = 0; i < face.length; i++) {
                int a = face[i];
                int b = face[(i + 1) % face.length];

                if (a < 0 || a >= vertices.size() || b < 0 || b >= vertices.size()) continue;
                if (vertices.get(a) == null || vertices.get(b) == null) continue;

                // Normalise edge key: smaller index first
                int min = Math.min(a, b);
                int max = Math.max(a, b);
                String key = min + ":" + max;

                if (edgeSet.add(key)) {
                    // New edge – interpolate with +0.5 offset on both endpoints
                    // so the edge passes through the centre of the outermost blocks
                    // (matching TextDisplay PolyhedronRenderer's convention).
                    Vector3 vA = vertices.get(min);
                    Vector3 vB = vertices.get(max);
                    Vector3 offsetA = new Vector3(vA.getX() + 0.5, vA.getY() + 0.5, vA.getZ() + 0.5);
                    Vector3 offsetB = new Vector3(vB.getX() + 0.5, vB.getY() + 0.5, vB.getZ() + 0.5);
                    edgePoints.addAll(interpolateLine(offsetA, offsetB, edgeDensity));
                }
            }
        }

        // ── Vertex markers (small cubes matching TextDisplay PolyhedronRenderer.renderVertices) ──
        Color vertexColor = settings.getPolyhedronVertexColor();
        double vertexSize = settings.getPolyhedronVertexSize();
        for (int i = 0; i < vertices.size(); i++) {
            Vector3 v = vertices.get(i);
            if (v == null) continue;
            Color color = (i == 0) ? settings.getPolyhedronVertex0Color() : vertexColor;
            Vector3 markerPos = new Vector3(v.getX() + 0.5, v.getY() + 0.5, v.getZ() + 0.5);
            renderSmallCube(markerPos, color, vertexSize);
        }
    }
}
