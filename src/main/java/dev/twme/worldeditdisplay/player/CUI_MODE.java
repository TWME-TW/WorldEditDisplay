package dev.twme.worldeditdisplay.player;

/*
    modeName 代表的是 透過 cui channel 發送的訊息中，所使用的 mode 名稱
    例如: CUBOID 代表的 modeName 是 "cuboid"， poly
 */
public enum CUI_MODE {

    // cuboid, extend
    CUBOID("cuboid"),

    // convex, hull, polyhedron, polyhedral
    POLYHEDRON("polyhedron"),

    // ellipsoid, sphere
    ELLIPSOID("ellipsoid"),

    // poly
    POLYGON2D("polygon2d"),

    // cyl
    CYLINDER("cylinder");

    // define enum
    public final String modeName;
    CUI_MODE(String modeName) {
        this.modeName = modeName;
    }

}
