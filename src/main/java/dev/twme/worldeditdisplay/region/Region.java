package dev.twme.worldeditdisplay.region;

import org.bukkit.Color;

import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Base class for all region types
 * Stores region data without rendering logic
 */
public abstract class Region {

    protected final PlayerData playerData;
    protected double gridSpacing;
    protected boolean dirty = true;

    /**
     * Color overrides (for rendering)
     * 0 = primary, 1 = secondary, 2 = grid, 3 = background
     * null will return the default
     */
    protected final Color[] overrideColors = new Color[4];

    protected Region(PlayerData playerData) {
        this.playerData = playerData;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public void markDirty() {
        dirty = true;
    }

    /**
     * Get the type of this region
     */
    public abstract RegionType getType();

    /**
     * Check if the region is fully defined (has all required points/data)
     * Subclasses should override this to implement their specific logic
     */
    public abstract boolean isDefined();

    /**
     * Set grid spacing for visualization
     */
    public void setGridSpacing(double spacing) {
        this.gridSpacing = spacing;
        markDirty();
    }

    /**
     * Get current grid spacing
     */
    public double getGridSpacing() {
        return this.gridSpacing;
    }

    /**
     * Set color overrides from CUI color event
     *
     * @param colors Array of colors for [primary, secondary, grid, background]
     *               null elements will use the default color
     */
    public void setOverrideColors(Color[] colors) {
        if (colors == null || colors.length != 4) {
            throw new IllegalArgumentException("Override colors array must have exactly 4 elements");
        }
        System.arraycopy(colors, 0, this.overrideColors, 0, 4);
        markDirty();
    }

    /**
     * Get color override for specific index
     *
     * @param index Color index (0=primary, 1=secondary, 2=grid, 3=background)
     * @return Color override, or null if using default
     */
    public Color getOverrideColor(int index) {
        return (index >= 0 && index < 4) ? overrideColors[index] : null;
    }

    /**
     * Clear all color overrides
     */
    public void clearOverrideColors() {
        for (int i = 0; i < 4; i++) {
            overrideColors[i] = null;
        }
        markDirty();
    }

    /**
     * Set a cuboid point (for cuboid regions)
     */
    public void setCuboidPoint(int id, double x, double y, double z) {
        unsupported("setCuboidPoint");
    }

    /**
     * Set a 2D polygon point (for polygon regions)
     */
    public void setPolygonPoint(int id, int x, int z) {
        unsupported("setPolygonPoint");
    }

    /**
     * Set min/max Y bounds (for polygon regions)
     */
    public void setMinMax(int min, int max) {
        unsupported("setMinMax");
    }

    /**
     * Set ellipsoid center
     */
    public void setEllipsoidCenter(int x, int y, int z) {
        unsupported("setEllipsoidCenter");
    }

    /**
     * Set ellipsoid radii
     */
    public void setEllipsoidRadii(double x, double y, double z) {
        unsupported("setEllipsoidRadii");
    }

    /**
     * Set cylinder center
     */
    public void setCylinderCenter(int x, int y, int z) {
        unsupported("setCylinderCenter");
    }

    /**
     * Set cylinder radius
     */
    public void setCylinderRadius(double x, double z) {
        unsupported("setCylinderRadius");
    }

    /**
     * Add a polygon face (for polyhedron regions)
     */
    public void addPolygon(int[] vertexIds) {
        unsupported("addPolygon");
    }

    protected void unsupported(String method) {
        throw new UnsupportedOperationException(
                method + " is not supported for " + getType().getName()
        );
    }

    /**
     * Get a string representation of the region data
     */
    public abstract String getInfo();

    /**
     * Get the axis-aligned bounding box of this region.
     * Returns {@code null} if the region is not fully defined.
     * Non-cuboid shapes are approximated by their AABB.
     */
    public BoundingBox getBoundingBox() {
        return null;
    }
}
