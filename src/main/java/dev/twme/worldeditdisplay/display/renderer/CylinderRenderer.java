package dev.twme.worldeditdisplay.display.renderer;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.CylinderRegion;
import dev.twme.worldeditdisplay.region.Vector3;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

/**
 * Renders cylinder-shaped selections.
 * Shows circles at each Y layer, vertical grid lines, and a center cube.
 */
public class CylinderRenderer extends RegionRenderer<CylinderRegion> {

    public CylinderRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public void render(CylinderRegion region) {
        clear();

        boolean isMulti = isMultiSelection(region);
        Vector3 center = region.getCenter();
        if (center == null) return;

        double radiusX = region.getRadiusX();
        double radiusZ = region.getRadiusZ();
        int minY = region.getMinY();
        int maxY = region.getMaxY();

        double cxCircle = center.getX() + 0.5;
        double czCircle = center.getZ() + 0.5;

        Material circleMat = getMaterialWithOverride(region, 0, settings.getCylinderCircleMaterial(), isMulti);
        Material gridMat = getMaterialWithOverride(region, 1, settings.getCylinderGridMaterial(), isMulti);
        Material centerMat = getMaterialWithOverride(region, 2, settings.getCylinderCenterMaterial(), isMulti);
        Material centerLineMat = settings.getCylinderCenterLineMaterial();

        // If both radii are zero, just render the center cube
        if (radiusX == 0 && radiusZ == 0) {
            renderCube(new Vector3f((float)(center.getX() + 0.5),
                            (float)(center.getY() + 0.5),
                            (float)(center.getZ() + 0.5)),
                    1.03f, centerMat, settings.getCylinderCenterThickness());
            return;
        }

        // If one radius is zero, render as a rectangular grid
        if (radiusX == 0 || radiusZ == 0) {
            renderRectangularGrid(cxCircle, czCircle, radiusX, radiusZ, minY, maxY, gridMat, centerLineMat);
            renderCube(new Vector3f((float)(center.getX() + 0.5),
                            (float)(center.getY() + 0.5),
                            (float)(center.getZ() + 0.5)),
                    1.03f, centerMat, settings.getCylinderCenterThickness());
            return;
        }

        int height = maxY - minY + 1;
        int stepY = calculateGridStep(height);

        // Render circles for each layer
        for (int y = minY; y <= maxY + 1; y += stepY) {
            if (y == center.getY() || y == center.getY() + 1) continue;
            renderCircle(cxCircle, y, czCircle, radiusX, radiusZ, circleMat, settings.getCylinderCircleThickness());
        }

        // Ensure top layer is rendered
        if ((maxY + 1 - minY) % stepY != 0) {
            renderCircle(cxCircle, maxY + 1, czCircle, radiusX, radiusZ, circleMat, settings.getCylinderCircleThickness());
        }

        // Render center circle lines
        renderCircle(cxCircle, center.getY(), czCircle, radiusX, radiusZ, centerLineMat, settings.getCylinderCenterLineThickness());
        if (center.getY() + 1 != center.getY()) {
            renderCircle(cxCircle, center.getY() + 1, czCircle, radiusX, radiusZ, centerLineMat, settings.getCylinderCenterLineThickness());
        }

        // Render vertical grid lines
        renderGrid(cxCircle, czCircle, radiusX, radiusZ, minY, maxY + 1, gridMat, centerLineMat);

        // Render center cube
        renderCube(new Vector3f((float)(center.getX() + 0.5),
                        (float)(center.getY() + 0.5),
                        (float)(center.getZ() + 0.5)),
                1.03f, centerMat, settings.getCylinderCenterThickness());
    }

    private void renderRectangularGrid(double centerX, double centerZ,
                                       double radiusX, double radiusZ,
                                       int minY, int maxY,
                                       Material gridMat, Material centerLineMat) {
        int stepY = calculateGridStep(maxY - minY + 1);

        if (radiusX == 0) {
            double zMin = centerZ - radiusZ;
            double zMax = centerZ + radiusZ;

            for (int y = minY; y <= maxY + 1; y += stepY) {
                renderLines(gridMat, settings.getCylinderGridThickness(),
                        new Line(new Vector3f((float) centerX, (float)y, (float)zMin),
                                new Vector3f((float) centerX, (float)y, (float)zMax))
                );
            }

            for (int dz = (int)-Math.ceil(radiusZ); dz <= Math.ceil(radiusZ); dz++) {
                double z = centerZ + dz;
                Material mat = (dz == 0) ? centerLineMat : gridMat;
                float thick = (dz == 0) ? settings.getCylinderCenterLineThickness() : settings.getCylinderGridThickness();
                renderLine(new Line(new Vector3f((float) centerX, (float)minY, (float)z),
                                new Vector3f((float) centerX, (float)(maxY + 1), (float)z)),
                        mat, thick);
            }

        } else if (radiusZ == 0) {
            double xMin = centerX - radiusX;
            double xMax = centerX + radiusX;

            for (int y = minY; y <= maxY + 1; y += stepY) {
                renderLines(gridMat, settings.getCylinderGridThickness(),
                        new Line(new Vector3f((float)xMin, (float)y, (float) centerZ),
                                new Vector3f((float)xMax, (float)y, (float) centerZ))
                );
            }

            for (int dx = (int)-Math.ceil(radiusX); dx <= Math.ceil(radiusX); dx++) {
                double x = centerX + dx;
                Material mat = (dx == 0) ? centerLineMat : gridMat;
                float thick = (dx == 0) ? settings.getCylinderCenterLineThickness() : settings.getCylinderGridThickness();
                renderLine(new Line(new Vector3f((float)x, (float)minY, (float) centerZ),
                                new Vector3f((float)x, (float)(maxY + 1), (float) centerZ)),
                        mat, thick);
            }
        }
    }

    private int calculateGridStep(int height) {
        int step = Math.max(1, height / settings.getCylinderHeightGridDivision());
        if (settings.getCylinderMaxGridSpacing() != -1) {
            step = Math.min(step, settings.getCylinderMaxGridSpacing());
        }
        return step;
    }

    private int calculateCircleSegments(double radiusX, double radiusZ) {
        double avg = (radiusX + radiusZ) / 2.0;
        int byLength = (int)Math.ceil(2 * Math.PI * avg / settings.getCylinderTargetSegmentLength());
        int byRadius = (int)(settings.getCylinderMinCircleSegments() + settings.getCylinderSqrtScaleFactor() * Math.sqrt(avg));
        int segments = Math.max(byLength, byRadius);
        return Math.max(settings.getCylinderMinCircleSegments(),
                Math.min(segments, settings.getCylinderMaxCircleSegments()));
    }

    private void renderCircle(double cx, double y, double cz,
                              double radiusX, double radiusZ,
                              Material mat, float thickness) {
        int segments = calculateCircleSegments(radiusX, radiusZ);
        Vector3f[] points = new Vector3f[segments];
        double twoPi = Math.PI * 2;

        for (int i = 0; i < segments; i++) {
            double angle = i * twoPi / segments;
            points[i] = new Vector3f((float)(cx + radiusX * Math.cos(angle)),
                    (float)y,
                    (float)(cz + radiusZ * Math.sin(angle)));
        }

        for (int i = 0; i < segments; i++) {
            renderLine(new Line(points[i], points[(i + 1) % segments]), mat, thickness);
        }
    }

    private void renderGrid(double centerX, double centerZ,
                            double radiusX, double radiusZ,
                            int minY, int maxY,
                            Material gridMat, Material centerLineMat) {
        int posX = (int)Math.ceil(radiusX), negX = (int)-Math.ceil(radiusX);
        int posZ = (int)Math.ceil(radiusZ), negZ = (int)-Math.ceil(radiusZ);

        int xStep = calculateXGridStep(radiusX);
        int zStep = calculateZGridStep(radiusZ);

        for (int dx = negX; dx <= posX; dx += xStep) {
            double x = centerX + dx;
            Material mat = (dx == 0) ? centerLineMat : gridMat;
            float thick = (dx == 0) ? settings.getCylinderCenterLineThickness() : settings.getCylinderGridThickness();

            double ratio = dx / radiusX;
            if (Math.abs(ratio) > 1.0) continue;
            double offsetZ = radiusZ * Math.cos(Math.asin(ratio));
            double z1 = centerZ - offsetZ, z2 = centerZ + offsetZ;

            renderLines(mat, thick,
                    new Line(new Vector3f((float)x, (float)minY, (float)z1),
                            new Vector3f((float)x, (float)maxY, (float)z1)),
                    new Line(new Vector3f((float)x, (float)minY, (float)z2),
                            new Vector3f((float)x, (float)maxY, (float)z2))
            );
        }

        for (int dz = negZ; dz <= posZ; dz += zStep) {
            double z = centerZ + dz;
            Material mat = (dz == 0) ? centerLineMat : gridMat;
            float thick = (dz == 0) ? settings.getCylinderCenterLineThickness() : settings.getCylinderGridThickness();

            double ratio = dz / radiusZ;
            if (Math.abs(ratio) > 1.0) continue;
            double offsetX = radiusX * Math.sin(Math.acos(ratio));
            double x1 = centerX - offsetX, x2 = centerX + offsetX;

            renderLines(mat, thick,
                    new Line(new Vector3f((float)x1, (float)minY, (float)z),
                            new Vector3f((float)x1, (float)maxY, (float)z)),
                    new Line(new Vector3f((float)x2, (float)minY, (float)z),
                            new Vector3f((float)x2, (float)maxY, (float)z))
            );
        }
    }

    private int calculateXGridStep(double radiusX) {
        int step = Math.max(1, (int)(radiusX / settings.getCylinderRadiusGridDivision()));
        if (settings.getCylinderMaxGridSpacing() != -1) step = Math.min(step, settings.getCylinderMaxGridSpacing());
        return step;
    }

    private int calculateZGridStep(double radiusZ) {
        int step = Math.max(1, (int)(radiusZ / settings.getCylinderRadiusGridDivision()));
        if (settings.getCylinderMaxGridSpacing() != -1) step = Math.min(step, settings.getCylinderMaxGridSpacing());
        return step;
    }

    @Override
    public Class<CylinderRegion> getRegionType() {
        return CylinderRegion.class;
    }
}
