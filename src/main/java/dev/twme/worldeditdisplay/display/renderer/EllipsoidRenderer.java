package dev.twme.worldeditdisplay.display.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.EllipsoidRegion;
import dev.twme.worldeditdisplay.region.Vector3;

public class EllipsoidRenderer extends RegionRenderer<EllipsoidRegion> {

    private static final double TAU = Math.PI * 2.0;

    /**
     * Cache for calculateEllipseSegments within a single render() call.
     * Key encodes (r1*100 as int) in upper 32 bits and (r2*100 as int) in lower 32 bits.
     * Cleared at the start of each render pass.
     */
    private final HashMap<Long, Integer> segmentCache = new HashMap<>();

    public EllipsoidRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    protected boolean isSeeThrough() {
        return settings.isEllipsoidSeeThrough();
    }

    @Override
    public void render(EllipsoidRegion region) {
        clear();
        segmentCache.clear();

        if (!region.isDefined()) return;

        Vector3 center = region.getCenter();
        Vector3 radii = region.getRadii();

        Vector3f centerPos = new Vector3f(
                (float) center.getX() + 0.5f,
                (float) center.getY() + 0.5f,
                (float) center.getZ() + 0.5f
        );

        boolean multi = isMultiSelection(region);

        Color lineMat = getColorWithOverride(region, 0, settings.getEllipsoidLineColor(), multi);
        Color centerMat = getColorWithOverride(region, 2, settings.getEllipsoidCenterColor(), multi);
        Color centerLineMat = settings.getEllipsoidCenterLineColor();

        renderCube(centerPos, settings.getEllipsoidCenterMarkerSize(), centerMat, settings.getEllipsoidCenterThickness());

        int xStep = calculateGridStep(radii.getX());
        int yStep = calculateGridStep(radii.getY());
        int zStep = calculateGridStep(radii.getZ());

        renderXZPlane(centerPos, radii, yStep, lineMat, centerLineMat);
        renderYZPlane(centerPos, radii, xStep, lineMat, centerLineMat);
        renderXYPlane(centerPos, radii, zStep, lineMat, centerLineMat);

        // Render fill surface using parallelogram latitude bands
        if (settings.isEllipsoidFillEnabled()) {
            Color fillMat = getFillColorWithOverride(region, 3, settings.getEllipsoidFillColor(), multi);
            renderEllipsoidFill(centerPos, radii, fillMat);
        }
    }

    private int calculateGridStep(double radius) {
        int step = Math.max(1, (int) Math.ceil(2.0 * radius / settings.getEllipsoidRadiusGridDivision()));
        if (settings.getEllipsoidMaxGridSpacing() != -1) step = Math.min(step, settings.getEllipsoidMaxGridSpacing());
        return step;
    }

    private int calculateEllipseSegments(double r1, double r2) {
        // Quantize to 2 decimal places so nearly-identical rings share a cache entry
        long key = ((long) (int) (r1 * 100) << 32) | ((int) (r2 * 100) & 0xFFFFFFFFL);
        Integer cached = segmentCache.get(key);
        if (cached != null) return cached;

        double a = Math.max(r1, r2);
        double b = Math.min(r1, r2);
        double h = Math.pow((a - b) / (a + b), 2);
        double circumference = Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h)));

        int segByLength = (int) Math.ceil(circumference / settings.getEllipsoidTargetSegmentLength());
        int segByRadius = (int) (settings.getEllipsoidMinSegments() + settings.getEllipsoidSqrtScaleFactor() * Math.sqrt((r1 + r2) / 2));

        int seg = Math.max(segByLength, segByRadius);
        int result = Math.max(settings.getEllipsoidMinSegments(), Math.min(seg, settings.getEllipsoidMaxSegments()));
        segmentCache.put(key, result);
        return result;
    }

    private void renderXZPlane(Vector3f center, Vector3 radii, int step, Color mat, Color centerLine) {
        float rx = (float) radii.getX();
        float ry = (float) radii.getY();
        float rz = (float) radii.getZ();

        if (ry < 0.5) {
            drawEllipseXZ(center, rx, ry, rz, 0, centerLine, settings.getEllipsoidCenterLineThickness());
            return;
        }

        int yRad = (int) Math.floor(ry);
        for (int yOffset = -yRad; yOffset < yRad; yOffset += step) {
            if (yOffset == 0) continue;
            drawEllipseXZ(center, rx, ry, rz, yOffset, mat, settings.getEllipsoidLineThickness());
        }
        drawEllipseXZ(center, rx, ry, rz, 0, centerLine, settings.getEllipsoidCenterLineThickness());
    }

    private void drawEllipseXZ(Vector3f center, float rx, float ry, float rz, int yOffset, Color mat, float thickness) {
        double scale = (ry < 0.01) ? 1 : Math.sqrt(1 - Math.pow(yOffset / ry, 2));
        double scaledRx = rx * scale, scaledRz = rz * scale;
        int segments = calculateEllipseSegments(scaledRx, scaledRz);

        for (int i = 0; i < segments; i++) {
            double t1 = i * TAU / segments, t2 = (i + 1) * TAU / segments;
            Vector3f p1 = new Vector3f(center.x + (float)(rx * Math.cos(t1) * scale), center.y + yOffset, center.z + (float)(rz * Math.sin(t1) * scale));
            Vector3f p2 = new Vector3f(center.x + (float)(rx * Math.cos(t2) * scale), center.y + yOffset, center.z + (float)(rz * Math.sin(t2) * scale));
            renderLine(new Line(p1, p2), mat, thickness);
        }
    }

    private void renderYZPlane(Vector3f center, Vector3 radii, int step, Color mat, Color centerLine) {
        float rx = (float) radii.getX();
        float ry = (float) radii.getY();
        float rz = (float) radii.getZ();

        if (rx < 0.5) {
            drawEllipseYZ(center, rx, ry, rz, 0, centerLine, settings.getEllipsoidCenterLineThickness());
            return;
        }

        int xRad = (int) Math.floor(rx);
        for (int xOffset = -xRad; xOffset < xRad; xOffset += step) {
            if (xOffset == 0) continue;
            drawEllipseYZ(center, rx, ry, rz, xOffset, mat, settings.getEllipsoidLineThickness());
        }
        drawEllipseYZ(center, rx, ry, rz, 0, centerLine, settings.getEllipsoidCenterLineThickness());
    }

    private void drawEllipseYZ(Vector3f center, float rx, float ry, float rz, int xOffset, Color mat, float thickness) {
        double scale = (rx < 0.01) ? 1 : Math.sqrt(1 - Math.pow(xOffset / rx, 2));
        double sy = ry * scale, sz = rz * scale;
        int segments = calculateEllipseSegments(sy, sz);

        for (int i = 0; i < segments; i++) {
            double t1 = i * TAU / segments, t2 = (i + 1) * TAU / segments;
            Vector3f p1 = new Vector3f(center.x + xOffset, center.y + (float)(ry * Math.cos(t1) * scale), center.z + (float)(rz * Math.sin(t1) * scale));
            Vector3f p2 = new Vector3f(center.x + xOffset, center.y + (float)(ry * Math.cos(t2) * scale), center.z + (float)(rz * Math.sin(t2) * scale));
            renderLine(new Line(p1, p2), mat, thickness);
        }
    }

    private void renderXYPlane(Vector3f center, Vector3 radii, int step, Color mat, Color centerLine) {
        float rx = (float) radii.getX();
        float ry = (float) radii.getY();
        float rz = (float) radii.getZ();

        if (rz < 0.5) {
            drawEllipseXY(center, rx, ry, rz, 0, centerLine, settings.getEllipsoidCenterLineThickness());
            return;
        }

        int zRad = (int) Math.floor(rz);
        for (int zOffset = -zRad; zOffset < zRad; zOffset += step) {
            if (zOffset == 0) continue;
            drawEllipseXY(center, rx, ry, rz, zOffset, mat, settings.getEllipsoidLineThickness());
        }
        drawEllipseXY(center, rx, ry, rz, 0, centerLine, settings.getEllipsoidCenterLineThickness());
    }

    private void drawEllipseXY(Vector3f center, float rx, float ry, float rz, int zOffset, Color mat, float thickness) {
        double scale = (rz < 0.01) ? 1 : Math.sqrt(1 - Math.pow(zOffset / rz, 2));
        double sx = rx * scale, sy = ry * scale;
        int segments = calculateEllipseSegments(sx, sy);

        for (int i = 0; i < segments; i++) {
            double t1 = i * TAU / segments, t2 = (i + 1) * TAU / segments;
            Vector3f p1 = new Vector3f(center.x + (float)(rx * Math.cos(t1) * scale), center.y + (float)(ry * Math.sin(t1) * scale), center.z + zOffset);
            Vector3f p2 = new Vector3f(center.x + (float)(rx * Math.cos(t2) * scale), center.y + (float)(ry * Math.sin(t2) * scale), center.z + zOffset);
            renderLine(new Line(p1, p2), mat, thickness);
        }
    }

    @Override
    public Class<EllipsoidRegion> getRegionType() {
        return EllipsoidRegion.class;
    }

    /**
     * Renders the ellipsoid surface as a zonohedron (菱形多面體).
     * Uses Fibonacci lattice to generate n uniformly-distributed generator vectors on the hemisphere.
     * Each pair of generators defines a parallelogram face; the ellipsoid scaling is applied via affine transform.
     * Face count = n*(n-1), where n = fill_generators setting.
     */
    private void renderEllipsoidFill(Vector3f centerPos, Vector3 radii, Color mat) {
        float rx = (float) radii.getX();
        float ry = (float) radii.getY();
        float rz = (float) radii.getZ();

        int n = settings.getEllipsoidFillGenerators();
        if (n < 3) n = 3;

        // 1. Generate n uniformly-distributed vectors on the upper hemisphere using Fibonacci lattice
        double phi = (1.0 + Math.sqrt(5.0)) / 2.0;
        List<double[]> generators = new ArrayList<>(n);
        double sumX = 0, sumY = 0, sumZ = 0;

        for (int i = 0; i < n; i++) {
            double gy = (double) i / (n - 0.5);
            double radius = Math.sqrt(1.0 - gy * gy);
            double theta = TAU * i / phi;

            double gx = Math.cos(theta) * radius;
            double gz = Math.sin(theta) * radius;

            generators.add(new double[]{gx, gy, gz});
            sumX += gx;
            sumY += gy;
            sumZ += gz;
        }

        double halfSumX = sumX / 2.0;
        double halfSumY = sumY / 2.0;
        double halfSumZ = sumZ / 2.0;

        // 2. First pass: compute all face data and find the average face-center distance
        //    for normalization. Using face-center distance (midsphere radius) ensures the
        //    faces sit on the ellipsoid surface rather than being inscribed too small.
        List<double[]> faceData = new ArrayList<>();
        double sumFaceDist = 0;
        int faceCount = 0;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double[] vi = generators.get(i);
                double[] vj = generators.get(j);

                double nx = vi[1] * vj[2] - vi[2] * vj[1];
                double ny = vi[2] * vj[0] - vi[0] * vj[2];
                double nz = vi[0] * vj[1] - vi[1] * vj[0];

                if (nx * nx + ny * ny + nz * nz < 1e-12) continue;

                double bx = 0, by = 0, bz = 0;
                for (int k = 0; k < n; k++) {
                    if (k == i || k == j) continue;
                    double[] vk = generators.get(k);
                    if (vk[0] * nx + vk[1] * ny + vk[2] * nz > 1e-6) {
                        bx += vk[0];
                        by += vk[1];
                        bz += vk[2];
                    }
                }

                faceData.add(new double[]{bx, by, bz, i, j});

                // Face center = base + (vi + vj) / 2, relative to zonohedron center
                double fcx = bx + (vi[0] + vj[0]) / 2.0 - halfSumX;
                double fcy = by + (vi[1] + vj[1]) / 2.0 - halfSumY;
                double fcz = bz + (vi[2] + vj[2]) / 2.0 - halfSumZ;
                sumFaceDist += Math.sqrt(fcx * fcx + fcy * fcy + fcz * fcz);
                faceCount++;
            }
        }

        if (faceCount == 0) return;
        double avgFaceRadius = sumFaceDist / faceCount;
        if (avgFaceRadius < 1e-10) return;
        double normScale = 1.0 / avgFaceRadius;

        // 3. Second pass: render all faces with normalized coordinates
        for (double[] face : faceData) {
            double bx = face[0], by = face[1], bz = face[2];
            double[] vi = generators.get((int) face[3]);
            double[] vj = generators.get((int) face[4]);

            // Compute 3 key corners for the parallelogram (p1=base, p2=base+vi, p3=base+vj)
            Vector3f p1 = zonoToEllipsoid(centerPos, rx, ry, rz, bx, by, bz, halfSumX, halfSumY, halfSumZ, normScale);
            Vector3f p2 = zonoToEllipsoid(centerPos, rx, ry, rz, bx + vi[0], by + vi[1], bz + vi[2], halfSumX, halfSumY, halfSumZ, normScale);
            Vector3f p3 = zonoToEllipsoid(centerPos, rx, ry, rz, bx + vj[0], by + vj[1], bz + vj[2], halfSumX, halfSumY, halfSumZ, normScale);
            renderParallelogram(p1, p2, p3, mat);

            // Opposite face (central symmetry: sum - corner)
            Vector3f op1 = zonoToEllipsoid(centerPos, rx, ry, rz, sumX - bx, sumY - by, sumZ - bz, halfSumX, halfSumY, halfSumZ, normScale);
            Vector3f op2 = zonoToEllipsoid(centerPos, rx, ry, rz, sumX - bx - vi[0], sumY - by - vi[1], sumZ - bz - vi[2], halfSumX, halfSumY, halfSumZ, normScale);
            Vector3f op3 = zonoToEllipsoid(centerPos, rx, ry, rz, sumX - bx - vj[0], sumY - by - vj[1], sumZ - bz - vj[2], halfSumX, halfSumY, halfSumZ, normScale);
            renderParallelogram(op1, op2, op3, mat);
        }
    }

    private Vector3f zonoToEllipsoid(Vector3f center, float rx, float ry, float rz,
                                      double vx, double vy, double vz,
                                      double halfSumX, double halfSumY, double halfSumZ,
                                      double normScale) {
        return new Vector3f(
                center.x + (float) ((vx - halfSumX) * normScale * rx),
                center.y + (float) ((vy - halfSumY) * normScale * ry),
                center.z + (float) ((vz - halfSumZ) * normScale * rz)
        );
    }
}