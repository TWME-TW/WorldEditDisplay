package dev.twme.worldeditdisplay.display.renderer;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.BoundingBox;
import dev.twme.worldeditdisplay.region.CuboidRegion;
import dev.twme.worldeditdisplay.region.Vector3;

import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

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
    public void render(CuboidRegion region) {
        clear(); // remove old lines

        boolean isMultiSelection = isMultiSelection(region);
        Vector3 point1 = region.getPoint1();
        Vector3 point2 = region.getPoint2();

        if (point1 == null && point2 == null) return; // nothing to render

        // determine materials
        Material point1Material = getMaterialWithOverride(region, 2, settings.getCuboidPoint1Material(), isMultiSelection);
        Material point2Material = getMaterialWithOverride(region, 3, settings.getCuboidPoint2Material(), isMultiSelection);
        Material boxMaterial = getMaterialWithOverride(region, 0, settings.getCuboidEdgeMaterial(), isMultiSelection);
        Material gridMaterial = getMaterialWithOverride(region, 1, settings.getCuboidGridMaterial(), isMultiSelection);

        // draw point markers
        if (point1 != null) renderPointMarker(point1, point1Material, settings.getCuboidEdgeThickness());
        if (point2 != null) renderPointMarker(point2, point2Material, settings.getCuboidEdgeThickness());

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

        if (renderBox) renderBoxFrame(minX, minY, minZ, maxX, maxY, maxZ, boxMaterial, settings.getCuboidEdgeThickness());
        if (renderGrid) renderGrid(minX, minY, minZ, maxX, maxY, maxZ, region, gridMaterial);
    }

    /** Draws a grid on the six faces of the cuboid */
    private void renderGrid(double x1, double y1, double z1, double x2, double y2, double z2,
                            CuboidRegion region, Material gridMaterial) {

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
        renderXZPlane(x1, y1, z1, x2, z2, spacingX, spacingZ, gridMaterial);
        renderXZPlane(x1, y2, z1, x2, z2, spacingX, spacingZ, gridMaterial);
        renderXYPlane(x1, y1, z1, x2, y2, spacingX, spacingY, gridMaterial);
        renderXYPlane(x1, y1, z2, x2, y2, spacingX, spacingY, gridMaterial);
        renderYZPlane(x1, y1, z1, y2, z2, spacingY, spacingZ, gridMaterial);
        renderYZPlane(x2, y1, z1, y2, z2, spacingY, spacingZ, gridMaterial);
    }

    private void renderXZPlane(double x1, double y, double z1, double x2, double z2,
                               double spacingX, double spacingZ, Material material) {
        List<Line> lines = new ArrayList<>();
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
        renderLines(material, settings.getCuboidGridThickness(), lines.toArray(new Line[0]));
    }

    private void renderXYPlane(double x1, double y1, double z, double x2, double y2,
                               double spacingX, double spacingY, Material material) {
        List<Line> lines = new ArrayList<>();
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
        renderLines(material, settings.getCuboidGridThickness(), lines.toArray(new Line[0]));
    }

    private void renderYZPlane(double x, double y1, double z1, double y2, double z2,
                               double spacingY, double spacingZ, Material material) {
        List<Line> lines = new ArrayList<>();
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
        renderLines(material, settings.getCuboidGridThickness(), lines.toArray(new Line[0]));
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
