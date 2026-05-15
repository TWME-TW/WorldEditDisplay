package dev.twme.worldeditdisplay.config;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.util.ColorUtil;

public class PlayerRenderSettings {

    private final WorldEditDisplay plugin;
    private final UUID playerUUID;
    private final RenderSettings serverSettings;
    private final File configFile;
    private FileConfiguration config;
    private volatile boolean dirty = false;

    // Cuboid
    private Boolean cuboidSeeThrough;
    private Color cuboidEdgeColor;
    private Color cuboidPoint1Color;
    private Color cuboidPoint2Color;
    private Color cuboidGridColor;
    private Boolean cuboidFillEnabled;
    private Color cuboidFillColor;
    private Float cuboidEdgeThickness;
    private Float cuboidGridThickness;
    private Float cuboidVertexMarkerSize;
    private Integer cuboidHeightGridDivision;

    // Cylinder
    private Boolean cylinderSeeThrough;
    private Color cylinderCircleColor;
    private Color cylinderGridColor;
    private Color cylinderCenterColor;
    private Color cylinderCenterLineColor;
    private Float cylinderCircleThickness;
    private Float cylinderGridThickness;
    private Float cylinderCenterLineThickness;
    private Float cylinderCenterThickness;
    private Integer cylinderMinCircleSegments;
    private Integer cylinderMaxCircleSegments;
    private Double cylinderTargetSegmentLength;
    private Integer cylinderHeightGridDivision;
    private Integer cylinderRadiusGridDivision;
    private Boolean cylinderFillEnabled;
    private Color cylinderFillColor;

    // Ellipsoid
    private Boolean ellipsoidSeeThrough;
    private Color ellipsoidLineColor;
    private Color ellipsoidCenterLineColor;
    private Color ellipsoidCenterColor;
    private Float ellipsoidLineThickness;
    private Float ellipsoidCenterLineThickness;
    private Float ellipsoidCenterMarkerSize;
    private Float ellipsoidCenterThickness;
    private Integer ellipsoidMinSegments;
    private Integer ellipsoidMaxSegments;
    private Double ellipsoidTargetSegmentLength;
    private Integer ellipsoidRadiusGridDivision;
    private Boolean ellipsoidFillEnabled;
    private Color ellipsoidFillColor;
    private Integer ellipsoidFillGenerators;

    // Polygon
    private Boolean polygonSeeThrough;
    private Color polygonEdgeColor;
    private Color polygonVertexColor;
    private Color polygonVerticalColor;
    private Boolean polygonFillEnabled;
    private Color polygonFillColor;
    private Float polygonEdgeThickness;
    private Float polygonVerticalThickness;
    private Integer polygonHeightGridDivision;

    // Polyhedron
    private Boolean polyhedronSeeThrough;
    private Color polyhedronLineColor;
    private Color polyhedronVertex0Color;
    private Color polyhedronVertexColor;
    private Boolean polyhedronFillEnabled;
    private Color polyhedronFillColor;
    private Float polyhedronLineThickness;
    private Float polyhedronVertexSize;
    private Float polyhedronVertexThickness;

    public PlayerRenderSettings(WorldEditDisplay plugin, UUID playerUUID) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
        this.serverSettings = plugin.getRenderSettings();

        File playerConfigDir = new File(plugin.getDataFolder(), "player_config");
        if (!playerConfigDir.exists()) playerConfigDir.mkdirs();

        this.configFile = new File(playerConfigDir, playerUUID + ".yml");
        load();
    }

    /**
     * Protected constructor used by subclasses that manage their own colour overrides.
     * Does NOT load any YAML file – all fields remain null and getters fall back to server settings.
     */
    protected PlayerRenderSettings(WorldEditDisplay plugin) {
        this.plugin = plugin;
        this.playerUUID = null;
        this.serverSettings = plugin.getRenderSettings();
        this.configFile = null;
        this.config = new org.bukkit.configuration.file.YamlConfiguration();
    }

    public void load() {
        clearFields();
        if (!configFile.exists()) {
            config = new YamlConfiguration();
            return;
        }

        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            loadCuboidSettings(config.getConfigurationSection("renderer.cuboid"));
            loadCylinderSettings(config.getConfigurationSection("renderer.cylinder"));
            loadEllipsoidSettings(config.getConfigurationSection("renderer.ellipsoid"));
            loadPolygonSettings(config.getConfigurationSection("renderer.polygon"));
            loadPolyhedronSettings(config.getConfigurationSection("renderer.polyhedron"));
        } catch (Exception e) {
            config = new YamlConfiguration();
        }
    }

    private void clearFields() {
        cuboidSeeThrough = null;
        cuboidEdgeColor = null;
        cuboidPoint1Color = null;
        cuboidPoint2Color = null;
        cuboidGridColor = null;
        cuboidFillEnabled = null;
        cuboidFillColor = null;
        cuboidEdgeThickness = null;
        cuboidGridThickness = null;
        cuboidVertexMarkerSize = null;
        cuboidHeightGridDivision = null;

        cylinderSeeThrough = null;
        cylinderCircleColor = null;
        cylinderGridColor = null;
        cylinderCenterColor = null;
        cylinderCenterLineColor = null;
        cylinderCircleThickness = null;
        cylinderGridThickness = null;
        cylinderCenterLineThickness = null;
        cylinderCenterThickness = null;
        cylinderMinCircleSegments = null;
        cylinderMaxCircleSegments = null;
        cylinderTargetSegmentLength = null;
        cylinderHeightGridDivision = null;
        cylinderRadiusGridDivision = null;
        cylinderFillEnabled = null;
        cylinderFillColor = null;

        ellipsoidSeeThrough = null;
        ellipsoidLineColor = null;
        ellipsoidCenterLineColor = null;
        ellipsoidCenterColor = null;
        ellipsoidLineThickness = null;
        ellipsoidCenterLineThickness = null;
        ellipsoidCenterMarkerSize = null;
        ellipsoidCenterThickness = null;
        ellipsoidMinSegments = null;
        ellipsoidMaxSegments = null;
        ellipsoidTargetSegmentLength = null;
        ellipsoidRadiusGridDivision = null;
        ellipsoidFillEnabled = null;
        ellipsoidFillColor = null;
        ellipsoidFillGenerators = null;

        polygonSeeThrough = null;
        polygonEdgeColor = null;
        polygonVertexColor = null;
        polygonVerticalColor = null;
        polygonFillEnabled = null;
        polygonFillColor = null;
        polygonEdgeThickness = null;
        polygonVerticalThickness = null;
        polygonHeightGridDivision = null;

        polyhedronSeeThrough = null;
        polyhedronLineColor = null;
        polyhedronVertex0Color = null;
        polyhedronVertexColor = null;
        polyhedronFillEnabled = null;
        polyhedronFillColor = null;
        polyhedronLineThickness = null;
        polyhedronVertexSize = null;
        polyhedronVertexThickness = null;
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException ignored) {}
    }

    private void loadCuboidSettings(ConfigurationSection section) {
        if (section == null) return;
        cuboidSeeThrough = getBoolean(section, "see_through");
        cuboidEdgeColor = getColor(section, "edge_color");
        cuboidPoint1Color = getColor(section, "point1_color");
        cuboidPoint2Color = getColor(section, "point2_color");
        cuboidGridColor = getColor(section, "grid_color");
        cuboidFillEnabled = getBoolean(section, "fill_enabled");
        cuboidFillColor = getColor(section, "fill_color");
        cuboidEdgeThickness = getFloat(section, "edge_thickness");
        cuboidGridThickness = getFloat(section, "grid_thickness");
        cuboidVertexMarkerSize = getFloat(section, "vertex_marker_size");
        cuboidHeightGridDivision = getInt(section, "height_grid_division");
    }

    private void loadCylinderSettings(ConfigurationSection section) {
        if (section == null) return;
        cylinderSeeThrough = getBoolean(section, "see_through");
        cylinderCircleColor = getColor(section, "circle_color");
        cylinderGridColor = getColor(section, "grid_color");
        cylinderCenterColor = getColor(section, "center_color");
        cylinderCenterLineColor = getColor(section, "center_line_color");
        cylinderCircleThickness = getFloat(section, "circle_thickness");
        cylinderGridThickness = getFloat(section, "grid_thickness");
        cylinderCenterLineThickness = getFloat(section, "center_line_thickness");
        cylinderCenterThickness = getFloat(section, "center_thickness");
        cylinderMinCircleSegments = getInt(section, "min_circle_segments");
        cylinderMaxCircleSegments = getInt(section, "max_circle_segments");
        cylinderTargetSegmentLength = getDouble(section, "target_segment_length");
        cylinderHeightGridDivision = getInt(section, "height_grid_division");
        cylinderRadiusGridDivision = getInt(section, "radius_grid_division");
        cylinderFillEnabled = getBoolean(section, "fill_enabled");
        cylinderFillColor = getColor(section, "fill_color");
    }

    private void loadEllipsoidSettings(ConfigurationSection section) {
        if (section == null) return;
        ellipsoidSeeThrough = getBoolean(section, "see_through");
        ellipsoidLineColor = getColor(section, "line_color");
        ellipsoidCenterLineColor = getColor(section, "center_line_color");
        ellipsoidCenterColor = getColor(section, "center_color");
        ellipsoidLineThickness = getFloat(section, "line_thickness");
        ellipsoidCenterLineThickness = getFloat(section, "center_line_thickness");
        ellipsoidCenterMarkerSize = getFloat(section, "center_marker_size");
        ellipsoidCenterThickness = getFloat(section, "center_thickness");
        ellipsoidMinSegments = getInt(section, "min_segments");
        ellipsoidMaxSegments = getInt(section, "max_segments");
        ellipsoidTargetSegmentLength = getDouble(section, "target_segment_length");
        ellipsoidRadiusGridDivision = getInt(section, "radius_grid_division");
        ellipsoidFillEnabled = getBoolean(section, "fill_enabled");
        ellipsoidFillColor = getColor(section, "fill_color");
        ellipsoidFillGenerators = getInt(section, "fill_generators");
    }

    private void loadPolygonSettings(ConfigurationSection section) {
        if (section == null) return;
        polygonSeeThrough = getBoolean(section, "see_through");
        polygonEdgeColor = getColor(section, "edge_color");
        polygonVertexColor = getColor(section, "vertex_color");
        polygonVerticalColor = getColor(section, "vertical_color");
        polygonFillEnabled = getBoolean(section, "fill_enabled");
        polygonFillColor = getColor(section, "fill_color");
        polygonEdgeThickness = getFloat(section, "edge_thickness");
        polygonVerticalThickness = getFloat(section, "vertical_thickness");
        polygonHeightGridDivision = getInt(section, "height_grid_division");
    }

    private void loadPolyhedronSettings(ConfigurationSection section) {
        if (section == null) return;
        polyhedronSeeThrough = getBoolean(section, "see_through");
        polyhedronLineColor = getColor(section, "line_color");
        polyhedronVertex0Color = getColor(section, "vertex0_color");
        polyhedronVertexColor = getColor(section, "vertex_color");
        polyhedronFillEnabled = getBoolean(section, "fill_enabled");
        polyhedronFillColor = getColor(section, "fill_color");
        polyhedronLineThickness = getFloat(section, "line_thickness");
        polyhedronVertexSize = getFloat(section, "vertex_size");
        polyhedronVertexThickness = getFloat(section, "vertex_thickness");
    }

    private Color getColor(ConfigurationSection section, String key) {
        String value = section.getString(key);
        if (value == null) return null;
        return ColorUtil.parseHexColor(value);
    }

    private Float getFloat(ConfigurationSection section, String key) {
        if (!section.contains(key)) return null;
        return (float) section.getDouble(key);
    }

    private Integer getInt(ConfigurationSection section, String key) {
        if (!section.contains(key)) return null;
        return section.getInt(key);
    }

    private Double getDouble(ConfigurationSection section, String key) {
        if (!section.contains(key)) return null;
        return section.getDouble(key);
    }

    private Boolean getBoolean(ConfigurationSection section, String key) {
        if (!section.contains(key)) return null;
        return section.getBoolean(key);
    }

    public boolean set(String path, Object value) {
        if (value instanceof Color) value = ColorUtil.toHexString((Color) value);
        if (value instanceof String strVal) {
            if (strVal.startsWith("#")) {
                if (!ColorUtil.isValidHexColor(strVal)) return false;
            } else if (strVal.equalsIgnoreCase("true") || strVal.equalsIgnoreCase("false")) {
                value = Boolean.parseBoolean(strVal);
            } else {
                return false;
            }
        }
        if (path.endsWith("see_through") && !serverSettings.isSeeThroughAllowed()) return false;
        if (value instanceof Number && !validateNumericValue(path, ((Number) value).doubleValue())) return false;

        config.set(path, value instanceof Color ? ColorUtil.toHexString((Color) value) : value);
        dirty = true;
        save();
        load();
        return true;
    }

    private boolean validateNumericValue(String path, double value) {
        String key = path.substring(path.lastIndexOf('.') + 1);
        if (key.contains("thickness"))
            return value >= serverSettings.getThicknessMin() && value <= serverSettings.getThicknessMax();
        if (key.contains("size") || key.contains("marker"))
            return value >= serverSettings.getMarkerSizeMin() && value <= serverSettings.getMarkerSizeMax();
        if (key.contains("segments"))
            return value >= serverSettings.getSegmentsMin() && value <= serverSettings.getSegmentsMax();
        if (key.contains("division"))
            return value >= serverSettings.getGridDivisionMin() && value <= serverSettings.getGridDivisionMax();
        if (key.contains("spacing"))
            return value >= serverSettings.getGridSpacingMin() && value <= serverSettings.getGridSpacingMax();
        if (key.equals("target_segment_length"))
            return value >= serverSettings.getTargetSegmentLengthMin() && value <= serverSettings.getTargetSegmentLengthMax();
        if (key.contains("scale_factor"))
            return value >= serverSettings.getScaleFactorMin() && value <= serverSettings.getScaleFactorMax();
        if (key.equals("fill_generators"))
            return value >= serverSettings.getFillGeneratorsMin() && value <= serverSettings.getFillGeneratorsMax();
        return true;
    }

    public void reset(String path) { config.set(path, null); dirty = true; save(); load(); }

    public void resetAll() {
        if (configFile != null && configFile.exists()) configFile.delete();
        config = new YamlConfiguration();
        dirty = false;
        load();
    }

    public boolean isDirty() { return dirty; }
    public void markClean() { dirty = false; }

    // === Cuboid Getters ===
    public boolean isCuboidSeeThrough() { return cuboidSeeThrough != null ? cuboidSeeThrough : serverSettings.isCuboidSeeThrough(); }
    public Color getCuboidEdgeColor() { return cuboidEdgeColor != null ? cuboidEdgeColor : serverSettings.getCuboidEdgeColor(); }
    public Color getCuboidPoint1Color() { return cuboidPoint1Color != null ? cuboidPoint1Color : serverSettings.getCuboidPoint1Color(); }
    public Color getCuboidPoint2Color() { return cuboidPoint2Color != null ? cuboidPoint2Color : serverSettings.getCuboidPoint2Color(); }
    public Color getCuboidGridColor() { return cuboidGridColor != null ? cuboidGridColor : serverSettings.getCuboidGridColor(); }
    public boolean isCuboidFillEnabled() { return cuboidFillEnabled != null ? cuboidFillEnabled : serverSettings.isCuboidFillEnabled(); }
    public Color getCuboidFillColor() { return cuboidFillColor != null ? cuboidFillColor : serverSettings.getCuboidFillColor(); }
    public float getCuboidEdgeThickness() { return cuboidEdgeThickness != null ? cuboidEdgeThickness : serverSettings.getCuboidEdgeThickness(); }
    public float getCuboidGridThickness() { return cuboidGridThickness != null ? cuboidGridThickness : serverSettings.getCuboidGridThickness(); }
    public float getCuboidVertexMarkerSize() { return cuboidVertexMarkerSize != null ? cuboidVertexMarkerSize : serverSettings.getCuboidVertexMarkerSize(); }
    public int getCuboidHeightGridDivision() { return cuboidHeightGridDivision != null ? cuboidHeightGridDivision : serverSettings.getCuboidHeightGridDivision(); }
    public int getCuboidMaxGridSpacing() { return serverSettings.getCuboidMaxGridSpacing(); }

    // === Cylinder Getters ===
    public boolean isCylinderSeeThrough() { return cylinderSeeThrough != null ? cylinderSeeThrough : serverSettings.isCylinderSeeThrough(); }
    public Color getCylinderCircleColor() { return cylinderCircleColor != null ? cylinderCircleColor : serverSettings.getCylinderCircleColor(); }
    public Color getCylinderGridColor() { return cylinderGridColor != null ? cylinderGridColor : serverSettings.getCylinderGridColor(); }
    public Color getCylinderCenterColor() { return cylinderCenterColor != null ? cylinderCenterColor : serverSettings.getCylinderCenterColor(); }
    public Color getCylinderCenterLineColor() { return cylinderCenterLineColor != null ? cylinderCenterLineColor : serverSettings.getCylinderCenterLineColor(); }
    public float getCylinderCircleThickness() { return cylinderCircleThickness != null ? cylinderCircleThickness : serverSettings.getCylinderCircleThickness(); }
    public float getCylinderGridThickness() { return cylinderGridThickness != null ? cylinderGridThickness : serverSettings.getCylinderGridThickness(); }
    public float getCylinderCenterLineThickness() { return cylinderCenterLineThickness != null ? cylinderCenterLineThickness : serverSettings.getCylinderCenterLineThickness(); }
    public float getCylinderCenterThickness() { return cylinderCenterThickness != null ? cylinderCenterThickness : serverSettings.getCylinderCenterThickness(); }
    public int getCylinderMinCircleSegments() { return cylinderMinCircleSegments != null ? cylinderMinCircleSegments : serverSettings.getCylinderMinCircleSegments(); }
    public int getCylinderMaxCircleSegments() { return cylinderMaxCircleSegments != null ? cylinderMaxCircleSegments : serverSettings.getCylinderMaxCircleSegments(); }
    public double getCylinderTargetSegmentLength() { return cylinderTargetSegmentLength != null ? cylinderTargetSegmentLength : serverSettings.getCylinderTargetSegmentLength(); }
    public double getCylinderSqrtScaleFactor() { return serverSettings.getCylinderSqrtScaleFactor(); }
    public int getCylinderHeightGridDivision() { return cylinderHeightGridDivision != null ? cylinderHeightGridDivision : serverSettings.getCylinderHeightGridDivision(); }
    public int getCylinderRadiusGridDivision() { return cylinderRadiusGridDivision != null ? cylinderRadiusGridDivision : serverSettings.getCylinderRadiusGridDivision(); }
    public int getCylinderMaxGridSpacing() { return serverSettings.getCylinderMaxGridSpacing(); }
    public boolean isCylinderFillEnabled() { return cylinderFillEnabled != null ? cylinderFillEnabled : serverSettings.isCylinderFillEnabled(); }
    public Color getCylinderFillColor() { return cylinderFillColor != null ? cylinderFillColor : serverSettings.getCylinderFillColor(); }

    // === Ellipsoid Getters ===
    public boolean isEllipsoidSeeThrough() { return ellipsoidSeeThrough != null ? ellipsoidSeeThrough : serverSettings.isEllipsoidSeeThrough(); }
    public Color getEllipsoidLineColor() { return ellipsoidLineColor != null ? ellipsoidLineColor : serverSettings.getEllipsoidLineColor(); }
    public Color getEllipsoidCenterLineColor() { return ellipsoidCenterLineColor != null ? ellipsoidCenterLineColor : serverSettings.getEllipsoidCenterLineColor(); }
    public Color getEllipsoidCenterColor() { return ellipsoidCenterColor != null ? ellipsoidCenterColor : serverSettings.getEllipsoidCenterColor(); }
    public float getEllipsoidLineThickness() { return ellipsoidLineThickness != null ? ellipsoidLineThickness : serverSettings.getEllipsoidLineThickness(); }
    public float getEllipsoidCenterLineThickness() { return ellipsoidCenterLineThickness != null ? ellipsoidCenterLineThickness : serverSettings.getEllipsoidCenterLineThickness(); }
    public float getEllipsoidCenterMarkerSize() { return ellipsoidCenterMarkerSize != null ? ellipsoidCenterMarkerSize : serverSettings.getEllipsoidCenterMarkerSize(); }
    public float getEllipsoidCenterThickness() { return ellipsoidCenterThickness != null ? ellipsoidCenterThickness : serverSettings.getEllipsoidCenterThickness(); }
    public int getEllipsoidMinSegments() { return ellipsoidMinSegments != null ? ellipsoidMinSegments : serverSettings.getEllipsoidMinSegments(); }
    public int getEllipsoidMaxSegments() { return ellipsoidMaxSegments != null ? ellipsoidMaxSegments : serverSettings.getEllipsoidMaxSegments(); }
    public double getEllipsoidTargetSegmentLength() { return ellipsoidTargetSegmentLength != null ? ellipsoidTargetSegmentLength : serverSettings.getEllipsoidTargetSegmentLength(); }
    public double getEllipsoidSqrtScaleFactor() { return serverSettings.getEllipsoidSqrtScaleFactor(); }
    public int getEllipsoidRadiusGridDivision() { return ellipsoidRadiusGridDivision != null ? ellipsoidRadiusGridDivision : serverSettings.getEllipsoidRadiusGridDivision(); }
    public int getEllipsoidMaxGridSpacing() { return serverSettings.getEllipsoidMaxGridSpacing(); }
    public boolean isEllipsoidFillEnabled() { return ellipsoidFillEnabled != null ? ellipsoidFillEnabled : serverSettings.isEllipsoidFillEnabled(); }
    public Color getEllipsoidFillColor() { return ellipsoidFillColor != null ? ellipsoidFillColor : serverSettings.getEllipsoidFillColor(); }
    public int getEllipsoidFillGenerators() { return ellipsoidFillGenerators != null ? ellipsoidFillGenerators : serverSettings.getEllipsoidFillGenerators(); }

    // === Polygon Getters ===
    public boolean isPolygonSeeThrough() { return polygonSeeThrough != null ? polygonSeeThrough : serverSettings.isPolygonSeeThrough(); }
    public Color getPolygonEdgeColor() { return polygonEdgeColor != null ? polygonEdgeColor : serverSettings.getPolygonEdgeColor(); }
    public Color getPolygonVertexColor() { return polygonVertexColor != null ? polygonVertexColor : serverSettings.getPolygonVertexColor(); }
    public Color getPolygonVerticalColor() { return polygonVerticalColor != null ? polygonVerticalColor : serverSettings.getPolygonVerticalColor(); }
    public boolean isPolygonFillEnabled() { return polygonFillEnabled != null ? polygonFillEnabled : serverSettings.isPolygonFillEnabled(); }
    public Color getPolygonFillColor() { return polygonFillColor != null ? polygonFillColor : serverSettings.getPolygonFillColor(); }
    public float getPolygonEdgeThickness() { return polygonEdgeThickness != null ? polygonEdgeThickness : serverSettings.getPolygonEdgeThickness(); }
    public float getPolygonVerticalThickness() { return polygonVerticalThickness != null ? polygonVerticalThickness : serverSettings.getPolygonVerticalThickness(); }
    public int getPolygonHeightGridDivision() { return polygonHeightGridDivision != null ? polygonHeightGridDivision : serverSettings.getPolygonHeightGridDivision(); }
    public int getPolygonMaxGridSpacing() { return serverSettings.getPolygonMaxGridSpacing(); }

    // === Polyhedron Getters ===
    public boolean isPolyhedronSeeThrough() { return polyhedronSeeThrough != null ? polyhedronSeeThrough : serverSettings.isPolyhedronSeeThrough(); }
    public Color getPolyhedronLineColor() { return polyhedronLineColor != null ? polyhedronLineColor : serverSettings.getPolyhedronLineColor(); }
    public Color getPolyhedronVertex0Color() { return polyhedronVertex0Color != null ? polyhedronVertex0Color : serverSettings.getPolyhedronVertex0Color(); }
    public Color getPolyhedronVertexColor() { return polyhedronVertexColor != null ? polyhedronVertexColor : serverSettings.getPolyhedronVertexColor(); }
    public boolean isPolyhedronFillEnabled() { return polyhedronFillEnabled != null ? polyhedronFillEnabled : serverSettings.isPolyhedronFillEnabled(); }
    public Color getPolyhedronFillColor() { return polyhedronFillColor != null ? polyhedronFillColor : serverSettings.getPolyhedronFillColor(); }
    public float getPolyhedronLineThickness() { return polyhedronLineThickness != null ? polyhedronLineThickness : serverSettings.getPolyhedronLineThickness(); }
    public float getPolyhedronVertexSize() { return polyhedronVertexSize != null ? polyhedronVertexSize : serverSettings.getPolyhedronVertexSize(); }
    public float getPolyhedronVertexThickness() { return polyhedronVertexThickness != null ? polyhedronVertexThickness : serverSettings.getPolyhedronVertexThickness(); }

    public UUID getPlayerUUID() { return playerUUID; }
}
