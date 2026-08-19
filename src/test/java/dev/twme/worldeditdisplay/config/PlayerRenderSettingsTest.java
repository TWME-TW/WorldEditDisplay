package dev.twme.worldeditdisplay.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerRenderSettingsTest {

    private PlayerRenderSettings settings;

    @BeforeEach
    void setUp() {
        settings = PlayerRenderSettings.inMemory(new RenderSettings(null));
    }

    @Test
    void appliesAllPreviouslyMissingDisplayOverrides() {
        assertTrue(settings.set("renderer.cuboid.max_grid_spacing", 8));
        assertTrue(settings.set("renderer.cylinder.sqrt_scale_factor", 2.5));
        assertTrue(settings.set("renderer.cylinder.max_grid_spacing", 9));
        assertTrue(settings.set("renderer.ellipsoid.sqrt_scale_factor", 3.5));
        assertTrue(settings.set("renderer.ellipsoid.max_grid_spacing", 10));
        assertTrue(settings.set("renderer.polygon.max_grid_spacing", 11));

        assertEquals(8, settings.getCuboidMaxGridSpacing());
        assertEquals(2.5, settings.getCylinderSqrtScaleFactor());
        assertEquals(9, settings.getCylinderMaxGridSpacing());
        assertEquals(3.5, settings.getEllipsoidSqrtScaleFactor());
        assertEquals(10, settings.getEllipsoidMaxGridSpacing());
        assertEquals(11, settings.getPolygonMaxGridSpacing());
    }

    @Test
    void resettingRendererClearsParsedOverrides() {
        assertTrue(settings.set("renderer.cylinder.sqrt_scale_factor", 2.5));
        assertTrue(settings.set("renderer.cylinder.max_grid_spacing", 9));

        settings.reset("renderer.cylinder");

        assertEquals(4.0, settings.getCylinderSqrtScaleFactor());
        assertEquals(-1, settings.getCylinderMaxGridSpacing());
    }

    @Test
    void acceptsUnlimitedGridSpacingWhenConfiguredMinimumIsPositive() throws Exception {
        Field minimum = RenderSettings.class.getDeclaredField("gridSpacingMin");
        minimum.setAccessible(true);
        minimum.setInt(getServerSettings(), 1);

        assertTrue(settings.set("renderer.cuboid.max_grid_spacing", -1));
        assertEquals(-1, settings.getCuboidMaxGridSpacing());
    }

    @Test
    void reloadFromDiskDiscardsPendingDirtyState() {
        assertTrue(settings.set("renderer.cuboid.max_grid_spacing", 8));
        assertTrue(settings.isDirty());

        settings.load();

        assertFalse(settings.isDirty());
        assertEquals(-1, settings.getCuboidMaxGridSpacing());
    }

    private RenderSettings getServerSettings() throws Exception {
        Field field = PlayerRenderSettings.class.getDeclaredField("serverSettings");
        field.setAccessible(true);
        return (RenderSettings) field.get(settings);
    }
}
