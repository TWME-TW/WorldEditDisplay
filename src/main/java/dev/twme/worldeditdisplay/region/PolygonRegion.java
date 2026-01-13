package dev.twme.worldeditdisplay.region;

import dev.twme.worldeditdisplay.player.PlayerData;
import java.util.ArrayList;
import java.util.List;

/**
 * 2D Polygon region (extends vertically)
 */
public class PolygonRegion extends Region {

    private final List<Vector2> points = new ArrayList<>();
    private int minY;
    private int maxY;

    public PolygonRegion(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public RegionType getType() {
        return RegionType.POLYGON;
    }

    /**
     * Set a 2D polygon point (for polygon regions)
     */
    @Override
    public void setPolygonPoint(int id, int x, int z) {
        while (points.size() <= id) points.add(null);
        points.set(id, Vector2.at(x, z));
    }

    /**
     * Set min and max Y for vertical extension of polygon
     */
    @Override
    public void setMinMax(int min, int max) {
        minY = min;
        maxY = max;
    }

    public List<Vector2> getPoints() { return new ArrayList<>(points); }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }

    @Override
    public boolean isDefined() {
        return !points.isEmpty() && points.stream().anyMatch(p -> p != null) && maxY > minY;
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder("Polygon Region:\n");
        sb.append("  Points: ").append(points.size()).append('\n');
        sb.append("  Min Y: ").append(minY).append('\n');
        sb.append("  Max Y: ").append(maxY).append('\n');
        sb.append("  Height: ").append(maxY - minY).append(" blocks\n");

        int validPoints = 0;
        for (int i = 0; i < points.size(); i++) {
            Vector2 point = points.get(i);
            if (point != null) {
                sb.append("    Point ").append(i).append(": ").append(point).append('\n');
                validPoints++;
            }
        }

        sb.append("  Valid points: ").append(validPoints);
        return sb.toString();
    }
}