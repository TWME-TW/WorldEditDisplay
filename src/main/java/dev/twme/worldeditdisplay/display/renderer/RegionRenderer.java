package dev.twme.worldeditdisplay.display.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;

import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.Region;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.joml.Vector3f;

/**
 * Abstract base class for rendering regions.
 *
 * Provides common rendering utilities:
 * - entity pool management
 * - player visibility handling
 * - transforms (scale, translate, rotate)
 * - cleanup
 *
 * @param <T> the region type this renderer handles
 */
public abstract class RegionRenderer<T extends Region> {

    protected final WorldEditDisplay plugin;
    protected final Player player;
    protected final UUID playerUUID;
    protected final PlayerRenderSettings settings;

    // pool of display entities
    protected final List<WrapperEntity> entities;

    protected RenderConfig config;

    /**
     * Constructor
     *
     * @param plugin plugin instance
     * @param player target player
     * @param settings player render settings
     */
    public RegionRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        this.plugin = plugin;
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.settings = settings;
        this.entities = new ArrayList<>();
        this.config = RenderConfig.getDefault();
    }

    /**
     * Render the given region
     */
    public abstract void render(T region);

    /**
     * Get the type of region this renderer supports
     */
    public abstract Class<T> getRegionType();

    /**
     * Remove all entities from the world and clear the pool
     */
    public void clear() {
        for (WrapperEntity entity : entities) {
            try {
                entity.remove();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove entity: " + entity.getEntityId(), e);
            }
        }
        entities.clear();
    }

    /**
     * Spawn and register a new entity
     */
    protected WrapperEntity createEntity(EntityType entityType, Location location) {
        WrapperEntity entity = new WrapperEntity(entityType);
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(location));
        entities.add(entity);
        return entity;
    }

    /**
     * Set basic display meta for an entity
     */
    protected void setupDisplayMeta(WrapperEntity entity) {
        if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta)) return;
        AbstractDisplayMeta meta = (AbstractDisplayMeta) entity.getEntityMeta();
        meta.setViewRange(config.getViewRange());
        if (config.isAlwaysBright()) meta.setBrightnessOverride(config.getFullBrightness());
        meta.setShadowRadius(config.getShadowRadius());
        meta.setShadowStrength(config.getShadowStrength());
        if (config.hasGlowColor()) meta.setGlowColorOverride(config.getGlowColor());
    }

    /**
     * Set entity scale
     */
    protected void setScale(WrapperEntity entity, com.github.retrooper.packetevents.util.Vector3f scale) {
        if (entity.getEntityMeta() instanceof AbstractDisplayMeta) {
            ((AbstractDisplayMeta) entity.getEntityMeta()).setScale(scale);
        }
    }

    /**
     * Set entity translation
     */
    protected void setTranslation(WrapperEntity entity, com.github.retrooper.packetevents.util.Vector3f translation) {
        if (entity.getEntityMeta() instanceof AbstractDisplayMeta) {
            ((AbstractDisplayMeta) entity.getEntityMeta()).setTranslation(translation);
        }
    }

    /**
     * Set entity rotation
     */
    protected void setRotation(WrapperEntity entity, Quaternion4f rotation) {
        if (entity.getEntityMeta() instanceof AbstractDisplayMeta) {
            ((AbstractDisplayMeta) entity.getEntityMeta()).setLeftRotation(rotation);
        }
    }

    /**
     * Batch update entity meta to avoid sending multiple packets
     */
    protected void batchUpdate(WrapperEntity entity, Runnable updater) {
        if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta)) {
            updater.run();
            return;
        }
        AbstractDisplayMeta meta = (AbstractDisplayMeta) entity.getEntityMeta();
        meta.getMetadata().setNotifyAboutChanges(false);
        updater.run();
        meta.getMetadata().setNotifyAboutChanges(true);
        entity.sendPacketToViewers(meta.createPacket());
    }

    /**
     * Interpolate points between two locations
     * @return list of points (excluding start and end)
     */
    protected List<Location> interpolate(Location start, Location end, int segments) {
        List<Location> points = new ArrayList<>();
        if (segments <= 0) return points;
        double dx = (end.getX() - start.getX()) / (segments + 1);
        double dy = (end.getY() - start.getY()) / (segments + 1);
        double dz = (end.getZ() - start.getZ()) / (segments + 1);
        for (int i = 1; i <= segments; i++) {
            points.add(new Location(
                    start.getWorld(),
                    start.getX() + dx * i,
                    start.getY() + dy * i,
                    start.getZ() + dz * i
            ));
        }
        return points;
    }

    protected Location toLocation(double x, double y, double z) {
        return new Location(player.getWorld(), x, y, z);
    }

    /**
     * Render a line using an ItemDisplay entity
     */
    protected void renderLine(Line line, Material material, float thickness) {
        Vector3f start = line.start();
        Vector3f end = line.end();
        float length = start.distance(end) + thickness;
        Vector3f midpoint = new Vector3f((start.x + end.x)/2, (start.y + end.y)/2, (start.z + end.z)/2);
        Location spawnLoc = new Location(player.getWorld(), player.getLocation().x(), player.getLocation().y(), player.getLocation().z());
        WrapperEntity entity = createEntity(EntityTypes.ITEM_DISPLAY, spawnLoc);
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        Vector3f playerPos = new Vector3f((float) spawnLoc.getX(), (float) spawnLoc.getY(), (float) spawnLoc.getZ());
        Vector3f translation = new Vector3f(midpoint).sub(playerPos);
        Vector3f direction = new Vector3f(end).sub(start).normalize();
        meta.setItem(SpigotConversionUtil.fromBukkitItemStack(new ItemStack(material)));
        meta.setDisplayType(ItemDisplayMeta.DisplayType.NONE);
        meta.setScale(new com.github.retrooper.packetevents.util.Vector3f(thickness, thickness, length));
        Vector3f defaultDir = new Vector3f(0, 0, 1);
        org.joml.Quaternionf rotation = new org.joml.Quaternionf();
        rotation.rotationTo(defaultDir, direction);
        meta.setLeftRotation(new Quaternion4f(rotation.x, rotation.y, rotation.z, rotation.w));
        meta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(translation.x, translation.y, translation.z));
        setupDisplayMeta(entity);
        entity.addViewer(playerUUID);
    }

    /**
     * Renders multiple lines
     */
    protected void renderLines(Material material, float thickness, Line... lines) {
        for (Line line : lines) {
            renderLine(line, material, thickness);
        }
    }

    /**
     * Render a cube marker
     */
    protected void renderCube(Vector3f center, float size, Material material, float thickness) {
        float halfSize = size / 2.0f;
        renderBoxFrame(center.x - halfSize, center.y - halfSize, center.z - halfSize,
                center.x + halfSize, center.y + halfSize, center.z + halfSize,
                material, thickness);
    }

    /**
     * Render the edges of a box using 12 lines
     */
    protected void renderBoxFrame(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Material material, float thickness) {
        Vector3f v000 = new Vector3f((float) minX, (float) minY, (float) minZ);
        Vector3f v001 = new Vector3f((float) minX, (float) minY, (float) maxZ);
        Vector3f v010 = new Vector3f((float) minX, (float) maxY, (float) minZ);
        Vector3f v011 = new Vector3f((float) minX, (float) maxY, (float) maxZ);
        Vector3f v100 = new Vector3f((float) maxX, (float) minY, (float) minZ);
        Vector3f v101 = new Vector3f((float) maxX, (float) minY, (float) maxZ);
        Vector3f v110 = new Vector3f((float) maxX, (float) maxY, (float) minZ);
        Vector3f v111 = new Vector3f((float) maxX, (float) maxY, (float) maxZ);

        renderLines(material, thickness,
                // Bottom face
                new Line(v000, v001), new Line(v000, v100),
                new Line(v001, v101), new Line(v100, v101),

                // Top face
                new Line(v010, v011), new Line(v010, v110),
                new Line(v011, v111), new Line(v110, v111),

                // Vertical edges
                new Line(v000, v010), new Line(v001, v011),
                new Line(v100, v110), new Line(v101, v111)
        );
    }

    /**
     * Render a point marker with a small padding cube
     */
    protected void renderPointMarker(dev.twme.worldeditdisplay.region.Vector3 point, Material material, float thickness) {
        final double PADDING = 0.03;
        renderBoxFrame(point.getX() - PADDING, point.getY() - PADDING, point.getZ() - PADDING,
                point.getX() + 1.0 + PADDING, point.getY() + 1.0 + PADDING, point.getZ() + 1.0 + PADDING,
                material, thickness);
    }

    public void setConfig(RenderConfig config) {
        this.config = config;
    }

    public int getEntityCount() {
        return entities.size();
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Get the material for rendering, using CUI override for multi-selection
     */
    protected Material getMaterialWithOverride(Region region, int colorIndex,
                                               Material defaultMaterial, boolean isMultiSelection) {
        if (!isMultiSelection) return defaultMaterial;
        Material override = region.getColorMaterial(colorIndex);
        return override != null ? override : defaultMaterial;
    }

    /**
     * Check if a region is part of a multi-selection
     */
    protected boolean isMultiSelection(Region region) {
        if (player == null) return false;
        dev.twme.worldeditdisplay.player.PlayerData playerData = dev.twme.worldeditdisplay.player.PlayerData.getPlayerData(player);
        if (playerData == null) return false;
        return playerData.getMultiRegions().containsValue(region);
    }

    protected record Line(Vector3f start, Vector3f end){};
}