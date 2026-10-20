package dev.twme.worldeditdisplay.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerDataRenderDebounceTest {

    @Test
    void restartsDebounceForBurstsWithinTheDeferLimit() {
        assertTrue(PlayerData.shouldRestartRenderDebounce(1_000L, 1_000L));
        assertTrue(PlayerData.shouldRestartRenderDebounce(1_000L, 1_249L));
    }

    @Test
    void keepsPendingRenderOnceContinuousEventsReachTheDeferLimit() {
        assertFalse(PlayerData.shouldRestartRenderDebounce(1_000L, 1_250L));
        assertFalse(PlayerData.shouldRestartRenderDebounce(1_000L, 5_000L));
    }
}
