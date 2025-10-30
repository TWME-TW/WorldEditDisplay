package dev.twme.worldeditdisplay.event;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import dev.twme.worldeditdisplay.event.events.CUIEventBounds;
import dev.twme.worldeditdisplay.event.events.CUIEventColour;
import dev.twme.worldeditdisplay.event.events.CUIEventCylinder;
import dev.twme.worldeditdisplay.event.events.CUIEventEllipsoid;
import dev.twme.worldeditdisplay.event.events.CUIEventGrid;
import dev.twme.worldeditdisplay.event.events.CUIEventPoint2D;
import dev.twme.worldeditdisplay.event.events.CUIEventPoint3D;
import dev.twme.worldeditdisplay.event.events.CUIEventPolygon;
import dev.twme.worldeditdisplay.event.events.CUIEventSelection;
import dev.twme.worldeditdisplay.event.events.CUIEventUpdate;

/**
 * Event type enum for CUI events. Also stores class, arguments, and key for each value.
 */
public enum CUIEventType {
    SELECTION(CUIEventSelection::new, "Selection", "s", 1, 2),
    POINT(CUIEventPoint3D::new, "Point3D", "p", 5, 6),
    POINT2D(CUIEventPoint2D::new, "Point2D", "p2", 4, 5),
    ELLIPSOID(CUIEventEllipsoid::new, "Ellipsoid", "e", 4),
    CYLINDER(CUIEventCylinder::new, "Cylinder", "cyl", 5),
    MINMAX(CUIEventBounds::new, "Bounds", "mm", 2),
    UPDATE(CUIEventUpdate::new, "Update", "u", 1),
    POLYGON(CUIEventPolygon::new, "Polygon", "poly", 3, 99),
    COLOUR(CUIEventColour::new, "Colour", "col", 4),
    GRID(CUIEventGrid::new, "Grid", "grid", 1, 2);

    private final Function<CUIEventArgs, CUIEvent> maker;
    private final String key;
    private final String name;
    private final int minParams;
    private final int maxParams;

    CUIEventType(Function<CUIEventArgs, CUIEvent> maker, String name, String key, int minParams, int maxParams) {
        this.maker = maker;
        this.name = name;
        this.key = key;
        this.minParams = minParams;
        this.maxParams = maxParams;
    }

    CUIEventType(Function<CUIEventArgs, CUIEvent> maker, String name, String key, int paramCount) {
        this(maker, name, key, paramCount, paramCount);
    }

    public CUIEvent make(final CUIEventArgs args) {
        return this.maker.apply(args);
    }

    public String getKey() {
        return this.key;
    }

    public String getName() {
        return this.name;
    }

    public int getMaxParameters() {
        return this.maxParams;
    }

    public int getMinParameters() {
        return this.minParams;
    }

    private static final Map<String, CUIEventType> BY_NAME = new HashMap<>();

    static {
        for (CUIEventType type : values()) {
            BY_NAME.put(type.getKey(), type);
        }
    }

    /**
     * Get a CUI event type by key.
     *
     * @param key protocol key
     * @return the appropriate event type, or null if none found
     */
    public static CUIEventType named(final String key) {
        return BY_NAME.get(key);
    }
}
