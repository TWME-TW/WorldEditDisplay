package dev.twme.worldeditdisplay.region;

import org.bukkit.Material;

import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Base class for all region types
 * This class stores region data without rendering logic
 */
public abstract class Region {
    protected final PlayerData playerData;
    protected double gridSpacing = 0;
    
    // 顏色材質覆寫（來自 CUI col 事件）
    // null 表示使用預設材質
    protected Material[] colorMaterials = new Material[4];

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
    public boolean isDefined() {
        return false;  // Default to false, subclasses should override
    }

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
     *                  null elements mean use default material
     */
    public void setColorMaterials(Material[] materials) {
        if (materials == null || materials.length != 4) {
            throw new IllegalArgumentException("Color materials array must have exactly 4 elements");
        }
        this.colorMaterials = materials.clone();
    }
    
    /**
     * Get color material override for specific index
     * 
     * @param index Material index (0=primary, 1=secondary, 2=grid, 3=background)
     * @return Material override, or null if using default
     */
    public Material getColorMaterial(int index) {
        if (index < 0 || index >= 4) {
            return null;
        }
        return colorMaterials[index];
    }
    
    /**
     * Clear all color material overrides
     */
    public void clearColorMaterials() {
        this.colorMaterials = new Material[4];
    }

    /**
     * Set a cuboid point (for cuboid regions)
     */
    public void setCuboidPoint(int id, double x, double y, double z) {
        throw new UnsupportedOperationException(
                "setCuboidPoint is not supported for " + getType().getName());
    }

    /**
     * Set a 2D polygon point (for polygon regions)
     */
    public void setPolygonPoint(int id, int x, int z) {
        throw new UnsupportedOperationException(
                "setPolygonPoint is not supported for " + getType().getName());
    }

    /**
     * Set min/max Y bounds (for polygon regions)
     */
    public void setMinMax(int min, int max) {
        throw new UnsupportedOperationException(
                "setMinMax is not supported for " + getType().getName());
    }

    /**
     * Set ellipsoid center
     */
    public void setEllipsoidCenter(int x, int y, int z) {
        throw new UnsupportedOperationException(
                "setEllipsoidCenter is not supported for " + getType().getName());
    }

    /**
     * Set ellipsoid radii
     */
    public void setEllipsoidRadii(double x, double y, double z) {
        throw new UnsupportedOperationException(
                "setEllipsoidRadii is not supported for " + getType().getName());
    }

    /**
     * Set cylinder center
     */
    public void setCylinderCenter(int x, int y, int z) {
        throw new UnsupportedOperationException(
                "setCylinderCenter is not supported for " + getType().getName());
    }

    /**
     * Set cylinder radius
     */
    public void setCylinderRadius(double x, double z) {
        throw new UnsupportedOperationException(
                "setCylinderRadius is not supported for " + getType().getName());
    }

    /**
     * Add a polygon face (for polyhedron regions)
     */
    public void addPolygon(int[] vertexIds) {
        throw new UnsupportedOperationException(
                "addPolygon is not supported for " + getType().getName());
    }

    /**
     * Get a string representation of the region data
     */
    public abstract String getInfo();
}
