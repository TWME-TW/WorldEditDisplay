package dev.twme.worldeditdisplay.region;

import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Ellipsoid (sphere/oval) region
 */
public class EllipsoidRegion extends Region {
    private Vector3 center;
    private Vector3 radii;

    public EllipsoidRegion(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public RegionType getType() {
        return RegionType.ELLIPSOID;
    }

    @Override
    public void setEllipsoidCenter(int x, int y, int z) {
        this.center = Vector3.at(x, y, z);
    }

    @Override
    public void setEllipsoidRadii(double x, double y, double z) {
        this.radii = Vector3.at(x, y, z);
    }

    public Vector3 getCenter() {
        return center;
    }

    public Vector3 getRadii() {
        return radii;
    }

    public boolean isDefined() {
        return center != null && radii != null;
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ellipsoid Region:\n");
        sb.append("  Center: ").append(center != null ? center.toString() : "Not set").append("\n");
        sb.append("  Radii: ").append(radii != null ? radii.toString() : "Not set").append("\n");
        
        if (isDefined()) {
            // Approximate volume using ellipsoid formula: (4/3) * π * rx * ry * rz
            double volume = (4.0 / 3.0) * Math.PI * radii.getX() * radii.getY() * radii.getZ();
            sb.append("  Approximate volume: ").append(String.format("%.0f", volume)).append(" blocks");
        }
        
        return sb.toString();
    }
}
