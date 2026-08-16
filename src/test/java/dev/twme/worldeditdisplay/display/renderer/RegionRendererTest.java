package dev.twme.worldeditdisplay.display.renderer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class RegionRendererTest {

    @Test
    void rejectsDegenerateLineBelowShapeLibraryMinimumLength() {
        RegionRenderer.Line line = new RegionRenderer.Line(
                new Vector3f(1.0f, 2.0f, 3.0f),
                new Vector3f(1.0005f, 2.0f, 3.0f));

        assertFalse(RegionRenderer.isRenderableLine(line));
    }

    @Test
    void acceptsLineAtShapeLibraryMinimumLength() {
        RegionRenderer.Line line = new RegionRenderer.Line(
                new Vector3f(1.0f, 2.0f, 3.0f),
                new Vector3f(1.001f, 2.0f, 3.0f));

        assertTrue(RegionRenderer.isRenderableLine(line));
    }

    @Test
    void rejectsNonFiniteLine() {
        RegionRenderer.Line line = new RegionRenderer.Line(
                new Vector3f(Float.NaN, 2.0f, 3.0f),
                new Vector3f(4.0f, 5.0f, 6.0f));

        assertFalse(RegionRenderer.isRenderableLine(line));
    }

    @Test
    void rejectsUnloadedWorldLocationsWithoutResolvingTheirWorlds() {
        Location unloadedOrigin = new Location(null, 0.0, 64.0, 0.0);
        Location unloadedPlayerLocation = new Location(null, 100.0, 64.0, 0.0);

        assertFalse(RegionRenderer.isSameLoadedWorld(unloadedPlayerLocation, unloadedOrigin));
    }
}
