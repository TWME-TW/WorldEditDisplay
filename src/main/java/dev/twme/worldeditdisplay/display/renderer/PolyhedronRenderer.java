package dev.twme.worldeditdisplay.display.renderer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.PolyhedronRegion;
import dev.twme.worldeditdisplay.region.Vector3;

public class PolyhedronRenderer extends RegionRenderer<PolyhedronRegion> {

    public PolyhedronRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    protected boolean isSeeThrough() {
        return settings.isPolyhedronSeeThrough();
    }

    @Override
    public void render(PolyhedronRegion region) {
        clear();

        List<Vector3> vertices = region.getVertices();
        if (vertices.isEmpty()) return;

        long validCount = vertices.stream().filter(v -> v != null).count();
        if (validCount == 0) return;

        boolean multi = isMultiSelection(region);

        List<int[]> faces = region.getFaces();

        Color vertexColor = getColorWithOverride(region, 2, settings.getPolyhedronVertexColor(), multi);
        Color vertex0Color = getColorWithOverride(region, 3, settings.getPolyhedronVertex0Color(), multi);

        renderVertices(vertices, vertexColor, vertex0Color);

        if (!faces.isEmpty()) {
            Color lineColor = getColorWithOverride(region, 0, settings.getPolyhedronLineColor(), multi);
            renderFaceEdges(vertices, faces, lineColor);

            // Render fill faces if enabled
            if (settings.isPolyhedronFillEnabled()) {
                Color fillColor = getFillColorWithOverride(region, 1, settings.getPolyhedronFillColor(), multi);
                renderFillFaces(vertices, faces, fillColor);
            }
        }
    }

    private void renderVertices(List<Vector3> vertices, Color vertexColor, Color vertex0Color) {
        for (int i = 0; i < vertices.size(); i++) {
            Vector3 vertex = vertices.get(i);
            if (vertex == null) continue;

            Color color = (i == 0) ? vertex0Color : vertexColor;

            Vector3f center = new Vector3f(
                    (float) (vertex.getX() + 0.5),
                    (float) (vertex.getY() + 0.5),
                    (float) (vertex.getZ() + 0.5)
            );

            renderCube(center, settings.getPolyhedronVertexSize(), color, settings.getPolyhedronVertexThickness());
        }
    }

    private void renderFaceEdges(List<Vector3> vertices, List<int[]> faces, Color color) {
        Set<String> renderedEdges = new HashSet<>();

        for (int[] face : faces) {
            if (face == null || face.length < 2) continue;

            for (int i = 0; i < face.length; i++) {
                int v1 = face[i];
                int v2 = face[(i + 1) % face.length];

                if (v1 < 0 || v1 >= vertices.size() || v2 < 0 || v2 >= vertices.size()) continue;
                if (vertices.get(v1) == null || vertices.get(v2) == null) continue;

                String edgeKey = getEdgeKey(v1, v2);
                if (renderedEdges.contains(edgeKey)) continue;

                renderEdge(vertices.get(v1), vertices.get(v2), color);
                renderedEdges.add(edgeKey);
            }
        }
    }

    private void renderEdge(Vector3 v1, Vector3 v2, Color color) {
        Vector3f start = new Vector3f(
                (float) (v1.getX() + 0.5),
                (float) (v1.getY() + 0.5),
                (float) (v1.getZ() + 0.5)
        );

        Vector3f end = new Vector3f(
                (float) (v2.getX() + 0.5),
                (float) (v2.getY() + 0.5),
                (float) (v2.getZ() + 0.5)
        );

        renderLine(new Line(start, end), color, settings.getPolyhedronLineThickness());
    }

    private void renderFillFaces(List<Vector3> vertices, List<int[]> faces, Color fillColor) {
        for (int[] face : faces) {
            if (face == null || face.length < 3) continue;

            // Fan triangulation from first vertex of each face
            for (int i = 1; i < face.length - 1; i++) {
                int v0 = face[0], v1 = face[i], v2 = face[i + 1];
                if (v0 < 0 || v0 >= vertices.size() || v1 < 0 || v1 >= vertices.size() || v2 < 0 || v2 >= vertices.size()) continue;
                if (vertices.get(v0) == null || vertices.get(v1) == null || vertices.get(v2) == null) continue;

                Vector3f p0 = toVec3f(vertices.get(v0));
                Vector3f p1 = toVec3f(vertices.get(v1));
                Vector3f p2 = toVec3f(vertices.get(v2));

                renderTriangle(p0, p1, p2, fillColor);
            }
        }
    }

    private Vector3f toVec3f(Vector3 v) {
        return new Vector3f((float) (v.getX() + 0.5), (float) (v.getY() + 0.5), (float) (v.getZ() + 0.5));
    }

    private String getEdgeKey(int i1, int i2) {
        int min = Math.min(i1, i2);
        int max = Math.max(i1, i2);
        return min + "-" + max;
    }

    @Override
    public Class<PolyhedronRegion> getRegionType() {
        return PolyhedronRegion.class;
    }
}