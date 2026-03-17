package dev.twme.worldeditdisplay.display.renderer;

import java.util.List;

import org.bukkit.Color;
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
            Color vertexColor = getColorWithOverride(region, 2, settings.getPolygonVertexColor(), multi);
            renderVertexMarkers(points, minY, maxY, vertexColor);
            return;
        }

        int height = maxY - minY + 1;
        int step = calculateGridStep(height);

        Color edgeColor = getColorWithOverride(region, 0, settings.getPolygonEdgeColor(), multi);
        Color verticalColor = settings.getPolygonVerticalColor();
        Color vertexColor = getColorWithOverride(region, 2, settings.getPolygonVertexColor(), multi);

        // Render horizontal edges
        for (int y = minY; y <= maxY + 1; y += step) {
            renderPolygonEdges(points, y, edgeColor, settings.getPolygonEdgeThickness());
        }
        if ((maxY + 1 - minY) % step != 0) {
            renderPolygonEdges(points, maxY + 1, edgeColor, settings.getPolygonEdgeThickness());
        }

        // Render vertical edges
        renderVerticalEdges(points, minY, maxY, verticalColor);

        // Render vertex markers
        renderVertexMarkers(points, minY, maxY, vertexColor);

        // Render fill faces if enabled
        if (settings.isPolygonFillEnabled() && points.size() >= 3) {
            Color fillColor = getFillColorWithOverride(region, 1, settings.getPolygonFillColor(), multi);
            renderFillFaces(points, minY, maxY, fillColor);
        }
    }

    private int calculateGridStep(int height) {
        int step = Math.max(1, height / settings.getPolygonHeightGridDivision());
        if (settings.getPolygonMaxGridSpacing() != -1) step = Math.min(step, settings.getPolygonMaxGridSpacing());
        return step;
    }

    private void renderPolygonEdges(List<Vector2> points, int y, Color color, float thickness) {
        int size = points.size();
        for (int i = 0; i < size; i++) {
            Vector2 curr = points.get(i);
            Vector2 next = points.get((i + 1) % size);

            Vector3f start = new Vector3f(curr.getX() + 0.5f, y, curr.getZ() + 0.5f);
            Vector3f end = new Vector3f(next.getX() + 0.5f, y, next.getZ() + 0.5f);

            renderLine(new Line(start, end), color, thickness);
        }
    }

    private void renderVerticalEdges(List<Vector2> points, int minY, int maxY, Color color) {
        for (Vector2 point : points) {
            Vector3f start = new Vector3f(point.getX() + 0.5f, minY, point.getZ() + 0.5f);
            Vector3f end = new Vector3f(point.getX() + 0.5f, maxY + 1f, point.getZ() + 0.5f);
            renderLine(new Line(start, end), color, settings.getPolygonVerticalThickness());
        }
    }

    private void renderVertexMarkers(List<Vector2> points, int minY, int maxY, Color color) {
        float thickness = 0.05f;
        for (Vector2 point : points) {
            double minX = point.getX();
            double minYPos = minY;
            double minZ = point.getZ();
            double maxX = minX + 1.0;
            double maxYPos = maxY + 1.0;
            double maxZ = minZ + 1.0;
            renderBoxFrame(minX, minYPos, minZ, maxX, maxYPos, maxZ, color, thickness);
        }
    }

    private void renderFillFaces(List<Vector2> points, int minY, int maxY, Color fillColor) {
        int size = points.size();
        float topY = maxY + 1f;

        // Side walls: each edge is a rectangle, rendered as a single parallelogram
        for (int i = 0; i < size; i++) {
            Vector2 curr = points.get(i);
            Vector2 next = points.get((i + 1) % size);
            Vector3f bottomCurr = new Vector3f(curr.getX() + 0.5f, minY, curr.getZ() + 0.5f);
            Vector3f bottomNext = new Vector3f(next.getX() + 0.5f, minY, next.getZ() + 0.5f);
            Vector3f topCurr   = new Vector3f(curr.getX() + 0.5f, topY, curr.getZ() + 0.5f);
            // p1=bottomCurr, p2=bottomNext (width), p3=topCurr (height); 4th corner=topNext auto
            renderParallelogram(bottomCurr, bottomNext, topCurr, fillColor);
        }

        // Top and bottom caps: arbitrary polygon, use fan triangulation
        for (int i = 1; i < size - 1; i++) {
            Vector3f p1Bottom = new Vector3f(points.get(0).getX() + 0.5f, minY, points.get(0).getZ() + 0.5f);
            Vector3f p2Bottom = new Vector3f(points.get(i).getX() + 0.5f, minY, points.get(i).getZ() + 0.5f);
            Vector3f p3Bottom = new Vector3f(points.get(i + 1).getX() + 0.5f, minY, points.get(i + 1).getZ() + 0.5f);

            Vector3f p1Top = new Vector3f(p1Bottom.x, topY, p1Bottom.z);
            Vector3f p2Top = new Vector3f(p2Bottom.x, topY, p2Bottom.z);
            Vector3f p3Top = new Vector3f(p3Bottom.x, topY, p3Bottom.z);

            renderTriangle(p1Bottom, p2Bottom, p3Bottom, fillColor);
            renderTriangle(p1Top, p2Top, p3Top, fillColor);
        }
    }

    @Override
    public Class<PolygonRegion> getRegionType() {
        return PolygonRegion.class;
    }
}