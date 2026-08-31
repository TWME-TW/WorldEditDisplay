package dev.twme.worldeditdisplay.event.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.region.CuboidRegion;
import dev.twme.worldeditdisplay.region.Region;

class CUIEventSelectionTest {

    @Test
    void shapeOnlySelectionSchedulesRenderToClearPreviousGeometry() {
        PlayerData playerData = new PlayerData(null);
        CuboidRegion previous = new CuboidRegion(playerData);
        previous.setCuboidPoint(0, 1, 2, 3);
        previous.setCuboidPoint(1, 4, 5, 6);
        playerData.setSelection(previous);

        CUIEventSelection event = new CUIEventSelection(
                new CUIEventArgs(playerData, false, "s", List.of("cuboid")));
        event.prepare();
        event.raise();

        Region current = playerData.getSelection();
        assertNotSame(previous, current);
        assertFalse(current.isDefined());
        assertTrue(event.shouldUpdateRender());
    }

    @Test
    void initialShapeStillWaitsForPointEvents() {
        PlayerData playerData = new PlayerData(null);

        CUIEventSelection event = new CUIEventSelection(
                new CUIEventArgs(playerData, false, "s", List.of("cuboid")));
        event.prepare();
        event.raise();

        assertFalse(event.shouldUpdateRender());
    }
}
