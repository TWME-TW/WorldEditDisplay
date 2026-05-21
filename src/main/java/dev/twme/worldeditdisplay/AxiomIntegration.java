package dev.twme.worldeditdisplay;

import java.util.List;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import dev.twme.textdisplayshape.shape.Shape;

/**
 * Soft-dependency bridge for AxiomPaper.
 *
 * When the Axiom plugin is not installed, this class is safe to reference —
 * all methods return immediately and the inner {@code AxiomBridge} class
 * (which references Axiom API classes) is never loaded by the JVM.
 */
public final class AxiomIntegration {

    private static boolean present = false;

    private AxiomIntegration() {
    }

    /**
     * Must be called during {@code onEnable} to detect whether Axiom is installed.
     */
    public static void init(JavaPlugin plugin) {
        present = plugin.getServer().getPluginManager().getPlugin("AxiomPaper") != null;
        if (present) {
            plugin.getLogger().info("Axiom detected — WorldEditDisplay entities will be hidden from Axiom gizmos.");
        }
    }

    public static boolean isPresent() {
        return present;
    }

    /**
     * Hides every entity that makes up {@code shape} from Axiom's display gizmo overlay.
     *
     * <p>One opaque key object is created per entity UUID and appended to {@code holder}.
     * The caller must keep {@code holder} alive (i.e. clear it together with the shapes
     * list) so that the {@code WeakHashMap} inside Axiom retains the entries for as
     * long as the shape exists.</p>
     */
    public static void hideShape(List<Object> holder, Shape shape) {
        if (!present) return;
        AxiomBridge.hideShape(holder, shape);
    }

    /**
     * Hides a single entity from Axiom's display gizmo overlay.
     *
     * <p>The caller must hold a strong reference to {@code keyObject} (e.g. the entity
     * object itself stored in a map) for as long as the entity should stay hidden.</p>
     */
    public static void hideEntity(Object keyObject, UUID uuid) {
        if (!present) return;
        AxiomBridge.hideEntity(keyObject, uuid);
    }

    /**
     * Inner class that is only loaded when Axiom is actually present.
     * Isolating the {@code AxiomEntityAPI} reference here prevents a
     * {@code NoClassDefFoundError} when Axiom is not installed.
     */
    private static final class AxiomBridge {

        private AxiomBridge() {
        }

        static void hideShape(List<Object> holder, Shape shape) {
            com.moulberry.axiom.paperapi.AxiomEntityAPI api =
                    com.moulberry.axiom.paperapi.AxiomEntityAPI.getAPI();
            for (UUID uuid : shape.getEntityUUIDs()) {
                Object key = new Object();
                holder.add(key);
                api.hideCustomDisplayGizmo(key, uuid);
            }
        }

        static void hideEntity(Object keyObject, UUID uuid) {
            com.moulberry.axiom.paperapi.AxiomEntityAPI.getAPI()
                    .hideCustomDisplayGizmo(keyObject, uuid);
        }
    }
}
