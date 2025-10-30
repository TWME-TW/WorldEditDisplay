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
    public void setCylinderCenter(int x, int y, int z) {
        this.center = Vector3.at(x, y, z);
    }

    @Override
    public void setCylinderRadius(double x, double z) {
        this.radiusX = x;
        this.radiusZ = z;
    }

    @Override
    public void setMinMax(int min, int max) {
        this.minY = min;
        this.maxY = max;
    }

    public Vector3 getCenter() {
        return center;
    }

    public double getRadiusX() {
        return radiusX;
    }

    public double getRadiusZ() {
        return radiusZ;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public boolean isDefined() {
        return center != null && radiusX > 0 && radiusZ > 0;
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cylinder Region:\n");
        sb.append("  Center: ").append(center != null ? center.toString() : "Not set").append("\n");
        sb.append("  Radius X: ").append(radiusX).append("\n");
        sb.append("  Radius Z: ").append(radiusZ).append("\n");
        sb.append("  Min Y: ").append(minY).append("\n");
        sb.append("  Max Y: ").append(maxY).append("\n");
        sb.append("  Height: ").append(maxY - minY).append(" blocks\n");
        
        if (isDefined()) {
            // Approximate volume using cylinder formula: π * rx * rz * height
            double volume = Math.PI * radiusX * radiusZ * (maxY - minY);
            sb.append("  Approximate volume: ").append(String.format("%.0f", volume)).append(" blocks");
        }
        
        return sb.toString();
    }
}
