package dev.twme.worldeditdisplay.config;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.util.ColorUtil;

public class RenderSettings {
    
    private final WorldEditDisplay plugin;
    
    // === 玩家設定限制 ===
    private double thicknessMin;
    private double thicknessMax;
    private double markerSizeMin;
    private double markerSizeMax;
    private int segmentsMin;
    private int segmentsMax;
    private int gridDivisionMin;
    private int gridDivisionMax;
    private int gridSpacingMin;
    private int gridSpacingMax;
    private double targetSegmentLengthMin;
    private double targetSegmentLengthMax;
    private double scaleFactorMin;
    private double scaleFactorMax;
    private int fillGeneratorsMin;
    private int fillGeneratorsMax;
    
    // === Global 設定 ===
    private boolean seeThrough;
    
    // === Cuboid 設定 ===
    private Color cuboidEdgeColor;
    private Color cuboidPoint1Color;
    private Color cuboidPoint2Color;
    private Color cuboidGridColor;
    private boolean cuboidFillEnabled;
    private Color cuboidFillColor;
    private float cuboidEdgeThickness;
    private float cuboidGridThickness;
    private float cuboidVertexMarkerSize;
    private int cuboidHeightGridDivision;
    private int cuboidMaxGridSpacing;
    
    // === Cylinder 設定 ===
    private Color cylinderCircleColor;
    private Color cylinderGridColor;
    private Color cylinderCenterColor;
    private Color cylinderCenterLineColor;
    private float cylinderCircleThickness;
    private float cylinderGridThickness;
    private float cylinderCenterLineThickness;
    private float cylinderCenterThickness;
    private int cylinderMinCircleSegments;
    private int cylinderMaxCircleSegments;
    private double cylinderTargetSegmentLength;
    private double cylinderSqrtScaleFactor;
    private int cylinderHeightGridDivision;
    private int cylinderRadiusGridDivision;
    private int cylinderMaxGridSpacing;
    private boolean cylinderFillEnabled;
    private Color cylinderFillColor;
    
    // === Ellipsoid 設定 ===
    private Color ellipsoidLineColor;
    private Color ellipsoidCenterLineColor;
    private Color ellipsoidCenterColor;
    private float ellipsoidLineThickness;
    private float ellipsoidCenterLineThickness;
    private float ellipsoidCenterMarkerSize;
    private float ellipsoidCenterThickness;
    private int ellipsoidMinSegments;
    private int ellipsoidMaxSegments;
    private double ellipsoidTargetSegmentLength;
    private double ellipsoidSqrtScaleFactor;
    private int ellipsoidRadiusGridDivision;
    private int ellipsoidMaxGridSpacing;
    private boolean ellipsoidFillEnabled;
    private Color ellipsoidFillColor;
    private int ellipsoidFillGenerators;
    
    // === Polygon 設定 ===
    private Color polygonEdgeColor;
    private Color polygonVertexColor;
    private Color polygonVerticalColor;
    private boolean polygonFillEnabled;
    private Color polygonFillColor;
    private float polygonEdgeThickness;
    private float polygonVerticalThickness;
    private int polygonHeightGridDivision;
    private int polygonMaxGridSpacing;
    
    // === Polyhedron 設定 ===
    private Color polyhedronLineColor;
    private Color polyhedronVertex0Color;
    private Color polyhedronVertexColor;
    private boolean polyhedronFillEnabled;
    private Color polyhedronFillColor;
    private float polyhedronLineThickness;
    private float polyhedronVertexSize;
    private float polyhedronVertexThickness;
    
    public RenderSettings(WorldEditDisplay plugin) {
        this.plugin = plugin;
        loadDefaults();
    }
    
    private void loadDefaults() {
        thicknessMin = 0.005;
        thicknessMax = 0.7;
        markerSizeMin = 0.1;
        markerSizeMax = 2.0;
        segmentsMin = 10;
        segmentsMax = 40;
        gridDivisionMin = 5;
        gridDivisionMax = 12;
        gridSpacingMin = 1;
        gridSpacingMax = 100;
        targetSegmentLengthMin = 0.1;
        targetSegmentLengthMax = 1000.0;
        scaleFactorMin = 0.5;
        scaleFactorMax = 10.0;
        fillGeneratorsMin = 3;
        fillGeneratorsMax = 50;
        
        seeThrough = true;
        
        cuboidEdgeColor = ColorUtil.parseHexColor("#CC3333CC");
        cuboidPoint1Color = ColorUtil.parseHexColor("#33CC33CC");
        cuboidPoint2Color = ColorUtil.parseHexColor("#3333CCCC");
        cuboidGridColor = ColorUtil.parseHexColor("#CC4C4CCC");
        cuboidFillEnabled = true;
        cuboidFillColor = ColorUtil.parseHexColor("#FF000020");
        cuboidEdgeThickness = 0.03f;
        cuboidGridThickness = 0.01f;
        cuboidVertexMarkerSize = 1.0f;
        cuboidHeightGridDivision = 10;
        cuboidMaxGridSpacing = -1;
        
        cylinderCircleColor = ColorUtil.parseHexColor("#CC4C4CCC");
        cylinderGridColor = ColorUtil.parseHexColor("#CC3333CC");
        cylinderCenterColor = ColorUtil.parseHexColor("#CC33CCCC");
        cylinderCenterLineColor = ColorUtil.parseHexColor("#CC3333CC");
        cylinderCircleThickness = 0.03f;
        cylinderGridThickness = 0.03f;
        cylinderCenterLineThickness = 0.04f;
        cylinderCenterThickness = 0.05f;
        cylinderMinCircleSegments = 15;
        cylinderMaxCircleSegments = 30;
        cylinderTargetSegmentLength = 0.5;
        cylinderSqrtScaleFactor = 4.0;
        cylinderHeightGridDivision = 10;
        cylinderRadiusGridDivision = 5;
        cylinderMaxGridSpacing = -1;
        cylinderFillEnabled = true;
        cylinderFillColor = ColorUtil.parseHexColor("#CC4C4C40");
        
        ellipsoidLineColor = ColorUtil.parseHexColor("#CC4C4CCC");
        ellipsoidCenterLineColor = ColorUtil.parseHexColor("#CC3333CC");
        ellipsoidCenterColor = ColorUtil.parseHexColor("#CCCC33CC");
        ellipsoidLineThickness = 0.04f;
        ellipsoidCenterLineThickness = 0.05f;
        ellipsoidCenterMarkerSize = 1.0f;
        ellipsoidCenterThickness = 0.05f;
        ellipsoidMinSegments = 15;
        ellipsoidMaxSegments = 30;
        ellipsoidTargetSegmentLength = 0.5;
        ellipsoidSqrtScaleFactor = 4.0;
        ellipsoidRadiusGridDivision = 6;
        ellipsoidMaxGridSpacing = -1;
        ellipsoidFillEnabled = false;
        ellipsoidFillColor = ColorUtil.parseHexColor("#CC4C4C40");
        ellipsoidFillGenerators = 15;
        
        polygonEdgeColor = ColorUtil.parseHexColor("#CC4C4CCC");
        polygonVertexColor = ColorUtil.parseHexColor("#33CCCCCC");
        polygonVerticalColor = ColorUtil.parseHexColor("#CC4C4CCC");
        polygonFillEnabled = false;
        polygonFillColor = ColorUtil.parseHexColor("#CC4C4C20");
        polygonEdgeThickness = 0.04f;
        polygonVerticalThickness = 0.04f;
        polygonHeightGridDivision = 10;
        polygonMaxGridSpacing = -1;
        
        polyhedronLineColor = ColorUtil.parseHexColor("#CC3333CC");
        polyhedronVertex0Color = ColorUtil.parseHexColor("#33CC33CC");
        polyhedronVertexColor = ColorUtil.parseHexColor("#3333CCCC");
        polyhedronFillEnabled = false;
        polyhedronFillColor = ColorUtil.parseHexColor("#CC333320");
        polyhedronLineThickness = 0.03f;
        polyhedronVertexSize = 1.0f;
        polyhedronVertexThickness = 0.03f;
    }
    
    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        try {
            loadPlayerLimits(config.getConfigurationSection("player_limits"));
            loadGlobalSettings(config.getConfigurationSection("renderer.global"));
            loadCuboidSettings(config.getConfigurationSection("renderer.cuboid"));
            loadCylinderSettings(config.getConfigurationSection("renderer.cylinder"));
            loadEllipsoidSettings(config.getConfigurationSection("renderer.ellipsoid"));
            loadPolygonSettings(config.getConfigurationSection("renderer.polygon"));
            loadPolyhedronSettings(config.getConfigurationSection("renderer.polyhedron"));
        } catch (Exception e) {
            loadDefaults();
        }
    }
    
    private void loadPlayerLimits(ConfigurationSection section) {
        if (section == null) return;
        
        ConfigurationSection thickness = section.getConfigurationSection("thickness");
        if (thickness != null) {
            thicknessMin = thickness.getDouble("min", thicknessMin);
            thicknessMax = thickness.getDouble("max", thicknessMax);
        }
        
        ConfigurationSection markerSize = section.getConfigurationSection("marker_size");
        if (markerSize != null) {
            markerSizeMin = markerSize.getDouble("min", markerSizeMin);
            markerSizeMax = markerSize.getDouble("max", markerSizeMax);
        }
        
        ConfigurationSection segments = section.getConfigurationSection("segments");
        if (segments != null) {
            segmentsMin = segments.getInt("min", segmentsMin);
            segmentsMax = segments.getInt("max", segmentsMax);
        }
        
        ConfigurationSection gridDivision = section.getConfigurationSection("grid_division");
        if (gridDivision != null) {
            gridDivisionMin = gridDivision.getInt("min", gridDivisionMin);
            gridDivisionMax = gridDivision.getInt("max", gridDivisionMax);
        }
        
        ConfigurationSection gridSpacing = section.getConfigurationSection("grid_spacing");
        if (gridSpacing != null) {
            gridSpacingMin = gridSpacing.getInt("min", gridSpacingMin);
            gridSpacingMax = gridSpacing.getInt("max", gridSpacingMax);
        }
        
        ConfigurationSection targetSegmentLength = section.getConfigurationSection("target_segment_length");
        if (targetSegmentLength != null) {
            targetSegmentLengthMin = targetSegmentLength.getDouble("min", targetSegmentLengthMin);
            targetSegmentLengthMax = targetSegmentLength.getDouble("max", targetSegmentLengthMax);
        }
        
        ConfigurationSection scaleFactor = section.getConfigurationSection("scale_factor");
        if (scaleFactor != null) {
            scaleFactorMin = scaleFactor.getDouble("min", scaleFactorMin);
            scaleFactorMax = scaleFactor.getDouble("max", scaleFactorMax);
        }
        
        ConfigurationSection fillGenerators = section.getConfigurationSection("fill_generators");
        if (fillGenerators != null) {
            fillGeneratorsMin = fillGenerators.getInt("min", fillGeneratorsMin);
            fillGeneratorsMax = fillGenerators.getInt("max", fillGeneratorsMax);
        }
    }
    
    private void loadGlobalSettings(ConfigurationSection section) {
        if (section == null) return;
        seeThrough = section.getBoolean("see_through", seeThrough);
    }
    
    private void loadCuboidSettings(ConfigurationSection section) {
        if (section == null) return;
        cuboidEdgeColor = getColor(section, "edge_color", cuboidEdgeColor);
        cuboidPoint1Color = getColor(section, "point1_color", cuboidPoint1Color);
        cuboidPoint2Color = getColor(section, "point2_color", cuboidPoint2Color);
        cuboidGridColor = getColor(section, "grid_color", cuboidGridColor);
        cuboidFillEnabled = section.getBoolean("fill_enabled", cuboidFillEnabled);
        cuboidFillColor = getColor(section, "fill_color", cuboidFillColor);
        cuboidEdgeThickness = (float) section.getDouble("edge_thickness", cuboidEdgeThickness);
        cuboidGridThickness = (float) section.getDouble("grid_thickness", cuboidGridThickness);
        cuboidVertexMarkerSize = (float) section.getDouble("vertex_marker_size", cuboidVertexMarkerSize);
        cuboidHeightGridDivision = section.getInt("height_grid_division", cuboidHeightGridDivision);
        cuboidMaxGridSpacing = section.getInt("max_grid_spacing", cuboidMaxGridSpacing);
    }
    
    private void loadCylinderSettings(ConfigurationSection section) {
        if (section == null) return;
        cylinderCircleColor = getColor(section, "circle_color", cylinderCircleColor);
        cylinderGridColor = getColor(section, "grid_color", cylinderGridColor);
        cylinderCenterColor = getColor(section, "center_color", cylinderCenterColor);
        cylinderCenterLineColor = getColor(section, "center_line_color", cylinderCenterLineColor);
        cylinderCircleThickness = (float) section.getDouble("circle_thickness", cylinderCircleThickness);
        cylinderGridThickness = (float) section.getDouble("grid_thickness", cylinderGridThickness);
        cylinderCenterLineThickness = (float) section.getDouble("center_line_thickness", cylinderCenterLineThickness);
        cylinderCenterThickness = (float) section.getDouble("center_thickness", cylinderCenterThickness);
        cylinderMinCircleSegments = section.getInt("min_circle_segments", cylinderMinCircleSegments);
        cylinderMaxCircleSegments = section.getInt("max_circle_segments", cylinderMaxCircleSegments);
        cylinderTargetSegmentLength = section.getDouble("target_segment_length", cylinderTargetSegmentLength);
        cylinderSqrtScaleFactor = section.getDouble("sqrt_scale_factor", cylinderSqrtScaleFactor);
        cylinderHeightGridDivision = section.getInt("height_grid_division", cylinderHeightGridDivision);
        cylinderRadiusGridDivision = section.getInt("radius_grid_division", cylinderRadiusGridDivision);
        cylinderMaxGridSpacing = section.getInt("max_grid_spacing", cylinderMaxGridSpacing);
        cylinderFillEnabled = section.getBoolean("fill_enabled", cylinderFillEnabled);
        cylinderFillColor = getColor(section, "fill_color", cylinderFillColor);
    }
    
    private void loadEllipsoidSettings(ConfigurationSection section) {
        if (section == null) return;
        ellipsoidLineColor = getColor(section, "line_color", ellipsoidLineColor);
        ellipsoidCenterLineColor = getColor(section, "center_line_color", ellipsoidCenterLineColor);
        ellipsoidCenterColor = getColor(section, "center_color", ellipsoidCenterColor);
        ellipsoidLineThickness = (float) section.getDouble("line_thickness", ellipsoidLineThickness);
        ellipsoidCenterLineThickness = (float) section.getDouble("center_line_thickness", ellipsoidCenterLineThickness);
        ellipsoidCenterMarkerSize = (float) section.getDouble("center_marker_size", ellipsoidCenterMarkerSize);
        ellipsoidCenterThickness = (float) section.getDouble("center_thickness", ellipsoidCenterThickness);
        ellipsoidMinSegments = section.getInt("min_segments", ellipsoidMinSegments);
        ellipsoidMaxSegments = section.getInt("max_segments", ellipsoidMaxSegments);
        ellipsoidTargetSegmentLength = section.getDouble("target_segment_length", ellipsoidTargetSegmentLength);
        ellipsoidSqrtScaleFactor = section.getDouble("sqrt_scale_factor", ellipsoidSqrtScaleFactor);
        ellipsoidRadiusGridDivision = section.getInt("radius_grid_division", ellipsoidRadiusGridDivision);
        ellipsoidMaxGridSpacing = section.getInt("max_grid_spacing", ellipsoidMaxGridSpacing);
        ellipsoidFillEnabled = section.getBoolean("fill_enabled", ellipsoidFillEnabled);
        ellipsoidFillColor = getColor(section, "fill_color", ellipsoidFillColor);
        ellipsoidFillGenerators = section.getInt("fill_generators", ellipsoidFillGenerators);
    }
    
    private void loadPolygonSettings(ConfigurationSection section) {
        if (section == null) return;
        polygonEdgeColor = getColor(section, "edge_color", polygonEdgeColor);
        polygonVertexColor = getColor(section, "vertex_color", polygonVertexColor);
        polygonVerticalColor = getColor(section, "vertical_color", polygonVerticalColor);
        polygonFillEnabled = section.getBoolean("fill_enabled", polygonFillEnabled);
        polygonFillColor = getColor(section, "fill_color", polygonFillColor);
        polygonEdgeThickness = (float) section.getDouble("edge_thickness", polygonEdgeThickness);
        polygonVerticalThickness = (float) section.getDouble("vertical_thickness", polygonVerticalThickness);
        polygonHeightGridDivision = section.getInt("height_grid_division", polygonHeightGridDivision);
        polygonMaxGridSpacing = section.getInt("max_grid_spacing", polygonMaxGridSpacing);
    }
    
    private void loadPolyhedronSettings(ConfigurationSection section) {
        if (section == null) return;
        polyhedronLineColor = getColor(section, "line_color", polyhedronLineColor);
        polyhedronVertex0Color = getColor(section, "vertex0_color", polyhedronVertex0Color);
        polyhedronVertexColor = getColor(section, "vertex_color", polyhedronVertexColor);
        polyhedronFillEnabled = section.getBoolean("fill_enabled", polyhedronFillEnabled);
        polyhedronFillColor = getColor(section, "fill_color", polyhedronFillColor);
        polyhedronLineThickness = (float) section.getDouble("line_thickness", polyhedronLineThickness);
        polyhedronVertexSize = (float) section.getDouble("vertex_size", polyhedronVertexSize);
        polyhedronVertexThickness = (float) section.getDouble("vertex_thickness", polyhedronVertexThickness);
    }
    
    private Color getColor(ConfigurationSection section, String key, Color defaultValue) {
        String colorStr = section.getString(key);
        if (colorStr == null || colorStr.isEmpty()) return defaultValue;
        Color parsed = ColorUtil.parseHexColor(colorStr);
        return parsed != null ? parsed : defaultValue;
    }
    
    // === Global Getters ===
    public boolean isSeeThrough() { return seeThrough; }
    
    // === Cuboid Getters ===
    public Color getCuboidEdgeColor() { return cuboidEdgeColor; }
    public Color getCuboidPoint1Color() { return cuboidPoint1Color; }
    public Color getCuboidPoint2Color() { return cuboidPoint2Color; }
    public Color getCuboidGridColor() { return cuboidGridColor; }
    public boolean isCuboidFillEnabled() { return cuboidFillEnabled; }
    public Color getCuboidFillColor() { return cuboidFillColor; }
    public float getCuboidEdgeThickness() { return cuboidEdgeThickness; }
    public float getCuboidGridThickness() { return cuboidGridThickness; }
    public float getCuboidVertexMarkerSize() { return cuboidVertexMarkerSize; }
    public int getCuboidHeightGridDivision() { return cuboidHeightGridDivision; }
    public int getCuboidMaxGridSpacing() { return cuboidMaxGridSpacing; }
    
    // === Cylinder Getters ===
    public Color getCylinderCircleColor() { return cylinderCircleColor; }
    public Color getCylinderGridColor() { return cylinderGridColor; }
    public Color getCylinderCenterColor() { return cylinderCenterColor; }
    public Color getCylinderCenterLineColor() { return cylinderCenterLineColor; }
    public float getCylinderCircleThickness() { return cylinderCircleThickness; }
    public float getCylinderGridThickness() { return cylinderGridThickness; }
    public float getCylinderCenterLineThickness() { return cylinderCenterLineThickness; }
    public float getCylinderCenterThickness() { return cylinderCenterThickness; }
    public int getCylinderMinCircleSegments() { return cylinderMinCircleSegments; }
    public int getCylinderMaxCircleSegments() { return cylinderMaxCircleSegments; }
    public double getCylinderTargetSegmentLength() { return cylinderTargetSegmentLength; }
    public double getCylinderSqrtScaleFactor() { return cylinderSqrtScaleFactor; }
    public int getCylinderHeightGridDivision() { return cylinderHeightGridDivision; }
    public int getCylinderRadiusGridDivision() { return cylinderRadiusGridDivision; }
    public int getCylinderMaxGridSpacing() { return cylinderMaxGridSpacing; }
    public boolean isCylinderFillEnabled() { return cylinderFillEnabled; }
    public Color getCylinderFillColor() { return cylinderFillColor; }
    
    // === Ellipsoid Getters ===
    public Color getEllipsoidLineColor() { return ellipsoidLineColor; }
    public Color getEllipsoidCenterLineColor() { return ellipsoidCenterLineColor; }
    public Color getEllipsoidCenterColor() { return ellipsoidCenterColor; }
    public float getEllipsoidLineThickness() { return ellipsoidLineThickness; }
    public float getEllipsoidCenterLineThickness() { return ellipsoidCenterLineThickness; }
    public float getEllipsoidCenterMarkerSize() { return ellipsoidCenterMarkerSize; }
    public float getEllipsoidCenterThickness() { return ellipsoidCenterThickness; }
    public int getEllipsoidMinSegments() { return ellipsoidMinSegments; }
    public int getEllipsoidMaxSegments() { return ellipsoidMaxSegments; }
    public double getEllipsoidTargetSegmentLength() { return ellipsoidTargetSegmentLength; }
    public double getEllipsoidSqrtScaleFactor() { return ellipsoidSqrtScaleFactor; }
    public int getEllipsoidRadiusGridDivision() { return ellipsoidRadiusGridDivision; }
    public int getEllipsoidMaxGridSpacing() { return ellipsoidMaxGridSpacing; }
    public boolean isEllipsoidFillEnabled() { return ellipsoidFillEnabled; }
    public Color getEllipsoidFillColor() { return ellipsoidFillColor; }
    public int getEllipsoidFillGenerators() { return ellipsoidFillGenerators; }
    
    // === Polygon Getters ===
    public Color getPolygonEdgeColor() { return polygonEdgeColor; }
    public Color getPolygonVertexColor() { return polygonVertexColor; }
    public Color getPolygonVerticalColor() { return polygonVerticalColor; }
    public boolean isPolygonFillEnabled() { return polygonFillEnabled; }
    public Color getPolygonFillColor() { return polygonFillColor; }
    public float getPolygonEdgeThickness() { return polygonEdgeThickness; }
    public float getPolygonVerticalThickness() { return polygonVerticalThickness; }
    public int getPolygonHeightGridDivision() { return polygonHeightGridDivision; }
    public int getPolygonMaxGridSpacing() { return polygonMaxGridSpacing; }
    
    // === Polyhedron Getters ===
    public Color getPolyhedronLineColor() { return polyhedronLineColor; }
    public Color getPolyhedronVertex0Color() { return polyhedronVertex0Color; }
    public Color getPolyhedronVertexColor() { return polyhedronVertexColor; }
    public boolean isPolyhedronFillEnabled() { return polyhedronFillEnabled; }
    public Color getPolyhedronFillColor() { return polyhedronFillColor; }
    public float getPolyhedronLineThickness() { return polyhedronLineThickness; }
    public float getPolyhedronVertexSize() { return polyhedronVertexSize; }
    public float getPolyhedronVertexThickness() { return polyhedronVertexThickness; }
    
    // === 玩家設定限制 Getters ===
    public double getThicknessMin() { return thicknessMin; }
    public double getThicknessMax() { return thicknessMax; }
    public double getMarkerSizeMin() { return markerSizeMin; }
    public double getMarkerSizeMax() { return markerSizeMax; }
    public int getSegmentsMin() { return segmentsMin; }
    public int getSegmentsMax() { return segmentsMax; }
    public int getGridDivisionMin() { return gridDivisionMin; }
    public int getGridDivisionMax() { return gridDivisionMax; }
    public int getGridSpacingMin() { return gridSpacingMin; }
    public int getGridSpacingMax() { return gridSpacingMax; }
    public double getTargetSegmentLengthMin() { return targetSegmentLengthMin; }
    public double getTargetSegmentLengthMax() { return targetSegmentLengthMax; }
    public double getScaleFactorMin() { return scaleFactorMin; }
    public double getScaleFactorMax() { return scaleFactorMax; }
    public int getFillGeneratorsMin() { return fillGeneratorsMin; }
    public int getFillGeneratorsMax() { return fillGeneratorsMax; }
}
