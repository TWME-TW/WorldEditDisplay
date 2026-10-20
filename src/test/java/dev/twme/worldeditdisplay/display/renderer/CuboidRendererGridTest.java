package dev.twme.worldeditdisplay.display.renderer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CuboidRendererGridTest {

    @Test
    void unsetSpacingKeepsAutomaticGrid() {
        assertFalse(CuboidRenderer.isGridDisabledByCui(false, 0.0));
    }

    @Test
    void explicitSpacingBelowMinimumHidesGridLikeWorldEditCui() {
        assertTrue(CuboidRenderer.isGridDisabledByCui(true, 0.0));
        assertTrue(CuboidRenderer.isGridDisabledByCui(true, 0.5));
        assertTrue(CuboidRenderer.isGridDisabledByCui(true, -1.0));
        assertTrue(CuboidRenderer.isGridDisabledByCui(true, Double.NaN));
    }

    @Test
    void explicitSpacingAtOrAboveMinimumKeepsGrid() {
        assertFalse(CuboidRenderer.isGridDisabledByCui(true, 1.0));
        assertFalse(CuboidRenderer.isGridDisabledByCui(true, 16.0));
    }
}
