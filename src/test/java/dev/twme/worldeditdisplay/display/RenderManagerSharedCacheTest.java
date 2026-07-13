package dev.twme.worldeditdisplay.display;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

class RenderManagerSharedCacheTest {

    @Test
    void releaseSharedColorUsesLiveRendererReferencesNotPersistentShares() throws Exception {
        RenderManager renderManager = new RenderManager();
        UUID sharerId = UUID.randomUUID();

        renderManager.getSharedColors().put(sharerId, Color.RED);
        renderManager.getLabelComponentNames().put(sharerId, "Sharer");

        renderManager.releaseSharedColorForTest(sharerId);

        assertFalse(renderManager.getSharedColors().containsKey(sharerId));
        assertFalse(renderManager.getLabelComponentNames().containsKey(sharerId));
    }

    @Test
    void releaseSharedColorKeepsColorWhenLabelStillReferencesSharer() throws Exception {
        RenderManager renderManager = new RenderManager();
        UUID viewerId = UUID.randomUUID();
        UUID sharerId = UUID.randomUUID();

        renderManager.getSharedColors().put(sharerId, Color.RED);
        Map<UUID, me.tofaa.entitylib.wrapper.WrapperEntity> viewerLabels = new java.util.HashMap<>();
        viewerLabels.put(sharerId, null);
        renderManager.getLabelEntities().put(viewerId, viewerLabels);

        renderManager.releaseSharedColorForTest(sharerId);

        assertTrue(renderManager.getSharedColors().containsKey(sharerId));
    }

    @Test
    void clearSharedRenderReleasesCacheAfterRemovingLastLabel() {
        RenderManager renderManager = new RenderManager();
        UUID viewerId = UUID.randomUUID();
        UUID sharerId = UUID.randomUUID();

        renderManager.getSharedColors().put(sharerId, Color.RED);
        renderManager.getLabelComponentNames().put(sharerId, "Sharer");
        Map<UUID, me.tofaa.entitylib.wrapper.WrapperEntity> viewerLabels = new java.util.HashMap<>();
        viewerLabels.put(sharerId, null);
        renderManager.getLabelEntities().put(viewerId, viewerLabels);

        renderManager.clearSharedRender(viewerId, sharerId);

        assertFalse(renderManager.getSharedColors().containsKey(sharerId));
        assertFalse(renderManager.getLabelComponentNames().containsKey(sharerId));
        assertFalse(renderManager.getLabelEntities().containsKey(viewerId));
    }

    @Test
    void clearSharedRendersReleasesCacheForLabelOnlyReference() {
        RenderManager renderManager = new RenderManager();
        UUID viewerId = UUID.randomUUID();
        UUID sharerId = UUID.randomUUID();

        renderManager.getSharedColors().put(sharerId, Color.RED);
        renderManager.getLabelComponentNames().put(sharerId, "Sharer");
        Map<UUID, me.tofaa.entitylib.wrapper.WrapperEntity> viewerLabels = new java.util.HashMap<>();
        viewerLabels.put(sharerId, null);
        renderManager.getLabelEntities().put(viewerId, viewerLabels);

        renderManager.clearSharedRenders(viewerId);

        assertFalse(renderManager.getSharedColors().containsKey(sharerId));
        assertFalse(renderManager.getLabelComponentNames().containsKey(sharerId));
        assertFalse(renderManager.getLabelEntities().containsKey(viewerId));
    }

    @Test
    void clearSharerRendersClearsLabelOnlyReference() {
        RenderManager renderManager = new RenderManager();
        UUID viewerId = UUID.randomUUID();
        UUID sharerId = UUID.randomUUID();

        renderManager.getSharedColors().put(sharerId, Color.RED);
        renderManager.getLabelComponentNames().put(sharerId, "Sharer");
        Map<UUID, me.tofaa.entitylib.wrapper.WrapperEntity> viewerLabels = new java.util.HashMap<>();
        viewerLabels.put(sharerId, null);
        renderManager.getLabelEntities().put(viewerId, viewerLabels);

        renderManager.clearSharerRenders(sharerId);

        assertFalse(renderManager.getSharedColors().containsKey(sharerId));
        assertFalse(renderManager.getLabelComponentNames().containsKey(sharerId));
        assertFalse(renderManager.getLabelEntities().containsKey(viewerId));
    }

}
