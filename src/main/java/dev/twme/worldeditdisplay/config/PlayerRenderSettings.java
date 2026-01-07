package dev.twme.worldeditdisplay.config;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import dev.twme.worldeditdisplay.WorldEditDisplay;

/**
 * Stores per-player render settings.
 * Falls back to server defaults when player-specific value is null.
 */
public class PlayerRenderSettings {

    private final WorldEditDisplay plugin;
    private final UUID playerUUID;
    private final RenderSettings serverSettings;
    private final File configFile;
    private FileConfiguration config;

    // Player custom values (null = use server default)
    private Material cuboidEdgeMaterial;
    private Material cuboidPoint1Material;
    private Material cuboidPoint2Material;
    private Material cuboidGridMaterial;
    private Float cuboidEdgeThickness;
    private Float cuboidGridThickness;
    private Float cuboidVertexMarkerSize;
    private Integer cuboidHeightGridDivision;

    // Cylinder
    private Material cylinderCircleMaterial;
    private Material cylinderGridMaterial;
    private Material cylinderCenterMaterial;
    private Material cylinderCenterLineMaterial;
    private Float cylinderCircleThickness;
    private Float cylinderGridThickness;
    private Float cylinderCenterLineThickness;
    private Float cylinderCenterThickness;
    private Integer cylinderMinCircleSegments;
    private Integer cylinderMaxCircleSegments;
    private Double cylinderTargetSegmentLength;
    private Integer cylinderHeightGridDivision;
    private Integer cylinderRadiusGridDivision;

    // Ellipsoid
    private Material ellipsoidLineMaterial;
    private Material ellipsoidCenterLineMaterial;
    private Material ellipsoidCenterMaterial;
    private Float ellipsoidLineThickness;
    private Float ellipsoidCenterLineThickness;
    private Float ellipsoidCenterMarkerSize;
    private Float ellipsoidCenterThickness;
    private Integer ellipsoidMinSegments;
    private Integer ellipsoidMaxSegments;
    private Double ellipsoidTargetSegmentLength;
    private Integer ellipsoidRadiusGridDivision;

    // Polygon
    private Material polygonEdgeMaterial;
    private Material polygonVertexMaterial;
    private Material polygonVerticalMaterial;
    private Float polygonEdgeThickness;
    private Float polygonVerticalThickness;
    private Integer polygonHeightGridDivision;

    // Polyhedron
    private Material polyhedronLineMaterial;
    private Material polyhedronVertex0Material;
    private Material polyhedronVertexMaterial;
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

    /** Load settings from YAML file */
    public void load() {
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

    /** Save settings to file */
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException ignored) {}
    }

    private void loadCuboidSettings(ConfigurationSection section) {
        if (section == null) return;
        cuboidEdgeMaterial = getMaterial(section, "edge_material");
        cuboidPoint1Material = getMaterial(section, "point1_material");
        cuboidPoint2Material = getMaterial(section, "point2_material");
        cuboidGridMaterial = getMaterial(section, "grid_material");
        cuboidEdgeThickness = getFloat(section, "edge_thickness");
        cuboidGridThickness = getFloat(section, "grid_thickness");
        cuboidVertexMarkerSize = getFloat(section, "vertex_marker_size");
        cuboidHeightGridDivision = getInt(section, "height_grid_division");
    }

    private void loadCylinderSettings(ConfigurationSection section) {
        if (section == null) return;
        cylinderCircleMaterial = getMaterial(section, "circle_material");
        cylinderGridMaterial = getMaterial(section, "grid_material");
        cylinderCenterMaterial = getMaterial(section, "center_material");
        cylinderCenterLineMaterial = getMaterial(section, "center_line_material");
        cylinderCircleThickness = getFloat(section, "circle_thickness");
        cylinderGridThickness = getFloat(section, "grid_thickness");
        cylinderCenterLineThickness = getFloat(section, "center_line_thickness");
        cylinderCenterThickness = getFloat(section, "center_thickness");
        cylinderMinCircleSegments = getInt(section, "min_circle_segments");
        cylinderMaxCircleSegments = getInt(section, "max_circle_segments");
        cylinderTargetSegmentLength = getDouble(section, "target_segment_length");
        cylinderHeightGridDivision = getInt(section, "height_grid_division");
        cylinderRadiusGridDivision = getInt(section, "radius_grid_division");
    }

    private void loadEllipsoidSettings(ConfigurationSection section) {
        if (section == null) return;
        ellipsoidLineMaterial = getMaterial(section, "line_material");
        ellipsoidCenterLineMaterial = getMaterial(section, "center_line_material");
        ellipsoidCenterMaterial = getMaterial(section, "center_material");
        ellipsoidLineThickness = getFloat(section, "line_thickness");
        ellipsoidCenterLineThickness = getFloat(section, "center_line_thickness");
        ellipsoidCenterMarkerSize = getFloat(section, "center_marker_size");
        ellipsoidCenterThickness = getFloat(section, "center_thickness");
        ellipsoidMinSegments = getInt(section, "min_segments");
        ellipsoidMaxSegments = getInt(section, "max_segments");
        ellipsoidTargetSegmentLength = getDouble(section, "target_segment_length");
        ellipsoidRadiusGridDivision = getInt(section, "radius_grid_division");
    }

    private void loadPolygonSettings(ConfigurationSection section) {
        if (section == null) return;
        polygonEdgeMaterial = getMaterial(section, "edge_material");
        polygonVertexMaterial = getMaterial(section, "vertex_material");
        polygonVerticalMaterial = getMaterial(section, "vertical_material");
        polygonEdgeThickness = getFloat(section, "edge_thickness");
        polygonVerticalThickness = getFloat(section, "vertical_thickness");
        polygonHeightGridDivision = getInt(section, "height_grid_division");
    }

    private void loadPolyhedronSettings(ConfigurationSection section) {
        if (section == null) return;
        polyhedronLineMaterial = getMaterial(section, "line_material");
        polyhedronVertex0Material = getMaterial(section, "vertex0_material");
        polyhedronVertexMaterial = getMaterial(section, "vertex_material");
        polyhedronLineThickness = getFloat(section, "line_thickness");
        polyhedronVertexSize = getFloat(section, "vertex_size");
        polyhedronVertexThickness = getFloat(section, "vertex_thickness");
    }

    private Material getMaterial(ConfigurationSection section, String key) {
        String value = section.getString(key);
        if (value == null) return null;
        try { return Material.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
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

    /** Set a value (validated). Returns true if successful. */
    public boolean set(String path, Object value) {
        if (value instanceof Material) value = ((Material) value).name();
        if (value instanceof String) {
            try { Material.valueOf(((String) value).toUpperCase()); }
            catch (IllegalArgumentException e) { return false; }
        }

        if (value instanceof Number && !validateNumericValue(path, ((Number) value).doubleValue())) return false;

        config.set(path, value);
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
        return true;
    }

    /** Reset a setting to server default */
    public void reset(String path) { config.set(path, null); save(); load(); }

    /** Reset all player settings */
    public void resetAll() {
        if (configFile.exists()) configFile.delete();
        config = new YamlConfiguration();
        load();
    }

    /** Cuboid Getters **/
    public Material getCuboidEdgeMaterial() { return cuboidEdgeMaterial != null ? cuboidEdgeMaterial : serverSettings.getCuboidEdgeMaterial(); }
    public Material getCuboidPoint1Material() { return cuboidPoint1Material != null ? cuboidPoint1Material : serverSettings.getCuboidPoint1Material(); }
    public Material getCuboidPoint2Material() { return cuboidPoint2Material != null ? cuboidPoint2Material : serverSettings.getCuboidPoint2Material(); }
    public Material getCuboidGridMaterial() { return cuboidGridMaterial != null ? cuboidGridMaterial : serverSettings.getCuboidGridMaterial(); }
    public float getCuboidEdgeThickness() { return cuboidEdgeThickness != null ? cuboidEdgeThickness : serverSettings.getCuboidEdgeThickness(); }
    public float getCuboidGridThickness() { return cuboidGridThickness != null ? cuboidGridThickness : serverSettings.getCuboidGridThickness(); }
    public float getCuboidVertexMarkerSize() { return cuboidVertexMarkerSize != null ? cuboidVertexMarkerSize : serverSettings.getCuboidVertexMarkerSize(); }
    public int getCuboidHeightGridDivision() { return cuboidHeightGridDivision != null ? cuboidHeightGridDivision : serverSettings.getCuboidHeightGridDivision(); }
    public int getCuboidMaxGridSpacing() { return serverSettings.getCuboidMaxGridSpacing(); }

    /** Cylinder Getters **/
    public Material getCylinderCircleMaterial() { return cylinderCircleMaterial != null ? cylinderCircleMaterial : serverSettings.getCylinderCircleMaterial(); }
    public Material getCylinderGridMaterial() { return cylinderGridMaterial != null ? cylinderGridMaterial : serverSettings.getCylinderGridMaterial(); }
    public Material getCylinderCenterMaterial() { return cylinderCenterMaterial != null ? cylinderCenterMaterial : serverSettings.getCylinderCenterMaterial(); }
    public Material getCylinderCenterLineMaterial() { return cylinderCenterLineMaterial != null ? cylinderCenterLineMaterial : serverSettings.getCylinderCenterLineMaterial(); }
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

    /** Ellipsoid Getters **/
    public Material getEllipsoidLineMaterial() { return ellipsoidLineMaterial != null ? ellipsoidLineMaterial : serverSettings.getEllipsoidLineMaterial(); }
    public Material getEllipsoidCenterLineMaterial() { return ellipsoidCenterLineMaterial != null ? ellipsoidCenterLineMaterial : serverSettings.getEllipsoidCenterLineMaterial(); }
    public Material getEllipsoidCenterMaterial() { return ellipsoidCenterMaterial != null ? ellipsoidCenterMaterial : serverSettings.getEllipsoidCenterMaterial(); }
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

    /** Polygon Getters **/
    public Material getPolygonEdgeMaterial() { return polygonEdgeMaterial != null ? polygonEdgeMaterial : serverSettings.getPolygonEdgeMaterial(); }
    public Material getPolygonVertexMaterial() { return polygonVertexMaterial != null ? polygonVertexMaterial : serverSettings.getPolygonVertexMaterial(); }
    public Material getPolygonVerticalMaterial() { return polygonVerticalMaterial != null ? polygonVerticalMaterial : serverSettings.getPolygonVerticalMaterial(); }
    public float getPolygonEdgeThickness() { return polygonEdgeThickness != null ? polygonEdgeThickness : serverSettings.getPolygonEdgeThickness(); }
    public float getPolygonVerticalThickness() { return polygonVerticalThickness != null ? polygonVerticalThickness : serverSettings.getPolygonVerticalThickness(); }
    public int getPolygonHeightGridDivision() { return polygonHeightGridDivision != null ? polygonHeightGridDivision : serverSettings.getPolygonHeightGridDivision(); }
    public int getPolygonMaxGridSpacing() { return serverSettings.getPolygonMaxGridSpacing(); }

    /** Polyhedron Getters **/
    public Material getPolyhedronLineMaterial() { return polyhedronLineMaterial != null ? polyhedronLineMaterial : serverSettings.getPolyhedronLineMaterial(); }
    public Material getPolyhedronVertex0Material() { return polyhedronVertex0Material != null ? polyhedronVertex0Material : serverSettings.getPolyhedronVertex0Material(); }
    public Material getPolyhedronVertexMaterial() { return polyhedronVertexMaterial != null ? polyhedronVertexMaterial : serverSettings.getPolyhedronVertexMaterial(); }
    public float getPolyhedronLineThickness() { return polyhedronLineThickness != null ? polyhedronLineThickness : serverSettings.getPolyhedronLineThickness(); }
    public float getPolyhedronVertexSize() { return polyhedronVertexSize != null ? polyhedronVertexSize : serverSettings.getPolyhedronVertexSize(); }
    public float getPolyhedronVertexThickness() { return polyhedronVertexThickness != null ? polyhedronVertexThickness : serverSettings.getPolyhedronVertexThickness(); }

    public UUID getPlayerUUID() { return playerUUID; }
}
