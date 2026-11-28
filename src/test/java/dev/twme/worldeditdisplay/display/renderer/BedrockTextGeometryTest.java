package dev.twme.worldeditdisplay.display.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.Axis;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.twme.worldeditdisplay.display.renderer.BedrockTextGeometry.Stroke;

class BedrockTextGeometryTest {
    @Test
    void rectangularFaceKeepsUnequalSidesAndOppositeNormals() {
        var panels = BedrockTextGeometry.face(
                new Vector3f(2, 72, 4), new Vector3f(12, 72, 4), new Vector3f(2, 77, 4), false);

        assertEquals(10, panels.size());
        for (var panel : panels) {
            assertEquals(7, panel.position().x, 0.001);
            assertEquals(4, panel.position().z, 0.001);
            assertEquals(1, panel.text().lines().count());
            assertEquals(10, estimatedWidth(panel), 0.001);
            assertEquals(11, panel.text().length());
            assertTrue(panel.text().codePoints().allMatch(c -> c == '\u00a0'));
        }
        assertEquals(72, panels.get(0).position().y, 0.001);
        assertEquals(77, panels.get(4).position().y + estimatedHeight(panels.get(4)), 0.001);
        for (int i = 1; i < 5; i++) {
            assertEquals(panels.get(i - 1).position().y + estimatedHeight(panels.get(i - 1)),
                    panels.get(i).position().y, 0.001);
        }
        Vector3f front = panels.get(0).rotation().transform(new Vector3f(0, 0, 1));
        Vector3f back = panels.get(5).rotation().transform(new Vector3f(0, 0, 1));
        assertEquals(-1, front.dot(back), 0.001);
    }

    @Test
    void sixByThreeFaceReachesTheWireframe() {
        var panels = BedrockTextGeometry.face(
                new Vector3f(-89, -60, -70), new Vector3f(-89, -60, -64),
                new Vector3f(-89, -57, -70), false);

        int lastFrontRow = panels.size() / 2 - 1;
        var bottom = panels.get(0);
        var top = panels.get(lastFrontRow);
        assertEquals(-60, bottom.position().y, 0.001);
        assertEquals(-57, top.position().y + estimatedHeight(top), 0.001);
        assertEquals(-70, bottom.position().z - estimatedWidth(bottom) / 2, 0.001);
        assertEquals(-64, bottom.position().z + estimatedWidth(bottom) / 2, 0.001);
    }

    @Test
    void horizontalFacesFillTheXSpanWhileReachingBothZEdges() {
        for (int[] size : new int[][] {{10, 9}, {9, 16}, {1, 1}}) {
            for (float y : new float[] {-60, -54}) {
                Vector3f min = new Vector3f(-99, y, -76);
                Vector3f max = new Vector3f(min).add(size[0], 0, size[1]);
                var panels = BedrockTextGeometry.face(min, new Vector3f(max.x, y, min.z),
                        new Vector3f(min.x, y, max.z), false);

                int half = panels.size() / 2;
                assertTrue(half > 0 && half <= 128);
                for (int side = 0; side < 2; side++) {
                    Bounds covered = coveredBounds(panels.subList(side * half, (side + 1) * half));
                    Vector3f coveredMin = covered.min();
                    Vector3f coveredMax = covered.max();
                    assertTrue(coveredMin.x >= min.x - 0.001 && coveredMin.x <= min.x + 0.016);
                    assertTrue(coveredMax.x <= max.x + 0.001 && coveredMax.x >= max.x - 0.016);
                    assertEquals(y, coveredMin.y, 0.001);
                    assertEquals(y, coveredMax.y, 0.001);
                    assertEquals(min.z, coveredMin.z, 0.001);
                    assertEquals(max.z, coveredMax.z, 0.001);
                }
            }
        }
    }

    @Test
    void horizontalCuboidFillsInsetOnlyTheXEdges() {
        for (int[] size : new int[][] {{10, 9}, {9, 16}, {1, 1}, {6, 4}}) {
            for (float y : new float[] {-60, -54}) {
                Vector3f start = new Vector3f(-120, y, -51);
                Vector3f xEnd = new Vector3f(start).add(size[0], 0, 0);
                Vector3f zEnd = new Vector3f(start).add(0, 0, size[1]);
                Bounds original = coveredBounds(BedrockTextGeometry.face(start, xEnd, zEnd, false));
                var panels = BedrockTextGeometry.cuboidFace(start, xEnd, zEnd);

                int half = panels.size() / 2;
                assertTrue(half > 0 && half <= 128);
                for (int side = 0; side < 2; side++) {
                    Bounds covered = coveredBounds(panels.subList(side * half, (side + 1) * half));
                    float xMargin = covered.min().x - start.x;
                    assertTrue(xMargin >= 0.026f && xMargin <= 0.043f, "X margin was " + xMargin);
                    assertEquals(xMargin, xEnd.x - covered.max().x, 0.001);
                    assertEquals(original.min().z, covered.min().z, 0.001);
                    assertEquals(original.max().z, covered.max().z, 0.001);
                    assertEquals(y, covered.min().y, 0.001);
                    assertEquals(y, covered.max().y, 0.001);
                }
            }
        }
    }

    @Test
    void verticalCuboidFillsReserveRoomForTheClientOverdrawOnBothEdges() {
        for (Axis axis : List.of(Axis.X, Axis.Z)) {
            int across = axis.ordinal();
            int fixed = axis == Axis.X ? Axis.Z.ordinal() : Axis.X.ordinal();
            float inset = axis == Axis.X ? 0.008f : 0.045f;
            Vector3f start = new Vector3f(-120, -60, -51);
            Vector3f end = new Vector3f(start).setComponent(across, start.get(across) + 6);
            var panels = BedrockTextGeometry.cuboidFace(start, end, new Vector3f(start).add(0, 3, 0));

            int half = panels.size() / 2;
            assertTrue(half > 0);
            for (int side = 0; side < 2; side++) {
                Bounds covered = coveredBounds(panels.subList(side * half, (side + 1) * half));
                float startMargin = covered.min().get(across) - start.get(across);
                float endMargin = end.get(across) - covered.max().get(across);
                assertTrue(startMargin >= inset - 0.001f && startMargin <= inset + 0.01f);
                assertEquals(startMargin, endMargin, 0.001);
                assertEquals(start.get(fixed), covered.min().get(fixed), 0.001);
                assertEquals(start.get(fixed), covered.max().get(fixed), 0.001);
                assertEquals(-60, covered.min().y, 0.01);
                assertEquals(-57, covered.max().y, 0.01);
            }
        }
    }

    @Test
    void lineKeepsItsThicknessAcrossSeveralTextBoxes() {
        var panels = BedrockTextGeometry.line(new Vector3f(0, 0, 0), new Vector3f(40, 0, 0), 0.06f);

        assertTrue(panels.size() > 2);
        for (var panel : panels) assertEquals(0.06, estimatedHeight(panel), 0.001);
        assertContinuousLine(panels, 40);
    }

    @Test
    void thinGridLineHasNoGapsOrOvershoot() {
        var panels = BedrockTextGeometry.line(new Vector3f(0, 0, 0), new Vector3f(6, 0, 0), 0.02f);

        assertTrue(panels.size() > 2);
        for (var panel : panels) assertEquals(0.02, estimatedHeight(panel), 0.0005);
        assertContinuousLine(panels, 6);
    }

    @Test
    void shortEdgeEndsAtTheCornerInBothDirections() {
        var panels = BedrockTextGeometry.line(new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), 0.06f);

        assertEquals(2, panels.size());
        assertContinuousLine(panels, 1);
    }

    @Test
    void boxEdgeStrokesStartAtTheBoundaryAndExtendIntoBothFaces() {
        Vector3f start = new Vector3f(0, -60, -56);
        Vector3f end = new Vector3f(6, -60, -56);
        for (Axis axis : List.of(Axis.Y, Axis.Z)) {
            for (int inward : new int[] { -1, 1 }) {
                var panels = BedrockTextGeometry.line(start, end, 0.06f, new Stroke(axis, inward));
                assertContinuousLine(panels, 6);
                Vector3f expectedUp = new Vector3f().setComponent(axis.ordinal(), inward);
                for (var panel : panels) {
                    assertEquals(start.y, panel.position().y, 0.0001);
                    assertEquals(start.z, panel.position().z, 0.0001);
                    assertTrue(expectedUp.equals(panel.rotation().transform(new Vector3f(0, 1, 0)), 0.0001f));
                }
            }
        }
    }

    @Test
    void verticalGridLineOnXFaceStaysInTheFacePlane() {
        Vector3f start = new Vector3f(-78, -60, -56);
        Vector3f end = new Vector3f(-78, -54, -56);
        var panels = BedrockTextGeometry.line(start, end, 0.02f, new Stroke(Axis.Z, 0));

        for (var panel : panels) {
            assertEquals(start.x, panel.position().x, 0.0001);
            assertEquals(start.z, panel.position().z + estimatedHeight(panel) / 2, 0.0001);
            assertTrue(new Vector3f(0, 0, 1).equals(panel.rotation().transform(new Vector3f(0, 1, 0)), 0.0001f));
        }
    }

    @Test
    void shortInwardStrokeKeepsItsBandOnTheInside() {
        Vector3f start = new Vector3f(0, 10, 0);
        Vector3f end = new Vector3f(0.04f, 10, 0);
        var panels = BedrockTextGeometry.line(start, end, 0.06f, new Stroke(Axis.Y, -1));

        assertEquals(2, panels.size());
        assertContinuousLine(panels, 0.04f);
        for (var panel : panels) {
            assertEquals(10, panel.position().y, 0.0001);
            assertTrue(new Vector3f(0, -1, 0).equals(panel.rotation().transform(new Vector3f(0, 1, 0)), 0.0001f));
        }
    }

    private static void assertContinuousLine(List<BedrockTextGeometry.Panel> panels, float length) {
        int columns = panels.size() / 2;
        for (int side = 0; side < 2; side++) {
            float direction = side == 0 ? 1 : -1;
            float start = side == 0 ? 0 : length;
            float previousNarrowEnd = 0;
            for (int column = 0; column < columns; column++) {
                var panel = panels.get(side * columns + column);
                float center = (panel.position().x - start) * direction;
                float narrowHalfWidth = (panel.text().length() * 8.64f + 1) * 0.025f * panel.scale() / 2;
                if (column == 0) {
                    assertEquals(0, center - estimatedWidth(panel) / 2, 0.0001);
                } else {
                    assertEquals(0.025f * panel.scale(), previousNarrowEnd - (center - narrowHalfWidth), 0.0001);
                }
                if (column == columns - 1) {
                    assertEquals(length, center + estimatedWidth(panel) / 2, 0.0001);
                }
                previousNarrowEnd = center + narrowHalfWidth;
            }
        }
    }

    @Test
    void worldSpaceRotationKeepsVerticalFacesAtBothQuarterTurns() {
        for (int direction : new int[] { -1, 1 }) {
            for (float roll : new float[] { 0, 0.5f, (float) Math.PI }) {
                var matrix = new Matrix3f().rotationZYX(roll, direction * (float) Math.PI / 2, 0.35f);
                var angles = BedrockTextShape.bedrockRotation(matrix);
                var converted = new Matrix3f().rotationZYX(
                        (float) Math.toRadians(angles.z), (float) Math.toRadians(angles.y),
                        (float) Math.toRadians(angles.x));
                assertTrue(matrix.equals(converted, 0.00001f));
            }
        }
    }

    private static float estimatedHeight(BedrockTextGeometry.Panel panel) {
        return panel.text().lines().count() * 0.25f * panel.scale();
    }

    private static float estimatedWidth(BedrockTextGeometry.Panel panel) {
        int spaces = panel.text().lines().findFirst().orElseThrow().length();
        return (spaces * 9f + 1) * 0.025f * panel.scale();
    }

    private static Bounds coveredBounds(List<BedrockTextGeometry.Panel> panels) {
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (var panel : panels) {
            Vector3f right = panel.rotation().transform(new Vector3f(1, 0, 0));
            Vector3f up = panel.rotation().transform(new Vector3f(0, 1, 0));
            for (int across : new int[] {-1, 1}) {
                Vector3f corner = new Vector3f(panel.position()).fma(across * estimatedWidth(panel) / 2, right);
                Vector3f opposite = new Vector3f(corner).fma(estimatedHeight(panel), up);
                min.min(corner).min(opposite);
                max.max(corner).max(opposite);
            }
        }
        return new Bounds(min, max);
    }

    private record Bounds(Vector3f min, Vector3f max) {}
}
