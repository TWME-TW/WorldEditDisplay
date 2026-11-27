package dev.twme.worldeditdisplay.display.renderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.worldeditdisplay.util.GeyserPrimitiveText;
import dev.twme.worldeditdisplay.util.GeyserPrimitiveText.Primitive;

/** World-space overlays that do not put an entity in the player's block raycast. */
final class BedrockTextShape implements Shape {
    // Geyser uses its own small, increasing entity IDs for translated debug shapes.
    private static final AtomicLong NEXT_ID = new AtomicLong(1L << 62);
    private static final float FONT_SCALE = 0.25f / 0.266071429f;
    private static final float TEXT_ANCHOR_FROM_BOTTOM = 0.225f;

    private final List<BedrockTextGeometry.Panel> panels;
    private final Set<UUID> viewers = new HashSet<>();
    private final long firstId;
    private final Color color;
    private final boolean seeThrough;
    private boolean spawned;
    private double offsetX;
    private double offsetY;
    private double offsetZ;

    BedrockTextShape(List<BedrockTextGeometry.Panel> panels, org.bukkit.Color color, boolean seeThrough) {
        this.panels = panels;
        this.firstId = NEXT_ID.getAndAdd(panels.size());
        this.color = new Color(color.asARGB(), true);
        this.seeThrough = seeThrough;
    }

    @Override
    public void spawn() {
        if (spawned) return;
        spawned = true;
        try {
            draw(viewers);
        } catch (RuntimeException | LinkageError error) {
            remove();
            throw error;
        }
    }

    @Override
    public void remove() {
        if (!spawned) return;
        spawned = false;
        for (UUID viewer : viewers) GeyserPrimitiveText.remove(viewer, firstId, panels.size());
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }

    @Override
    public void addViewer(UUID viewerId) {
        if (viewers.add(viewerId) && spawned) draw(Set.of(viewerId));
    }

    @Override
    public void removeViewer(UUID viewerId) {
        if (viewers.remove(viewerId) && spawned) {
            GeyserPrimitiveText.remove(viewerId, firstId, panels.size());
        }
    }

    @Override
    public Set<UUID> getViewerUUIDs() {
        return Set.copyOf(viewers);
    }

    @Override
    public List<UUID> getEntityUUIDs() {
        return List.of();
    }

    @Override
    public int getEntityCount() {
        return 0;
    }

    @Override
    public void teleportOrigin(double x, double y, double z) {
        // Each primitive already has world coordinates.
    }

    @Override
    public void translate(double x, double y, double z) {
        offsetX += x;
        offsetY += y;
        offsetZ += z;
        if (spawned) draw(viewers);
    }

    private void draw(Set<UUID> recipients) {
        if (recipients.isEmpty() || panels.isEmpty()) return;
        List<Primitive> primitives = new ArrayList<>(panels.size());
        for (int i = 0; i < panels.size(); i++) {
            var panel = panels.get(i);
            // Match Geyser's single-row font scale, anchor and rotation in world space.
            Matrix3f transform = new Matrix3f().rotation(panel.rotation()).rotate(new Quaternionf(0, 1, 0, 0));
            Vector3f rotation = bedrockRotation(transform);
            float anchor = TEXT_ANCHOR_FROM_BOTTOM * panel.scale();
            var position = panel.position();
            primitives.add(new Primitive(firstId + i,
                    (float) (position.x + transform.m10() * anchor + offsetX),
                    (float) (position.y + transform.m11() * anchor + offsetY),
                    (float) (position.z + transform.m12() * anchor + offsetZ),
                    rotation.x, rotation.y, rotation.z,
                    panel.scale() * FONT_SCALE, panel.text()));
        }
        for (UUID viewer : recipients) GeyserPrimitiveText.draw(viewer, primitives, color, seeThrough);
    }

    static Vector3f bedrockRotation(Matrix3f transform) {
        double horizontal = Math.hypot(transform.m00(), transform.m01());
        double yaw = Math.atan2(-transform.m02(), horizontal);
        // At a quarter-turn, pitch and roll are coupled. Fix roll at zero and
        // recover the combined angle from the elements that remain defined.
        if (horizontal < 0.000001) {
            double pitch = Math.atan2(-transform.m21(), transform.m11());
            return new Vector3f((float) Math.toDegrees(pitch), (float) Math.toDegrees(yaw), 0);
        }
        return new Vector3f(
                (float) Math.toDegrees(Math.atan2(transform.m12(), transform.m22())),
                (float) Math.toDegrees(yaw),
                (float) Math.toDegrees(Math.atan2(transform.m01(), transform.m00())));
    }
}
