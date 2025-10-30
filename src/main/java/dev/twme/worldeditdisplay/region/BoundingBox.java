package dev.twme.worldeditdisplay.region;

/**
 * Simple bounding box class
 */
public class BoundingBox {
    private final Vector3 min;
    private final Vector3 max;

    public BoundingBox(Vector3 min, Vector3 max) {
        this.min = min;
        this.max = max;
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
        return new Vector3(
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2
        );
    }

    public long getVolume() {
        return (long) (getWidth() * getHeight() * getLength());
    }

    @Override
    public String toString() {
        return String.format("BoundingBox{min=%s, max=%s}", min, max);
    }
}
