package dev.twme.worldeditdisplay.event.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.region.Region;
import dev.twme.worldeditdisplay.region.RegionType;

class CUIEventUpdateTest {

    @Test
    void updateMarksSingleSelectionDirty() {
        PlayerData playerData = playerData();
        TestRegion region = new TestRegion(playerData);
        region.clearDirty();
        playerData.setSelection(region);

        CUIEventUpdate event = new CUIEventUpdate(new CUIEventArgs(playerData, false, "u", List.of("0")));
        event.prepare();
        event.raise();

        assertTrue(region.isDirty());
        assertTrue(event.shouldUpdateRender());
    }

    @Test
    void updateSkipsRenderWhenSelectionIsMissing() {
        PlayerData playerData = playerData();

        CUIEventUpdate event = new CUIEventUpdate(new CUIEventArgs(playerData, false, "u", List.of("0")));
        event.prepare();
        event.raise();

        assertFalse(event.shouldUpdateRender());
    }

    private PlayerData playerData() {
        return new PlayerData(null);
    }

    private static final class TestRegion extends Region {
        private TestRegion(PlayerData playerData) {
            super(playerData);
        }

        @Override
        public RegionType getType() {
            return RegionType.CUBOID;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public String getInfo() {
            return "test";
        }
    }
}