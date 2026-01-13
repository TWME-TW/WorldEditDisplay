package dev.twme.worldeditdisplay.region;

import dev.twme.worldeditdisplay.player.PlayerData;
import org.bukkit.Material;

/**
 * Base class for all region types
 * Stores region data without rendering logic
 */
public abstract class Region {

    protected final PlayerData playerData;
    protected double gridSpacing;

    /**
     * Color material overrides (for rendering)
     * 0 = primary, 1 = secondary, 2 = grid, 3 = background
     * null will return the default
     */
    protected final Material[] colorMaterials = new Material[4];

    protected Region(PlayerData playerData) {
        this.playerData = playerData;
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
    }

    /**
     * Get current grid spacing
     */
    public double getGridSpacing() {
        return this.gridSpacing;
    }

    /**
     * Set color materials override from CUI color event
     *
     * @param materials Array of materials for [primary, secondary, grid, background]
     *                  null elements will use the default material
     */
    public void setColorMaterials(Material[] materials) {
        if (materials == null || materials.length != 4) {
            throw new IllegalArgumentException("Color materials array must have exactly 4 elements");
        }
        System.arraycopy(materials, 0, this.colorMaterials, 0, 4);
    }

    /**
     * Get color material override for specific index
     *
     * @param index Material index (0=primary, 1=secondary, 2=grid, 3=background)
     * @return Material override, or null if using default
     */
    public Material getColorMaterial(int index) {
        return (index >= 0 && index < 4) ? colorMaterials[index] : null;
    }

    /**
     * Clear all color material overrides
     */
    public void clearColorMaterials() {
        for (int i = 0; i < 4; i++) {
            colorMaterials[i] = null;
        }
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
}
