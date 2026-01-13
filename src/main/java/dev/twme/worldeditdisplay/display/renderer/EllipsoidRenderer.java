package dev.twme.worldeditdisplay.display.renderer;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.EllipsoidRegion;
import dev.twme.worldeditdisplay.region.Vector3;

import org.joml.Vector3f;

public class EllipsoidRenderer extends RegionRenderer<EllipsoidRegion> {

    private static final double TAU = Math.PI * 2.0;

    public EllipsoidRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }

    @Override
    public void render(EllipsoidRegion region) {
        clear();

        if (!region.isDefined()) return;

        Vector3 center = region.getCenter();
        Vector3 radii = region.getRadii();

        Vector3f centerPos = new Vector3f(
                (float) center.getX() + 0.5f,
                (float) center.getY() + 0.5f,
                (float) center.getZ() + 0.5f
        );

        boolean multi = isMultiSelection(region);

        Material lineMat = getMaterialWithOverride(region, 0, settings.getEllipsoidLineMaterial(), multi);
        Material centerMat = getMaterialWithOverride(region, 2, settings.getEllipsoidCenterMaterial(), multi);
        Material centerLineMat = settings.getEllipsoidCenterLineMaterial();

        renderCube(centerPos, settings.getEllipsoidCenterMarkerSize(), centerMat, settings.getEllipsoidCenterThickness());

        int xStep = calculateGridStep(radii.getX());
        int yStep = calculateGridStep(radii.getY());
        int zStep = calculateGridStep(radii.getZ());

        renderXZPlane(centerPos, radii, yStep, lineMat, centerLineMat);
        renderYZPlane(centerPos, radii, xStep, lineMat, centerLineMat);
        renderXYPlane(centerPos, radii, zStep, lineMat, centerLineMat);
    }

    private int calculateGridStep(double radius) {
        int step = Math.max(1, (int) (radius / settings.getEllipsoidRadiusGridDivision()));
        if (settings.getEllipsoidMaxGridSpacing() != -1) step = Math.min(step, settings.getEllipsoidMaxGridSpacing());
        return step;
    }

    private int calculateEllipseSegments(double r1, double r2) {
        double a = Math.max(r1, r2);
        double b = Math.min(r1, r2);
        double h = Math.pow((a - b) / (a + b), 2);
        double circumference = Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h)));

        int segByLength = (int) Math.ceil(circumference / settings.getEllipsoidTargetSegmentLength());
        int segByRadius = (int) (settings.getEllipsoidMinSegments() + settings.getEllipsoidSqrtScaleFactor() * Math.sqrt((r1 + r2) / 2));

        int seg = Math.max(segByLength, segByRadius);
        return Math.max(settings.getEllipsoidMinSegments(), Math.min(seg, settings.getEllipsoidMaxSegments()));
    }

    private void renderXZPlane(Vector3f center, Vector3 radii, int step, Material mat, Material centerLine) {
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

    private void drawEllipseXZ(Vector3f center, float rx, float ry, float rz, int yOffset, Material mat, float thickness) {
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

    private void renderYZPlane(Vector3f center, Vector3 radii, int step, Material mat, Material centerLine) {
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

    private void drawEllipseYZ(Vector3f center, float rx, float ry, float rz, int xOffset, Material mat, float thickness) {
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

    private void renderXYPlane(Vector3f center, Vector3 radii, int step, Material mat, Material centerLine) {
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

    private void drawEllipseXY(Vector3f center, float rx, float ry, float rz, int zOffset, Material mat, float thickness) {
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
}