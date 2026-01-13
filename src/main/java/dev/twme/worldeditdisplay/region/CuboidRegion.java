package dev.twme.worldeditdisplay.region;

import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Cuboid (rectangular box) region
 */
public class CuboidRegion extends Region {

    private Vector3 point1;
    private Vector3 point2;

    public CuboidRegion(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public RegionType getType() {
        return RegionType.CUBOID;
    }

    @Override
    public void setCuboidPoint(int id, double x, double y, double z) {
        Vector3 point = Vector3.at(x, y, z);
        if (id == 0) point1 = point;
        else if (id == 1) point2 = point;
    }

    public Vector3 getPoint1() { return point1; }
    public Vector3 getPoint2() { return point2; }

    /**
     * Get the bounding box for this cuboid.
     * Returns null if the points are not fully set
     */
    public BoundingBox getBoundingBox() {
        return isDefined() ? BoundingBox.of(point1, point2) : null;
    }

    @Override
    public boolean isDefined() {
        return point1 != null && point2 != null;
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder("Cuboid Region:\n");
        sb.append("  Point 1: ").append(point1 != null ? point1 : "Not set").append('\n');
        sb.append("  Point 2: ").append(point2 != null ? point2 : "Not set").append('\n');

        if (isDefined()) {
            BoundingBox box = getBoundingBox();
            sb.append("  Volume: ").append(box.getVolume()).append(" blocks\n");
            sb.append("  Dimensions: ")
                    .append((int) box.getWidth()).append(" x ")
                    .append((int) box.getHeight()).append(" x ")
                    .append((int) box.getLength());
        }

        return sb.toString();
    }
}
