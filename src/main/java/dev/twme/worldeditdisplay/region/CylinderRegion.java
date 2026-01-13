package dev.twme.worldeditdisplay.region;

import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Cylinder region
 */
public class CylinderRegion extends Region {

    private Vector3 center;
    private double radiusX;
    private double radiusZ;
    private int minY;
    private int maxY;

    public CylinderRegion(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public RegionType getType() {
        return RegionType.CYLINDER;
    }

    @Override
    public void setCylinderCenter(int x, int y, int z) { center = Vector3.at(x, y, z); }

    @Override
    public void setCylinderRadius(double x, double z) { radiusX = x; radiusZ = z; }

    @Override
    public void setMinMax(int min, int max) { minY = min; maxY = max; }

    public Vector3 getCenter() { return center; }
    public double getRadiusX() { return radiusX; }
    public double getRadiusZ() { return radiusZ; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }

    @Override
    public boolean isDefined() {
        return center != null && radiusX > 0 && radiusZ > 0 && maxY > minY;
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder("Cylinder Region:\n");
        sb.append("  Center: ").append(center != null ? center : "Not set").append('\n');
        sb.append("  Radius X: ").append(radiusX).append('\n');
        sb.append("  Radius Z: ").append(radiusZ).append('\n');
        sb.append("  Min Y: ").append(minY).append('\n');
        sb.append("  Max Y: ").append(maxY).append('\n');

        if (isDefined()) {
            int height = maxY - minY;
            double volume = Math.PI * radiusX * radiusZ * height;
            sb.append("  Height: ").append(height).append(" blocks\n");
            sb.append("  Approximate volume: ").append((long) volume).append(" blocks");
        }

        return sb.toString();
    }
}
