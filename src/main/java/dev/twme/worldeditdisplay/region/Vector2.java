package dev.twme.worldeditdisplay.region;

/**
 * Simple 2D vector class for storing X and Z coordinates
 */
public class Vector2 {
    private final int x;
    private final int z;

    public Vector2(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public static Vector2 at(int x, int z) {
        return new Vector2(x, z);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector2)) return false;
        Vector2 other = (Vector2) obj;
        return x == other.x && z == other.z;
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }
}
