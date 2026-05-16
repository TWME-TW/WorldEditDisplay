package dev.twme.worldeditdisplay.display.renderer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.BoundingBox;
import dev.twme.worldeditdisplay.region.CuboidRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * Renders WorldEdit-style cuboid selections.
 * Shows edges, grid on faces, and point markers.
 */
public class CuboidRenderer extends RegionRenderer<CuboidRegion> {

    private static final double MIN_SPACING = 1.0;
    private static final double SKIP_THRESHOLD = 0.25;

    private boolean renderGrid = true;
    private boolean renderBox = true;

    public CuboidRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    protected boolean isSeeThrough() {
        return settings.isCuboidSeeThrough();
    }

    @Override
    public void render(CuboidRegion region) {
        clear(); // remove old lines

        boolean isMultiSelection = isMultiSelection(region);
        Vector3 point1 = region.getPoint1();
        Vector3 point2 = region.getPoint2();

        if (point1 == null && point2 == null) return; // nothing to render

        // determine colors
        Color point1Color = getColorWithOverride(region, 2, settings.getCuboidPoint1Color(), isMultiSelection);
        Color point2Color = getColorWithOverride(region, 3, settings.getCuboidPoint2Color(), isMultiSelection);
        Color boxColor = getColorWithOverride(region, 0, settings.getCuboidEdgeColor(), isMultiSelection);
        Color gridColor = getColorWithOverride(region, 1, settings.getCuboidGridColor(), isMultiSelection);

        // draw point markers
        if (point1 != null) renderPointMarker(point1, point1Color, settings.getCuboidEdgeThickness());
        if (point2 != null) renderPointMarker(point2, point2Color, settings.getCuboidEdgeThickness());

        if (!region.isDefined()) return; // need both points for box/grid

        BoundingBox regionBox = region.getBoundingBox();
        if (regionBox == null) return;

        Vector3 min = regionBox.getMin();
        Vector3 max = regionBox.getMax();

        // extend max coordinates by 1 to fully encompass blocks
        double minX = min.getX();
        double minY = min.getY();
        double minZ = min.getZ();
        double maxX = max.getX() + 1.0;
        double maxY = max.getY() + 1.0;
        double maxZ = max.getZ() + 1.0;

        if (renderBox) renderBoxFrame(minX, minY, minZ, maxX, maxY, maxZ, boxColor, settings.getCuboidEdgeThickness());
        if (renderGrid) renderGrid(minX, minY, minZ, maxX, maxY, maxZ, region, gridColor);

        // Render fill faces if enabled
        if (settings.isCuboidFillEnabled()) {
            Color fillColor = getFillColorWithOverride(region, 1, settings.getCuboidFillColor(), isMultiSelection);
            renderFillFaces(minX, minY, minZ, maxX, maxY, maxZ, fillColor);
        }
    }

    /** Draws a grid on the six faces of the cuboid */
    private void renderGrid(double x1, double y1, double z1, double x2, double y2, double z2,
                            CuboidRegion region, Color gridColor) {

        double sizeX = x2 - x1, sizeY = y2 - y1, sizeZ = z2 - z1;
        double gridSpacing = region.getGridSpacing();

        double spacingX, spacingY, spacingZ;

        if (gridSpacing > 0) {
            spacingX = spacingY = spacingZ = gridSpacing;
        } else {
            int gridDivision = settings.getCuboidHeightGridDivision();
            int maxGridSpacing = settings.getCuboidMaxGridSpacing();

            spacingX = Math.max(MIN_SPACING, (int) (sizeX / gridDivision));
            spacingY = Math.max(MIN_SPACING, (int) (sizeY / gridDivision));
            spacingZ = Math.max(MIN_SPACING, (int) (sizeZ / gridDivision));

            if (maxGridSpacing != -1) {
                spacingX = Math.min(spacingX, maxGridSpacing);
                spacingY = Math.min(spacingY, maxGridSpacing);
                spacingZ = Math.min(spacingZ, maxGridSpacing);
            }
        }

        if (sizeX < MIN_SPACING && sizeY < MIN_SPACING && sizeZ < MIN_SPACING) return;

        // draw grid on each face
        renderXZPlane(x1, y1, z1, x2, z2, spacingX, spacingZ, gridColor);
        renderXZPlane(x1, y2, z1, x2, z2, spacingX, spacingZ, gridColor);
        renderXYPlane(x1, y1, z1, x2, y2, spacingX, spacingY, gridColor);
        renderXYPlane(x1, y1, z2, x2, y2, spacingX, spacingY, gridColor);
        renderYZPlane(x1, y1, z1, y2, z2, spacingY, spacingZ, gridColor);
        renderYZPlane(x2, y1, z1, y2, z2, spacingY, spacingZ, gridColor);
    }

    private void renderXZPlane(double x1, double y, double z1, double x2, double z2,
                               double spacingX, double spacingZ, Color color) {
        int countZ = (int) Math.ceil((z2 - z1) / spacingZ) + 1;
        int countX = (int) Math.ceil((x2 - x1) / spacingX) + 1;
        List<Line> lines = new ArrayList<>(countZ + countX);
        for (double z = z1; z <= z2; z += spacingZ) {
            if (z > z1 && z2 - z < SKIP_THRESHOLD) continue;
            lines.add(new Line(new Vector3f((float) x1, (float) y, (float) z),
                    new Vector3f((float) x2, (float) y, (float) z)));
        }
        for (double x = x1; x <= x2; x += spacingX) {
            if (x > x1 && x2 - x < SKIP_THRESHOLD) continue;
            lines.add(new Line(new Vector3f((float) x, (float) y, (float) z1),
                    new Vector3f((float) x, (float) y, (float) z2)));
        }
        renderLines(color, settings.getCuboidGridThickness(), lines.toArray(new Line[0]));
    }

    private void renderXYPlane(double x1, double y1, double z, double x2, double y2,
                               double spacingX, double spacingY, Color color) {
        int countY = (int) Math.ceil((y2 - y1) / spacingY) + 1;
        int countX = (int) Math.ceil((x2 - x1) / spacingX) + 1;
        List<Line> lines = new ArrayList<>(countY + countX);
        for (double y = y1; y <= y2; y += spacingY) {
            if (y > y1 && y2 - y < SKIP_THRESHOLD) continue;
            lines.add(new Line(new Vector3f((float) x1, (float) y, (float) z),
                    new Vector3f((float) x2, (float) y, (float) z)));
        }
        for (double x = x1; x <= x2; x += spacingX) {
            if (x > x1 && x2 - x < SKIP_THRESHOLD) continue;
            lines.add(new Line(new Vector3f((float) x, (float) y1, (float) z),
                    new Vector3f((float) x, (float) y2, (float) z)));
        }
        renderLines(color, settings.getCuboidGridThickness(), lines.toArray(new Line[0]));
    }

    private void renderYZPlane(double x, double y1, double z1, double y2, double z2,
                               double spacingY, double spacingZ, Color color) {
        int countZ = (int) Math.ceil((z2 - z1) / spacingZ) + 1;
        int countY = (int) Math.ceil((y2 - y1) / spacingY) + 1;
        List<Line> lines = new ArrayList<>(countZ + countY);
        for (double z = z1; z <= z2; z += spacingZ) {
            if (z > z1 && z2 - z < SKIP_THRESHOLD) continue;
            lines.add(new Line(new Vector3f((float) x, (float) y1, (float) z),
                    new Vector3f((float) x, (float) y2, (float) z)));
        }
        for (double y = y1; y <= y2; y += spacingY) {
            if (y > y1 && y2 - y < SKIP_THRESHOLD) continue;
            lines.add(new Line(new Vector3f((float) x, (float) y, (float) z1),
                    new Vector3f((float) x, (float) y, (float) z2)));
        }
        renderLines(color, settings.getCuboidGridThickness(), lines.toArray(new Line[0]));
    }

    /** Renders fill faces for the 6 sides of the cuboid, one parallelogram per face */
    private void renderFillFaces(double x1, double y1, double z1, double x2, double y2, double z2, Color fillColor) {
        Vector3f v000 = new Vector3f((float) x1, (float) y1, (float) z1);
        Vector3f v001 = new Vector3f((float) x1, (float) y1, (float) z2);
        Vector3f v010 = new Vector3f((float) x1, (float) y2, (float) z1);
        Vector3f v011 = new Vector3f((float) x1, (float) y2, (float) z2);
        Vector3f v100 = new Vector3f((float) x2, (float) y1, (float) z1);
        Vector3f v101 = new Vector3f((float) x2, (float) y1, (float) z2);
        Vector3f v110 = new Vector3f((float) x2, (float) y2, (float) z1);
        Vector3f v111 = new Vector3f((float) x2, (float) y2, (float) z2);

        // Bottom face (y1): p1=v000, p2=v100 (width), p3=v001 (depth)
        renderParallelogram(v000, v100, v001, fillColor);
        // Top face (y2): p1=v010, p2=v110 (width), p3=v011 (depth)
        renderParallelogram(v010, v110, v011, fillColor);
        // Front face (z1): p1=v000, p2=v100 (width), p3=v010 (height)
        renderParallelogram(v000, v100, v010, fillColor);
        // Back face (z2): p1=v001, p2=v101 (width), p3=v011 (height)
        renderParallelogram(v001, v101, v011, fillColor);
        // Left face (x1): p1=v000, p2=v001 (depth), p3=v010 (height)
        renderParallelogram(v000, v001, v010, fillColor);
        // Right face (x2): p1=v100, p2=v101 (depth), p3=v110 (height)
        renderParallelogram(v100, v101, v110, fillColor);
    }

    public void setRenderGrid(boolean render) {
        this.renderGrid = render;
    }

    public void setRenderBox(boolean render) {
        this.renderBox = render;
    }

    @Override
    public Class<CuboidRegion> getRegionType() {
        return CuboidRegion.class;
    }
}
