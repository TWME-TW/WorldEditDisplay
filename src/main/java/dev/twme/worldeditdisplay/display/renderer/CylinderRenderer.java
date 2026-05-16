package dev.twme.worldeditdisplay.display.renderer;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.CylinderRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * Renders cylinder-shaped selections.
 * Shows circles at each Y layer, vertical grid lines, and a center cube.
 */
public class CylinderRenderer extends RegionRenderer<CylinderRegion> {

    public CylinderRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    protected boolean isSeeThrough() {
        return settings.isCylinderSeeThrough();
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

        Color circleMat = getColorWithOverride(region, 0, settings.getCylinderCircleColor(), isMulti);
        Color gridMat = getColorWithOverride(region, 1, settings.getCylinderGridColor(), isMulti);
        Color centerMat = getColorWithOverride(region, 2, settings.getCylinderCenterColor(), isMulti);
        Color centerLineMat = settings.getCylinderCenterLineColor();

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
        int circleSegments = calculateCircleSegments(radiusX, radiusZ);

        // Render circles for each layer
        for (int y = minY; y <= maxY + 1; y += stepY) {
            if (y == center.getY() || y == center.getY() + 1) continue;
            renderCircle(cxCircle, y, czCircle, radiusX, radiusZ, circleSegments, circleMat, settings.getCylinderCircleThickness());
        }

        // Ensure top layer is rendered
        if ((maxY + 1 - minY) % stepY != 0) {
            renderCircle(cxCircle, maxY + 1, czCircle, radiusX, radiusZ, circleSegments, circleMat, settings.getCylinderCircleThickness());
        }

        // Render center circle lines
        renderCircle(cxCircle, center.getY(), czCircle, radiusX, radiusZ, circleSegments, centerLineMat, settings.getCylinderCenterLineThickness());
        if (center.getY() + 1 != center.getY()) {
            renderCircle(cxCircle, center.getY() + 1, czCircle, radiusX, radiusZ, circleSegments, centerLineMat, settings.getCylinderCenterLineThickness());
        }

        // Render vertical grid lines
        renderGrid(cxCircle, czCircle, radiusX, radiusZ, minY, maxY + 1, gridMat, centerLineMat);

        // Render top/bottom cap grid lines
        renderCapGrid(cxCircle, minY, czCircle, radiusX, radiusZ, gridMat, centerLineMat);
        renderCapGrid(cxCircle, maxY + 1, czCircle, radiusX, radiusZ, gridMat, centerLineMat);

        // Render center cube
        renderCube(new Vector3f((float)(center.getX() + 0.5),
                        (float)(center.getY() + 0.5),
                        (float)(center.getZ() + 0.5)),
                1.03f, centerMat, settings.getCylinderCenterThickness());

        // Render fill surface using parallelograms
        if (settings.isCylinderFillEnabled()) {
            Color fillMat = getFillColorWithOverride(region, 3, settings.getCylinderFillColor(), isMulti);
            renderCylinderBand(cxCircle, czCircle, radiusX, radiusZ,
                    (float) minY, (float) (maxY + 1), fillMat);
            renderCircleCap(cxCircle, minY, czCircle, radiusX, radiusZ, fillMat);
            renderCircleCap(cxCircle, maxY + 1, czCircle, radiusX, radiusZ, fillMat);
        }
    }

    private void renderRectangularGrid(double centerX, double centerZ,
                                       double radiusX, double radiusZ,
                                       int minY, int maxY,
                                       Color gridMat, Color centerLineMat) {
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
                Color mat = (dz == 0) ? centerLineMat : gridMat;
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
                Color mat = (dx == 0) ? centerLineMat : gridMat;
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
                              double radiusX, double radiusZ, int segments,
                              Color mat, float thickness) {
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
                            Color gridMat, Color centerLineMat) {
        int posX = (int)Math.ceil(radiusX), negX = (int)-Math.ceil(radiusX);
        int posZ = (int)Math.ceil(radiusZ), negZ = (int)-Math.ceil(radiusZ);

        int xStep = calculateXGridStep(radiusX);
        int zStep = calculateZGridStep(radiusZ);

        for (int dx = negX; dx <= posX; dx += xStep) {
            double x = centerX + dx;
            Color mat = (dx == 0) ? centerLineMat : gridMat;
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
            Color mat = (dz == 0) ? centerLineMat : gridMat;
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

    /**
     * Renders horizontal grid lines across the top or bottom cap of the cylinder.
     * For each X grid line, draws a chord across the ellipse at that X offset.
     * For each Z grid line, draws a chord across the ellipse at that Z offset.
     */
    private void renderCapGrid(double centerX, double y, double centerZ,
                               double radiusX, double radiusZ,
                               Color gridMat, Color centerLineMat) {
        int posX = (int) Math.ceil(radiusX), negX = (int) -Math.ceil(radiusX);
        int posZ = (int) Math.ceil(radiusZ), negZ = (int) -Math.ceil(radiusZ);

        int xStep = calculateXGridStep(radiusX);
        int zStep = calculateZGridStep(radiusZ);

        for (int dx = negX; dx <= posX; dx += xStep) {
            double ratio = dx / radiusX;
            if (Math.abs(ratio) > 1.0) continue;
            double offsetZ = radiusZ * Math.cos(Math.asin(ratio));
            double x = centerX + dx;
            double z1 = centerZ - offsetZ;
            double z2 = centerZ + offsetZ;
            Color mat = (dx == 0) ? centerLineMat : gridMat;
            float thick = (dx == 0) ? settings.getCylinderCenterLineThickness() : settings.getCylinderGridThickness();
            renderLine(new Line(new Vector3f((float) x, (float) y, (float) z1),
                    new Vector3f((float) x, (float) y, (float) z2)), mat, thick);
        }

        for (int dz = negZ; dz <= posZ; dz += zStep) {
            double ratio = dz / radiusZ;
            if (Math.abs(ratio) > 1.0) continue;
            double offsetX = radiusX * Math.sin(Math.acos(ratio));
            double z = centerZ + dz;
            double x1 = centerX - offsetX;
            double x2 = centerX + offsetX;
            Color mat = (dz == 0) ? centerLineMat : gridMat;
            float thick = (dz == 0) ? settings.getCylinderCenterLineThickness() : settings.getCylinderGridThickness();
            renderLine(new Line(new Vector3f((float) x1, (float) y, (float) z),
                    new Vector3f((float) x2, (float) y, (float) z)), mat, thick);
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

    /**
     * Renders the lateral surface of a cylindrical band between two Y levels
     * using N parallelogram quads around the perimeter.
     * Each quad exactly covers one arc segment of the cylinder surface.
     */
    private void renderCylinderBand(double cx, double cz,
                                    double radiusX, double radiusZ,
                                    float y1, float y2, Color mat) {
        int segments = calculateCircleSegments(radiusX, radiusZ);
        double twoPi = Math.PI * 2;
        for (int i = 0; i < segments; i++) {
            double a1 = i * twoPi / segments;
            double a2 = (i + 1) * twoPi / segments;
            Vector3f p1 = new Vector3f(
                    (float)(cx + radiusX * Math.cos(a1)), y1,
                    (float)(cz + radiusZ * Math.sin(a1)));
            Vector3f p2 = new Vector3f(
                    (float)(cx + radiusX * Math.cos(a2)), y1,
                    (float)(cz + radiusZ * Math.sin(a2)));
            Vector3f p3 = new Vector3f(
                    (float)(cx + radiusX * Math.cos(a1)), y2,
                    (float)(cz + radiusZ * Math.sin(a1)));
            renderParallelogram(p1, p2, p3, mat);
        }
    }

    /**
        * Special thanks to Sexual Umut Şahin.
        *
     * Renders a filled disc cap (top or bottom) as a 2D zonogon:
     * m = segments/2 generator vectors (consecutive arc chords on the upper semicircle)
     * are used; each pair (i,j) produces one parallelogram tile so that together
     * they cover the ellipse without any overlap or gap.
     *
     * <p>The generators are arc-chord vectors of the circle, so their Minkowski sum
     * exactly spans the full ellipse boundary. The base corner for pair (i,j) is the
     * sum of all generators strictly between i and j, shifted from the rightmost
     * point of the ellipse.</p>
     */
    private void renderCircleCap(double cx, double cy, double cz,
                                  double radiusX, double radiusZ, Color mat) {

        int segments = calculateCircleSegments(radiusX, radiusZ);
        // The zonogon-based cap fill assumes an even segment count so that
        // m = segments / 2 generators cover one semicircle exactly. If the
        // computed value is odd, bump it to the next even number to avoid
        // dropping a generator and leaving gaps in the cap.
        if ((segments & 1) != 0) {
            segments++;
        }
        int m = segments / 2;

        Vector3f[] V = new Vector3f[m];
        double twoPi = Math.PI * 2;

        for (int i = 0; i < m; i++) {
            double a1 = i * twoPi / segments;
            double a2 = (i + 1) * twoPi / segments;
            float vx = (float) (radiusX * Math.cos(a2) - radiusX * Math.cos(a1));
            float vz = (float) (radiusZ * Math.sin(a2) - radiusZ * Math.sin(a1));
            V[i] = new Vector3f(vx, 0, vz);
        }

        float y = (float) cy;
        Vector3f originOffset = new Vector3f((float)(cx + radiusX), y, (float)cz);

        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {

                Vector3f base = new Vector3f(originOffset);
                for (int k = i + 1; k < j; k++) {
                    base.add(V[k]);
                }

                Vector3f p1 = new Vector3f(base);
                Vector3f p2 = new Vector3f(base).add(V[i]);
                Vector3f p3 = new Vector3f(base).add(V[j]);

                renderParallelogram(p1, p2, p3, mat);
            }
        }
    }

    @Override
    public Class<CylinderRegion> getRegionType() {
        return CylinderRegion.class;
    }
}
