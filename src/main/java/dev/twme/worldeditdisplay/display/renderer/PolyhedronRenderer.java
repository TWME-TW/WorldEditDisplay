package dev.twme.worldeditdisplay.display.renderer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.PolyhedronRegion;
import dev.twme.worldeditdisplay.region.Vector3;

import org.joml.Vector3f;

public class PolyhedronRenderer extends RegionRenderer<PolyhedronRegion> {

    public PolyhedronRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public void render(PolyhedronRegion region) {
        clear();

        if (!region.isDefined()) return;

        boolean multi = isMultiSelection(region);

        List<Vector3> vertices = region.getVertices();
        List<int[]> faces = region.getFaces();

        if (vertices.isEmpty()) return;

        long validCount = vertices.stream().filter(v -> v != null).count();
        if (validCount == 0) return;

        Material lineMat = getMaterialWithOverride(region, 0, settings.getPolyhedronLineMaterial(), multi);
        Material vertexMat = getMaterialWithOverride(region, 2, settings.getPolyhedronVertexMaterial(), multi);
        Material vertex0Mat = getMaterialWithOverride(region, 3, settings.getPolyhedronVertex0Material(), multi);

        renderVertices(vertices, vertexMat, vertex0Mat);

        if (!faces.isEmpty()) {
            renderFaceEdges(vertices, faces, lineMat);
        }
    }

    private void renderVertices(List<Vector3> vertices, Material vertexMat, Material vertex0Mat) {
        for (int i = 0; i < vertices.size(); i++) {
            Vector3 vertex = vertices.get(i);
            if (vertex == null) continue;

            Material mat = (i == 0) ? vertex0Mat : vertexMat;

            Vector3f center = new Vector3f(
                    (float) (vertex.getX() + 0.5),
                    (float) (vertex.getY() + 0.5),
                    (float) (vertex.getZ() + 0.5)
            );

            renderCube(center, settings.getPolyhedronVertexSize(), mat, settings.getPolyhedronVertexThickness());
        }
    }

    private void renderFaceEdges(List<Vector3> vertices, List<int[]> faces, Material mat) {
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

                renderEdge(vertices.get(v1), vertices.get(v2), mat);
                renderedEdges.add(edgeKey);
            }
        }
    }

    private void renderEdge(Vector3 v1, Vector3 v2, Material mat) {
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

        renderLine(new Line(start, end), mat, settings.getPolyhedronLineThickness());
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