package dev.twme.worldeditdisplay.display.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

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
}
