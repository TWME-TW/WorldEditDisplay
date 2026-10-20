package dev.twme.worldeditdisplay.display.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.twme.worldeditdisplay.display.renderer.RegionRenderer.Line;
import dev.twme.worldeditdisplay.display.renderer.RegionRenderer.LineKey;

class RegionRendererTranslationTest {

    private static final Color EDGE = Color.fromARGB(0x55, 0xFF, 0xFF, 0xFF);
    private static final Color MARKER = Color.fromARGB(0xFF, 0x00, 0xFF, 0x00);

    @Test
    void findsOffsetOfAMovedBox() {
        List<LineKey> previous = box(10, -60, -20, 15, -57, -11, EDGE);
        previous.addAll(box(10.03f, -60.03f, -20.03f, 11.03f, -58.97f, -18.97f, MARKER));
        List<LineKey> next = box(27, -60, -20, 32, -57, -11, EDGE);
        next.addAll(box(27.03f, -60.03f, -20.03f, 28.03f, -58.97f, -18.97f, MARKER));

        Vector3f offset = RegionRenderer.findTranslation(
                RegionRenderer.sortedLineKeys(previous), RegionRenderer.sortedLineKeys(next));

        assertEquals(new Vector3f(17, 0, 0), offset);
    }

    @Test
    void rejectsResizedBox() {
        assertNull(RegionRenderer.findTranslation(
                RegionRenderer.sortedLineKeys(box(0, 0, 0, 5, 3, 9, EDGE)),
                RegionRenderer.sortedLineKeys(box(1, 0, 0, 10, 3, 5, EDGE))));
    }

    @Test
    void rejectsColourChangeEvenWhenMoved() {
        assertNull(RegionRenderer.findTranslation(
                RegionRenderer.sortedLineKeys(box(0, 0, 0, 5, 3, 9, EDGE)),
                RegionRenderer.sortedLineKeys(box(2, 0, 0, 7, 3, 9, MARKER))));
    }

    @Test
    void rejectsPartialMoves() {
        List<LineKey> previous = box(0, 0, 0, 5, 3, 9, EDGE);
        previous.addAll(box(0.03f, 0.03f, 0.03f, 1.03f, 1.03f, 1.03f, MARKER));
        List<LineKey> next = box(4, 0, 0, 9, 3, 9, EDGE);
        next.addAll(box(0.03f, 0.03f, 0.03f, 1.03f, 1.03f, 1.03f, MARKER)); // marker stayed

        assertNull(RegionRenderer.findTranslation(
                RegionRenderer.sortedLineKeys(previous), RegionRenderer.sortedLineKeys(next)));
    }

    @Test
    void rejectsEmptyAndDifferentCounts() {
        assertNull(RegionRenderer.findTranslation(List.of(), List.of()));
        List<LineKey> previous = RegionRenderer.sortedLineKeys(box(0, 0, 0, 5, 3, 9, EDGE));
        List<LineKey> next = RegionRenderer.sortedLineKeys(box(1, 0, 0, 6, 3, 9, EDGE));
        assertNull(RegionRenderer.findTranslation(previous, next.subList(0, 11)));
    }

    @Test
    void identicalPassIsNotATranslation() {
        List<LineKey> keys = RegionRenderer.sortedLineKeys(box(0, 0, 0, 5, 3, 9, EDGE));
        assertNull(RegionRenderer.findTranslation(keys, keys));
    }

    @Test
    void negativeOffsetsAtLargeCoordinates() {
        List<LineKey> previous = box(29_990, 64, -30_000, 30_001, 70, -29_990, EDGE);
        List<LineKey> next = box(29_987, 63, -29_998, 29_998, 69, -29_988, EDGE);

        Vector3f offset = RegionRenderer.findTranslation(
                RegionRenderer.sortedLineKeys(previous), RegionRenderer.sortedLineKeys(next));

        assertEquals(new Vector3f(-3, -1, 2), offset);
    }

    /** The 12 edges of a box, in the order RegionRenderer#renderBoxFrame emits them. */
    private static List<LineKey> box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color color) {
        Vector3f v000 = new Vector3f(minX, minY, minZ);
        Vector3f v001 = new Vector3f(minX, minY, maxZ);
        Vector3f v010 = new Vector3f(minX, maxY, minZ);
        Vector3f v011 = new Vector3f(minX, maxY, maxZ);
        Vector3f v100 = new Vector3f(maxX, minY, minZ);
        Vector3f v101 = new Vector3f(maxX, minY, maxZ);
        Vector3f v110 = new Vector3f(maxX, maxY, minZ);
        Vector3f v111 = new Vector3f(maxX, maxY, maxZ);
        List<LineKey> keys = new ArrayList<>();
        for (Line line : List.of(
                new Line(v000, v001), new Line(v000, v100), new Line(v001, v101), new Line(v100, v101),
                new Line(v010, v011), new Line(v010, v110), new Line(v011, v111), new Line(v110, v111),
                new Line(v000, v010), new Line(v001, v011), new Line(v100, v110), new Line(v101, v111))) {
            keys.add(LineKey.of(line, color, 0.05f, true));
        }
        return keys;
    }
}
