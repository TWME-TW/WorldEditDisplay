package dev.twme.worldeditdisplay.display.renderer;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.PolygonRegion;
import dev.twme.worldeditdisplay.region.Vector2;

public class PolygonRenderer extends RegionRenderer<PolygonRegion> {

    public PolygonRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public void render(PolygonRegion region) {
        clear();

        if (!region.isDefined()) return;

        boolean multi = isMultiSelection(region);

        List<Vector2> points = region.getPoints().stream()
                .filter(p -> p != null)
                .toList();
        if (points.isEmpty()) return;

        int minY = region.getMinY();
        int maxY = region.getMaxY();

        if (points.size() == 1) {
            Material vertexMat = getMaterialWithOverride(region, 2, settings.getPolygonVertexMaterial(), multi);
            renderVertexMarkers(points, minY, maxY, vertexMat);
            return;
        }

        int height = maxY - minY + 1;
        int step = calculateGridStep(height);

        Material edgeMat = getMaterialWithOverride(region, 0, settings.getPolygonEdgeMaterial(), multi);
        Material verticalMat = settings.getPolygonVerticalMaterial();
        Material vertexMat = getMaterialWithOverride(region, 2, settings.getPolygonVertexMaterial(), multi);

        // Render horizontal edges
        for (int y = minY; y <= maxY + 1; y += step) {
            renderPolygonEdges(points, y, edgeMat, settings.getPolygonEdgeThickness());
        }
        if ((maxY + 1 - minY) % step != 0) {
            renderPolygonEdges(points, maxY + 1, edgeMat, settings.getPolygonEdgeThickness());
        }

        // Render vertical edges
        renderVerticalEdges(points, minY, maxY, verticalMat);

        // Render vertex markers
        renderVertexMarkers(points, minY, maxY, vertexMat);
    }

    private int calculateGridStep(int height) {
        int step = Math.max(1, height / settings.getPolygonHeightGridDivision());
        if (settings.getPolygonMaxGridSpacing() != -1) step = Math.min(step, settings.getPolygonMaxGridSpacing());
        return step;
    }

    private void renderPolygonEdges(List<Vector2> points, int y, Material mat, float thickness) {
        int size = points.size();
        for (int i = 0; i < size; i++) {
            Vector2 curr = points.get(i);
            Vector2 next = points.get((i + 1) % size);

            Vector3f start = new Vector3f(curr.getX() + 0.5f, y, curr.getZ() + 0.5f);
            Vector3f end = new Vector3f(next.getX() + 0.5f, y, next.getZ() + 0.5f);

            renderLine(new Line(start, end), mat, thickness);
        }
    }

    private void renderVerticalEdges(List<Vector2> points, int minY, int maxY, Material mat) {
        for (Vector2 point : points) {
            Vector3f start = new Vector3f(point.getX() + 0.5f, minY, point.getZ() + 0.5f);
            Vector3f end = new Vector3f(point.getX() + 0.5f, maxY + 1f, point.getZ() + 0.5f);
            renderLine(new Line(start, end), mat, settings.getPolygonVerticalThickness());
        }
    }

    private void renderVertexMarkers(List<Vector2> points, int minY, int maxY, Material mat) {
        float thickness = 0.05f;
        for (Vector2 point : points) {
            double minX = point.getX();
            double minYPos = minY;
            double minZ = point.getZ();
            double maxX = minX + 1.0;
            double maxYPos = maxY + 1.0;
            double maxZ = minZ + 1.0;
            renderBoxFrame(minX, minYPos, minZ, maxX, maxYPos, maxZ, mat, thickness);
        }
    }

    @Override
    public Class<PolygonRegion> getRegionType() {
        return PolygonRegion.class;
    }
}