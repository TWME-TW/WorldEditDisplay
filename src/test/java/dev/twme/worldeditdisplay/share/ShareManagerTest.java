package dev.twme.worldeditdisplay.share;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShareManagerTest {

    @TempDir
    File tempDir;

    @Test
    void acceptedSharesPersistAcrossManagerReloads() {
        File dataFile = new File(tempDir, "share_data.yml");
        UUID sharer = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        ShareManager manager = new ShareManager(dataFile, 30);
    assertEquals(ShareManager.RequestResult.SENT, manager.sendRequest(sharer, viewer));
        assertTrue(manager.acceptShare(viewer, sharer));

        ShareManager reloaded = new ShareManager(dataFile, 30);

        assertTrue(reloaded.isActiveShare(sharer, viewer));
        assertTrue(reloaded.getActiveSharers(viewer).contains(sharer));
        assertTrue(reloaded.getActiveViewers(sharer).contains(viewer));
    }

    @Test
    void playerQuitClearsPendingInvitesButKeepsAcceptedShares() {
        File dataFile = new File(tempDir, "share_data.yml");
        UUID sharer = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        UUID pendingTarget = UUID.randomUUID();

        ShareManager manager = new ShareManager(dataFile, 30);
    assertEquals(ShareManager.RequestResult.SENT, manager.sendRequest(sharer, viewer));
        assertTrue(manager.acceptShare(viewer, sharer));
    assertEquals(ShareManager.RequestResult.SENT, manager.sendRequest(sharer, pendingTarget));

        manager.onPlayerQuit(sharer);

        assertTrue(manager.isActiveShare(sharer, viewer));
        assertTrue(manager.getActiveSharers(viewer).contains(sharer));
        assertFalse(manager.hasPendingRequest(sharer, pendingTarget));
    }
}