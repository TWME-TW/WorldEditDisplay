package dev.twme.worldeditdisplay.util;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.bukkit.plugin.Plugin;
import org.geysermc.geyser.api.GeyserApi;

/** Sends world-space Bedrock primitives through the local Geyser connection. */
public final class GeyserPrimitiveText {
    private static final String BEDROCK = "org.cloudburstmc.protocol.bedrock.";
    private static final Color INVISIBLE_TEXT = new Color(255, 255, 255, 0);
    private static volatile GeyserPrimitiveText instance;

    private final ClassLoader loader;
    private final Class<?> sessionType;
    private final Constructor<?> packetConstructor;
    private final Constructor<?> textConstructor;
    private final Constructor<?> removeConstructor;
    private final Method shapes;
    private final Method vector;
    private final Method dimension;
    private final Method dimensionId;
    private final Method useRotation;
    private final Method background;
    private final Method depthTest;
    private final Method renderDistance;
    private final Method closed;
    private final Method send;

    private GeyserPrimitiveText(ClassLoader loader) throws ReflectiveOperationException {
        this.loader = loader;
        Class<?> vectorType = loader.loadClass("org.cloudburstmc.math.vector.Vector3f");
        Class<?> textType = loader.loadClass(BEDROCK + "data.debugshape.DebugText");
        Class<?> packetType = loader.loadClass(BEDROCK + "packet.DebugDrawerPacket");
        sessionType = loader.loadClass("org.geysermc.geyser.session.GeyserSession");
        packetConstructor = packetType.getConstructor();
        textConstructor = textType.getConstructor(long.class, int.class, vectorType, Float.class,
                vectorType, Float.class, Color.class, String.class);
        removeConstructor = loader.loadClass(BEDROCK + "data.debugshape.DebugShape").getConstructor(long.class);
        shapes = packetType.getMethod("getShapes");
        vector = vectorType.getMethod("from", float.class, float.class, float.class);
        dimension = sessionType.getMethod("getBedrockDimension");
        dimensionId = dimension.getReturnType().getMethod("bedrockId");
        useRotation = textType.getMethod("setUseRotation", boolean.class);
        background = textType.getMethod("setBackgroundColor", Color.class);
        depthTest = textType.getMethod("setDepthTest", boolean.class);
        renderDistance = textType.getMethod("setMaximumRenderDistance", Float.class);
        closed = sessionType.getMethod("isClosed");
        send = sessionType.getMethod("sendUpstreamPacket", loader.loadClass(BEDROCK + "packet.BedrockPacket"));
    }

    public static synchronized boolean isAvailable(Plugin geyser) {
        ClassLoader loader = geyser.getClass().getClassLoader();
        if (instance != null && instance.loader == loader) return true;
        try {
            instance = new GeyserPrimitiveText(loader);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            instance = null;
            return false;
        }
    }

    public static void draw(UUID viewer, List<Primitive> primitives, Color color, boolean seeThrough) {
        GeyserPrimitiveText transport = instance;
        if (transport == null || primitives.isEmpty()) return;
        transport.send(viewer, primitives, 0, 0, color, seeThrough);
    }

    public static void remove(UUID viewer, long firstId, int count) {
        GeyserPrimitiveText transport = instance;
        if (transport == null || count == 0) return;
        transport.send(viewer, List.of(), firstId, count, null, false);
    }

    @SuppressWarnings("unchecked")
    private void send(UUID viewer, List<Primitive> primitives, long firstId, int count,
                      Color color, boolean seeThrough) {
        Object session = GeyserApi.api().connectionByUuid(viewer);
        if (!sessionType.isInstance(session)) return;
        try {
            if ((boolean) closed.invoke(session)) return;
            Object packet = packetConstructor.newInstance();
            List<Object> entries = (List<Object>) shapes.invoke(packet);
            if (count > 0) {
                for (int i = 0; i < count; i++) entries.add(removeConstructor.newInstance(firstId + i));
            } else {
                Object currentDimension = dimension.invoke(session);
                if (currentDimension == null) return;
                int id = (int) dimensionId.invoke(currentDimension);
                for (Primitive primitive : primitives) {
                    Object position = vector.invoke(null, primitive.x(), primitive.y(), primitive.z());
                    Object rotation = vector.invoke(null, primitive.pitch(), primitive.yaw(), primitive.roll());
                    Object text = textConstructor.newInstance(primitive.id(), id, position, primitive.scale(),
                            rotation, null, INVISIBLE_TEXT, primitive.text());
                    useRotation.invoke(text, true);
                    background.invoke(text, color);
                    depthTest.invoke(text, !seeThrough);
                    renderDistance.invoke(text, 128f);
                    entries.add(text);
                }
            }
            send.invoke(session, packet);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not send Bedrock selection geometry", exception);
        }
    }

    public record Primitive(long id, float x, float y, float z,
                            float pitch, float yaw, float roll, float scale, String text) {}
}
