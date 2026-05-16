package dev.twme.worldeditdisplay.region;

/**
 * Simple bounding box class
 */
public class BoundingBox {
    private final Vector3 min;
    private final Vector3 max;
    private final Vector3 center;
    private final double halfDiagonal;

    public BoundingBox(Vector3 min, Vector3 max) {
        this.min = min;
        this.max = max;
        this.center = new Vector3(
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);
        double w = max.getX() - min.getX();
        double h = max.getY() - min.getY();
        double l = max.getZ() - min.getZ();
        this.halfDiagonal = Math.sqrt(w * w + h * h + l * l) / 2.0;
    }

    public static BoundingBox of(Vector3 pos1, Vector3 pos2) {
        return new BoundingBox(pos1.getMinimum(pos2), pos1.getMaximum(pos2));
    }

    public Vector3 getMin() {
        return min;
    }

    public Vector3 getMax() {
        return max;
    }

    public double getWidth() {
        return max.getX() - min.getX();
    }

    public double getHeight() {
        return max.getY() - min.getY();
    }

    public double getLength() {
        return max.getZ() - min.getZ();
    }

    public Vector3 getCenter() {
        return center;
    }

    public long getVolume() {
        return (long) (getWidth() * getHeight() * getLength());
    }

    /** Half-length of the space diagonal: sqrt(w²+h²+l²) / 2 */
    public double getHalfDiagonal() {
        return halfDiagonal;
    }

    /** Returns true when the point lies inside (or on the surface of) this box. */
    public boolean contains(Vector3 p) {
        return p.getX() >= min.getX() && p.getX() <= max.getX()
            && p.getY() >= min.getY() && p.getY() <= max.getY()
            && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }

    /**
     * Euclidean distance from point {@code p} to the nearest surface/edge/corner of this box.
     * Returns 0 if the point is inside.
     */
    public double distanceTo(Vector3 p) {
        double dx = Math.max(0, Math.max(min.getX() - p.getX(), p.getX() - max.getX()));
        double dy = Math.max(0, Math.max(min.getY() - p.getY(), p.getY() - max.getY()));
        double dz = Math.max(0, Math.max(min.getZ() - p.getZ(), p.getZ() - max.getZ()));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return String.format("BoundingBox{min=%s, max=%s}", min, max);
    }
}
