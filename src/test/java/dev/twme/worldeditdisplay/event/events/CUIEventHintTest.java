package dev.twme.worldeditdisplay.event.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.region.EllipsoidRegion;
import dev.twme.worldeditdisplay.region.Region;

class CUIEventHintTest {

    @Test
    void outlineHintMarksCurrentMultiRegion() {
        PlayerData playerData = new PlayerData(null);
        Region region = startMultiEllipsoid(playerData);

        raise(playerData, true, "OUTLINE");

        assertTrue(region.isOutlineOnly());
    }

    @Test
    void unknownHintsAreIgnored() {
        PlayerData playerData = new PlayerData(null);
        Region region = startMultiEllipsoid(playerData);

        assertDoesNotThrow(() -> raise(playerData, true, "future-hint"));
        assertFalse(region.isOutlineOnly());
    }

    @Test
    void hintRequiresMultiRegion() {
        PlayerData playerData = new PlayerData(null);
        assertThrows(IllegalStateException.class, () -> raise(playerData, false, "outline"));
    }

    @Test
    void hintIsRegisteredUnderWedKey() {
        assertSame(CUIEventType.HINT, CUIEventType.named("wed"));
    }

    private static Region startMultiEllipsoid(PlayerData playerData) {
        CUIEventSelection selection = new CUIEventSelection(new CUIEventArgs(
                playerData, true, "s", List.of("ellipsoid", UUID.randomUUID().toString())));
        selection.prepare();
        selection.raise();
        Region region = playerData.getCurrentMultiRegion();
        assertInstanceOf(EllipsoidRegion.class, region);
        return region;
    }

    private static void raise(PlayerData playerData, boolean multi, String... params) {
        CUIEventHint event = new CUIEventHint(new CUIEventArgs(playerData, multi, "wed", List.of(params)));
        event.prepare();
        event.raise();
    }
}
