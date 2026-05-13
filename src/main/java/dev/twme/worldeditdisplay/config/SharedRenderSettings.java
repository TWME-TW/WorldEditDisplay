package dev.twme.worldeditdisplay.config;

import org.bukkit.Color;

import dev.twme.worldeditdisplay.WorldEditDisplay;

/**
 * A read-only {@link PlayerRenderSettings} subclass used when rendering
 * another player's selection for a viewer.
 *
 * All colour getters return the configured tint colour so the shared
 * selection is visually distinct from the viewer's own selection.
 * Structural settings (thickness, segments, etc.) still fall back to the
 * server defaults via the parent class.
 */
public class SharedRenderSettings extends PlayerRenderSettings {

    private final Color tintColor;
    private final Color fillColor;

    /**
     * @param plugin    the plugin instance
     * @param tintColor the colour to use for all line / edge / vertex colours
     */
    public SharedRenderSettings(WorldEditDisplay plugin, Color tintColor) {
        super(plugin); // no-load protected constructor
        this.tintColor = tintColor;
        // Fill colour uses the same hue but 20 % opacity (alpha ≈ 50/255)
        this.fillColor = Color.fromARGB(50, tintColor.getRed(), tintColor.getGreen(), tintColor.getBlue());
    }

    // ─── Override: always see-through for shared renders ─────────────────────
    @Override public boolean isCuboidSeeThrough()    { return true; }
    @Override public boolean isCylinderSeeThrough()  { return true; }
    @Override public boolean isEllipsoidSeeThrough() { return true; }
    @Override public boolean isPolygonSeeThrough()   { return true; }
    @Override public boolean isPolyhedronSeeThrough(){ return true; }

    // ─── Override: fills disabled for shared renders ──────────────────────────
    @Override public boolean isCuboidFillEnabled()    { return false; }
    @Override public boolean isCylinderFillEnabled()  { return false; }
    @Override public boolean isEllipsoidFillEnabled() { return false; }
    @Override public boolean isPolygonFillEnabled()   { return false; }
    @Override public boolean isPolyhedronFillEnabled(){ return false; }

    // ─── Fill colours (used if fill is ever enabled elsewhere) ───────────────
    @Override public Color getCuboidFillColor()    { return fillColor; }
    @Override public Color getCylinderFillColor()  { return fillColor; }
    @Override public Color getEllipsoidFillColor() { return fillColor; }
    @Override public Color getPolygonFillColor()   { return fillColor; }
    @Override public Color getPolyhedronFillColor(){ return fillColor; }

    // ─── Cuboid colours ───────────────────────────────────────────────────────
    @Override public Color getCuboidEdgeColor()   { return tintColor; }
    @Override public Color getCuboidPoint1Color() { return tintColor; }
    @Override public Color getCuboidPoint2Color() { return tintColor; }
    @Override public Color getCuboidGridColor()   { return tintColor; }

    // ─── Cylinder colours ────────────────────────────────────────────────────
    @Override public Color getCylinderCircleColor()     { return tintColor; }
    @Override public Color getCylinderGridColor()       { return tintColor; }
    @Override public Color getCylinderCenterColor()     { return tintColor; }
    @Override public Color getCylinderCenterLineColor() { return tintColor; }

    // ─── Ellipsoid colours ───────────────────────────────────────────────────
    @Override public Color getEllipsoidLineColor()       { return tintColor; }
    @Override public Color getEllipsoidCenterLineColor() { return tintColor; }
    @Override public Color getEllipsoidCenterColor()     { return tintColor; }

    // ─── Polygon colours ─────────────────────────────────────────────────────
    @Override public Color getPolygonEdgeColor()     { return tintColor; }
    @Override public Color getPolygonVertexColor()   { return tintColor; }
    @Override public Color getPolygonVerticalColor() { return tintColor; }

    // ─── Polyhedron colours ──────────────────────────────────────────────────
    @Override public Color getPolyhedronLineColor()    { return tintColor; }
    @Override public Color getPolyhedronVertex0Color() { return tintColor; }
    @Override public Color getPolyhedronVertexColor()  { return tintColor; }
}
