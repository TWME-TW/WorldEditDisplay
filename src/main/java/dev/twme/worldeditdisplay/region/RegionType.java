package dev.twme.worldeditdisplay.region;

import dev.twme.worldeditdisplay.player.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Enum of different region types
 */
public enum RegionType {
    CUBOID("cuboid", "Cuboid", CuboidRegion::new),
    POLYGON("polygon2d", "2D Polygon", PolygonRegion::new),
    ELLIPSOID("ellipsoid", "Ellipsoid", EllipsoidRegion::new),
    CYLINDER("cylinder", "Cylinder", CylinderRegion::new),
    POLYHEDRON("polyhedron", "Polyhedron", PolyhedronRegion::new);

    private static final Map<String, RegionType> BY_KEY = new HashMap<>();

    private final String key;
    private final String name;
    private final Function<PlayerData, Region> maker;

    RegionType(String key, String name, Function<PlayerData, Region> maker) {
        this.key = key;
        this.name = name;
        this.maker = maker;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public Region createRegion(PlayerData playerData) {
        return maker.apply(playerData);
    }

    static {
        for (RegionType type : values()) {
            BY_KEY.put(type.getKey(), type);
        }
    }

    public static RegionType fromKey(String key) {
        return BY_KEY.get(key);
    }
}
