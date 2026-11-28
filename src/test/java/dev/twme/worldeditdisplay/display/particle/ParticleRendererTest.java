package dev.twme.worldeditdisplay.display.particle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.CuboidRegion;

class ParticleRendererTest {
    @Test
    void cuboidRedrawKeepsViewersUntilRendererIsCleared() {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        Player owner = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[] {Player.class}, (proxy, method, args) -> ownerId);
        PlayerRenderSettings settings = new PlayerRenderSettings(null) {
            @Override public Color getCuboidPoint1Color() { return Color.GREEN; }
            @Override public Color getCuboidPoint2Color() { return Color.BLUE; }
        };
        var renderer = new ParticleCuboidRenderer(null, owner, settings);
        renderer.addViewer(viewerId);

        var selection = new CuboidRegion(null);
        selection.setCuboidPoint(0, 2, 72, 2);
        renderer.render(selection);
        int markerCount = renderer.getPointCount();
        assertTrue(markerCount > 0);
        assertEquals(List.of(ownerId, viewerId), renderer.getViewers());

        selection.setCuboidPoint(1, 6, 75, 6);
        renderer.render(selection);
        int selectionCount = renderer.getPointCount();
        assertTrue(selectionCount > markerCount);
        renderer.render(selection);
        assertEquals(selectionCount, renderer.getPointCount());
        assertEquals(List.of(ownerId, viewerId), renderer.getViewers());

        renderer.clear();
        assertEquals(0, renderer.getPointCount());
        assertTrue(renderer.getViewers().isEmpty());
    }
}
