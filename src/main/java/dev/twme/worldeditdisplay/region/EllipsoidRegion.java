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

    /**
     * Set the center point of the ellipsoid
     */
    @Override
    public void setEllipsoidCenter(int x, int y, int z) {
        center = Vector3.at(x, y, z);
        markDirty();
    }

    /**
     * Set the radii of the ellipsoid along X, Y, Z axes
     */
    @Override
    public void setEllipsoidRadii(double x, double y, double z) {
        radii = Vector3.at(x, y, z);
        markDirty();
    }

    public Vector3 getCenter() { return center; }
    public Vector3 getRadii() { return radii; }

    @Override
    public BoundingBox getBoundingBox() {
        if (!isDefined()) return null;
        Vector3 min = Vector3.at(center.getX() - radii.getX(),
                                 center.getY() - radii.getY(),
                                 center.getZ() - radii.getZ());
        Vector3 max = Vector3.at(center.getX() + radii.getX(),
                                 center.getY() + radii.getY(),
                                 center.getZ() + radii.getZ());
        return new BoundingBox(min, max);
    }

    @Override
    public boolean isDefined() {
        return center != null && radii != null;
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder("Ellipsoid Region:\n");
        sb.append("  Center: ").append(center != null ? center : "Not set").append('\n');
        sb.append("  Radii: ").append(radii != null ? radii : "Not set").append('\n');

        if (isDefined()) {
            // approximate volume: (4/3) * π * rx * ry * rz
            double volume = (4.0 / 3.0) * Math.PI * radii.getX() * radii.getY() * radii.getZ();
            sb.append("  Approximate volume: ").append((long) volume).append(" blocks");
        }

        return sb.toString();
    }
}