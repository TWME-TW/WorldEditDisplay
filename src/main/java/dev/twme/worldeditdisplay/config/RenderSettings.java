package dev.twme.worldeditdisplay.config;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 渲染設定管理類
 * 
 * 負責從 config.yml 讀取和管理所有渲染器的配置參數
 * 支援熱重載 (reload)
 */
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
    
    // === Cuboid 設定 ===
    private Material cuboidEdgeMaterial;
    private Material cuboidPoint1Material;
    private Material cuboidPoint2Material;
    private Material cuboidGridMaterial;
    private float cuboidEdgeThickness;
    private float cuboidGridThickness;
    private float cuboidVertexMarkerSize;
    private int cuboidHeightGridDivision;
    private int cuboidMaxGridSpacing;
    
    // === Cylinder 設定 ===
    private Material cylinderCircleMaterial;
    private Material cylinderGridMaterial;
    private Material cylinderCenterMaterial;
    private Material cylinderCenterLineMaterial;
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
    
    // === Ellipsoid 設定 ===
    private Material ellipsoidLineMaterial;
    private Material ellipsoidCenterLineMaterial;
    private Material ellipsoidCenterMaterial;
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
    
    // === Polygon 設定 ===
    private Material polygonEdgeMaterial;
    private Material polygonVertexMaterial;
    private Material polygonVerticalMaterial;
    private float polygonEdgeThickness;
    private float polygonVerticalThickness;
    private int polygonHeightGridDivision;
    private int polygonMaxGridSpacing;
    
    // === Polyhedron 設定 ===
    private Material polyhedronLineMaterial;
    private Material polyhedronVertex0Material;
    private Material polyhedronVertexMaterial;
    private float polyhedronLineThickness;
    private float polyhedronVertexSize;
    private float polyhedronVertexThickness;
    
    public RenderSettings(WorldEditDisplay plugin) {
        this.plugin = plugin;
        loadDefaults();
    }
    
    /**
     * 載入預設值
     */
    private void loadDefaults() {
        // 玩家設定限制預設值
        thicknessMin = 0.01;
        thicknessMax = 0.5;
        markerSizeMin = 0.1;
        markerSizeMax = 5.0;
        segmentsMin = 10;
        segmentsMax = 120;
        gridDivisionMin = 1;
        gridDivisionMax = 50;
        gridSpacingMin = -1;
        gridSpacingMax = 100;
        targetSegmentLengthMin = 0.1;
        targetSegmentLengthMax = 5.0;
        scaleFactorMin = 0.5;
        scaleFactorMax = 10.0;
        
        // Cuboid 預設值
        cuboidEdgeMaterial = Material.GOLD_BLOCK;
        cuboidPoint1Material = Material.DIAMOND_BLOCK;
        cuboidPoint2Material = Material.EMERALD_BLOCK;
        cuboidGridMaterial = Material.IRON_BLOCK;
        cuboidEdgeThickness = 0.05f;
        cuboidGridThickness = 0.03f;
        cuboidVertexMarkerSize = 1.0f;
        cuboidHeightGridDivision = 10;
        cuboidMaxGridSpacing = -1;
        
        // Cylinder 預設值
        cylinderCircleMaterial = Material.GOLD_BLOCK;
        cylinderGridMaterial = Material.IRON_BLOCK;
        cylinderCenterMaterial = Material.GLOWSTONE;
        cylinderCenterLineMaterial = Material.REDSTONE_BLOCK;
        cylinderCircleThickness = 0.08f;
        cylinderGridThickness = 0.05f;
        cylinderCenterLineThickness = 0.08f;
        cylinderCenterThickness = 0.05f;
        cylinderMinCircleSegments = 30;
        cylinderMaxCircleSegments = 60;
        cylinderTargetSegmentLength = 0.5;
        cylinderSqrtScaleFactor = 4.0;
        cylinderHeightGridDivision = 10;
        cylinderRadiusGridDivision = 5;
        cylinderMaxGridSpacing = -1;
        
        // Ellipsoid 預設值
        ellipsoidLineMaterial = Material.GOLD_BLOCK;
        ellipsoidCenterLineMaterial = Material.REDSTONE_BLOCK;
        ellipsoidCenterMaterial = Material.GLOWSTONE;
        ellipsoidLineThickness = 0.06f;
        ellipsoidCenterLineThickness = 0.08f;
        ellipsoidCenterMarkerSize = 1.0f;
        ellipsoidCenterThickness = 0.05f;
        ellipsoidMinSegments = 20;
        ellipsoidMaxSegments = 40;
        ellipsoidTargetSegmentLength = 0.5;
        ellipsoidSqrtScaleFactor = 4.0;
        ellipsoidRadiusGridDivision = 6;
        ellipsoidMaxGridSpacing = -1;
        
        // Polygon 預設值
        polygonEdgeMaterial = Material.GOLD_BLOCK;
        polygonVertexMaterial = Material.DIAMOND_BLOCK;
        polygonVerticalMaterial = Material.IRON_BLOCK;
        polygonEdgeThickness = 0.05f;
        polygonVerticalThickness = 0.04f;
        polygonHeightGridDivision = 10;
        polygonMaxGridSpacing = -1;
        
        // Polyhedron 預設值
        polyhedronLineMaterial = Material.CYAN_STAINED_GLASS;
        polyhedronVertex0Material = Material.ORANGE_STAINED_GLASS;
        polyhedronVertexMaterial = Material.YELLOW_STAINED_GLASS;
        polyhedronLineThickness = 0.03f;
        polyhedronVertexSize = 1.0f;
        polyhedronVertexThickness = 0.03f;
    }
    
    /**
     * 從配置文件重新載入設定
     */
    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        try {
            // 載入玩家設定限制
            loadPlayerLimits(config.getConfigurationSection("player_limits"));
            
            // 載入 Cuboid 設定
            loadCuboidSettings(config.getConfigurationSection("renderer.cuboid"));
            
            // 載入 Cylinder 設定
            loadCylinderSettings(config.getConfigurationSection("renderer.cylinder"));
            
            // 載入 Ellipsoid 設定
            loadEllipsoidSettings(config.getConfigurationSection("renderer.ellipsoid"));
            
            // 載入 Polygon 設定
            loadPolygonSettings(config.getConfigurationSection("renderer.polygon"));
            
            // 載入 Polyhedron 設定
            loadPolyhedronSettings(config.getConfigurationSection("renderer.polyhedron"));
            
        } catch (Exception e) {
            loadDefaults();
        }
    }
    
    /**
     * 載入玩家設定限制
     */
    private void loadPlayerLimits(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        
        // 載入各項限制
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
    }
    
    /**
     * 載入 Cuboid 設定
     */
    private void loadCuboidSettings(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        
        cuboidEdgeMaterial = getMaterial(section, "edge_material", cuboidEdgeMaterial);
        cuboidPoint1Material = getMaterial(section, "point1_material", cuboidPoint1Material);
        cuboidPoint2Material = getMaterial(section, "point2_material", cuboidPoint2Material);
        cuboidGridMaterial = getMaterial(section, "grid_material", cuboidGridMaterial);
        cuboidEdgeThickness = (float) section.getDouble("edge_thickness", cuboidEdgeThickness);
        cuboidGridThickness = (float) section.getDouble("grid_thickness", cuboidGridThickness);
        cuboidVertexMarkerSize = (float) section.getDouble("vertex_marker_size", cuboidVertexMarkerSize);
        cuboidHeightGridDivision = section.getInt("height_grid_division", cuboidHeightGridDivision);
        cuboidMaxGridSpacing = section.getInt("max_grid_spacing", cuboidMaxGridSpacing);
    }
    
    /**
     * 載入 Cylinder 設定
     */
    private void loadCylinderSettings(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        
        cylinderCircleMaterial = getMaterial(section, "circle_material", cylinderCircleMaterial);
        cylinderGridMaterial = getMaterial(section, "grid_material", cylinderGridMaterial);
        cylinderCenterMaterial = getMaterial(section, "center_material", cylinderCenterMaterial);
        cylinderCenterLineMaterial = getMaterial(section, "center_line_material", cylinderCenterLineMaterial);
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
    }
    
    /**
     * 載入 Ellipsoid 設定
     */
    private void loadEllipsoidSettings(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        
        ellipsoidLineMaterial = getMaterial(section, "line_material", ellipsoidLineMaterial);
        ellipsoidCenterLineMaterial = getMaterial(section, "center_line_material", ellipsoidCenterLineMaterial);
        ellipsoidCenterMaterial = getMaterial(section, "center_material", ellipsoidCenterMaterial);
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
    }
    
    /**
     * 載入 Polygon 設定
     */
    private void loadPolygonSettings(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        
        polygonEdgeMaterial = getMaterial(section, "edge_material", polygonEdgeMaterial);
        polygonVertexMaterial = getMaterial(section, "vertex_material", polygonVertexMaterial);
        polygonVerticalMaterial = getMaterial(section, "vertical_material", polygonVerticalMaterial);
        polygonEdgeThickness = (float) section.getDouble("edge_thickness", polygonEdgeThickness);
        polygonVerticalThickness = (float) section.getDouble("vertical_thickness", polygonVerticalThickness);
        polygonHeightGridDivision = section.getInt("height_grid_division", polygonHeightGridDivision);
        polygonMaxGridSpacing = section.getInt("max_grid_spacing", polygonMaxGridSpacing);
    }
    
    /**
     * 載入 Polyhedron 設定
     */
    private void loadPolyhedronSettings(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        
        polyhedronLineMaterial = getMaterial(section, "line_material", polyhedronLineMaterial);
        polyhedronVertex0Material = getMaterial(section, "vertex0_material", polyhedronVertex0Material);
        polyhedronVertexMaterial = getMaterial(section, "vertex_material", polyhedronVertexMaterial);
        polyhedronLineThickness = (float) section.getDouble("line_thickness", polyhedronLineThickness);
        polyhedronVertexSize = (float) section.getDouble("vertex_size", polyhedronVertexSize);
        polyhedronVertexThickness = (float) section.getDouble("vertex_thickness", polyhedronVertexThickness);
    }
    
    /**
     * 從配置中讀取 Material
     * 
     * @param section 配置區段
     * @param key 鍵名
     * @param defaultValue 預設值
     * @return Material
     */
    private Material getMaterial(ConfigurationSection section, String key, Material defaultValue) {
        String materialName = section.getString(key);
        if (materialName == null || materialName.isEmpty()) {
            return defaultValue;
        }
        
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            return material;
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
    
    // === Cuboid Getters ===
    
    public Material getCuboidEdgeMaterial() {
        return cuboidEdgeMaterial;
    }
    
    public Material getCuboidPoint1Material() {
        return cuboidPoint1Material;
    }
    
    public Material getCuboidPoint2Material() {
        return cuboidPoint2Material;
    }
    
    public Material getCuboidGridMaterial() {
        return cuboidGridMaterial;
    }
    
    public float getCuboidEdgeThickness() {
        return cuboidEdgeThickness;
    }
    
    public float getCuboidGridThickness() {
        return cuboidGridThickness;
    }
    
    public float getCuboidVertexMarkerSize() {
        return cuboidVertexMarkerSize;
    }
    
    public int getCuboidHeightGridDivision() {
        return cuboidHeightGridDivision;
    }
    
    public int getCuboidMaxGridSpacing() {
        return cuboidMaxGridSpacing;
    }
    
    // === Cylinder Getters ===
    
    public Material getCylinderCircleMaterial() {
        return cylinderCircleMaterial;
    }
    
    public Material getCylinderGridMaterial() {
        return cylinderGridMaterial;
    }
    
    public Material getCylinderCenterMaterial() {
        return cylinderCenterMaterial;
    }
    
    public Material getCylinderCenterLineMaterial() {
        return cylinderCenterLineMaterial;
    }
    
    public float getCylinderCircleThickness() {
        return cylinderCircleThickness;
    }
    
    public float getCylinderGridThickness() {
        return cylinderGridThickness;
    }
    
    public float getCylinderCenterLineThickness() {
        return cylinderCenterLineThickness;
    }
    
    public float getCylinderCenterThickness() {
        return cylinderCenterThickness;
    }
    
    public int getCylinderMinCircleSegments() {
        return cylinderMinCircleSegments;
    }
    
    public int getCylinderMaxCircleSegments() {
        return cylinderMaxCircleSegments;
    }
    
    public double getCylinderTargetSegmentLength() {
        return cylinderTargetSegmentLength;
    }
    
    public double getCylinderSqrtScaleFactor() {
        return cylinderSqrtScaleFactor;
    }
    
    public int getCylinderHeightGridDivision() {
        return cylinderHeightGridDivision;
    }
    
    public int getCylinderRadiusGridDivision() {
        return cylinderRadiusGridDivision;
    }
    
    public int getCylinderMaxGridSpacing() {
        return cylinderMaxGridSpacing;
    }
    
    // === Ellipsoid Getters ===
    
    public Material getEllipsoidLineMaterial() {
        return ellipsoidLineMaterial;
    }
    
    public Material getEllipsoidCenterLineMaterial() {
        return ellipsoidCenterLineMaterial;
    }
    
    public Material getEllipsoidCenterMaterial() {
        return ellipsoidCenterMaterial;
    }
    
    public float getEllipsoidLineThickness() {
        return ellipsoidLineThickness;
    }
    
    public float getEllipsoidCenterLineThickness() {
        return ellipsoidCenterLineThickness;
    }
    
    public float getEllipsoidCenterMarkerSize() {
        return ellipsoidCenterMarkerSize;
    }
    
    public float getEllipsoidCenterThickness() {
        return ellipsoidCenterThickness;
    }
    
    public int getEllipsoidMinSegments() {
        return ellipsoidMinSegments;
    }
    
    public int getEllipsoidMaxSegments() {
        return ellipsoidMaxSegments;
    }
    
    public double getEllipsoidTargetSegmentLength() {
        return ellipsoidTargetSegmentLength;
    }
    
    public double getEllipsoidSqrtScaleFactor() {
        return ellipsoidSqrtScaleFactor;
    }
    
    public int getEllipsoidRadiusGridDivision() {
        return ellipsoidRadiusGridDivision;
    }
    
    public int getEllipsoidMaxGridSpacing() {
        return ellipsoidMaxGridSpacing;
    }
    
    // === Polygon Getters ===
    
    public Material getPolygonEdgeMaterial() {
        return polygonEdgeMaterial;
    }
    
    public Material getPolygonVertexMaterial() {
        return polygonVertexMaterial;
    }
    
    public Material getPolygonVerticalMaterial() {
        return polygonVerticalMaterial;
    }
    
    public float getPolygonEdgeThickness() {
        return polygonEdgeThickness;
    }
    
    public float getPolygonVerticalThickness() {
        return polygonVerticalThickness;
    }
    
    public int getPolygonHeightGridDivision() {
        return polygonHeightGridDivision;
    }
    
    public int getPolygonMaxGridSpacing() {
        return polygonMaxGridSpacing;
    }
    
    // === Polyhedron Getters ===
    
    public Material getPolyhedronLineMaterial() {
        return polyhedronLineMaterial;
    }
    
    public Material getPolyhedronVertex0Material() {
        return polyhedronVertex0Material;
    }
    
    public Material getPolyhedronVertexMaterial() {
        return polyhedronVertexMaterial;
    }
    
    public float getPolyhedronLineThickness() {
        return polyhedronLineThickness;
    }
    
    public float getPolyhedronVertexSize() {
        return polyhedronVertexSize;
    }
    
    public float getPolyhedronVertexThickness() {
        return polyhedronVertexThickness;
    }
    
    // === 玩家設定限制 Getters ===
    
    public double getThicknessMin() {
        return thicknessMin;
    }
    
    public double getThicknessMax() {
        return thicknessMax;
    }
    
    public double getMarkerSizeMin() {
        return markerSizeMin;
    }
    
    public double getMarkerSizeMax() {
        return markerSizeMax;
    }
    
    public int getSegmentsMin() {
        return segmentsMin;
    }
    
    public int getSegmentsMax() {
        return segmentsMax;
    }
    
    public int getGridDivisionMin() {
        return gridDivisionMin;
    }
    
    public int getGridDivisionMax() {
        return gridDivisionMax;
    }
    
    public int getGridSpacingMin() {
        return gridSpacingMin;
    }
    
    public int getGridSpacingMax() {
        return gridSpacingMax;
    }
    
    public double getTargetSegmentLengthMin() {
        return targetSegmentLengthMin;
    }
    
    public double getTargetSegmentLengthMax() {
        return targetSegmentLengthMax;
    }
    
    public double getScaleFactorMin() {
        return scaleFactorMin;
    }
    
    public double getScaleFactorMax() {
        return scaleFactorMax;
    }
}
