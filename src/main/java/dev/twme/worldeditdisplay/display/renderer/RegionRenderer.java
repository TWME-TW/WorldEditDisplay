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
import com.github.retrooper.packetevents.util.Vector3f;

import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.Region;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;

/**
 * 選區渲染器抽象基類
 * 
 * 提供通用的渲染功能:
 * - 實體池管理
 * - 玩家可見性控制
 * - 變換(平移、縮放、旋轉)
 * - 清理機制
 * 
 * @param <T> 對應的選區類型
 */
public abstract class RegionRenderer<T extends Region> {
    
    protected final WorldEditDisplay plugin;
    protected final Player player;
    protected final UUID playerUUID;
    protected final PlayerRenderSettings settings;
    
    // 顯示實體池
    protected final List<WrapperEntity> entities;
    
    // 渲染配置
    protected RenderConfig config;
    
    /**
     * 建構子
     * 
     * @param plugin 插件實例
     * @param player 目標玩家
     * @param settings 玩家渲染設定
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
     * 渲染選區
     * 
     * @param region 要渲染的選區
     */
    public abstract void render(T region);
    
    /**
     * 獲取此渲染器支援的選區類型
     * 
     * @return 選區類別
     */
    public abstract Class<T> getRegionType();
    
    /**
     * 清除所有渲染實體
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
     * 創建並註冊一個新的顯示實體
     * 
     * @param entityType 實體類型
     * @param location 生成位置
     * @return WrapperEntity 實例
     */
    protected WrapperEntity createEntity(EntityType entityType, Location location) {
        WrapperEntity entity = new WrapperEntity(entityType);
        
        // 生成實體
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(location));
        
        // 加入實體池
        entities.add(entity);
        
        return entity;
    }
    
    /**
     * 設定顯示實體的基礎屬性
     * 
     * @param entity 顯示實體
     */
    protected void setupDisplayMeta(WrapperEntity entity) {
        if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta)) {
            return;
        }
        
        AbstractDisplayMeta meta = (AbstractDisplayMeta) entity.getEntityMeta();
        // 設定可見距離
        meta.setViewRange(config.getViewRange());
        
        // 設定亮度(始終發光)
        if (config.isAlwaysBright()) {
            meta.setBrightnessOverride(config.getFullBrightness());
        }
        
        // 設定陰影
        meta.setShadowRadius(config.getShadowRadius());
        meta.setShadowStrength(config.getShadowStrength());
        
        // 設定發光顏色
        if (config.hasGlowColor()) {
            meta.setGlowColorOverride(config.getGlowColor());
        }
    }
    
    /**
     * 設定實體的變換(縮放)
     * 
     * @param entity 顯示實體
     * @param scale 縮放向量
     */
    protected void setScale(WrapperEntity entity, Vector3f scale) {
        if (entity.getEntityMeta() instanceof AbstractDisplayMeta) {
            AbstractDisplayMeta meta = (AbstractDisplayMeta) entity.getEntityMeta();
            meta.setScale(scale);
        }
    }
    
    /**
     * 設定實體的變換(平移)
     * 
     * @param entity 顯示實體
     * @param translation 平移向量
     */
    protected void setTranslation(WrapperEntity entity, Vector3f translation) {
        if (entity.getEntityMeta() instanceof AbstractDisplayMeta) {
            AbstractDisplayMeta meta = (AbstractDisplayMeta) entity.getEntityMeta();
            meta.setTranslation(translation);
        }
    }
    
    /**
     * 設定實體的變換(旋轉)
     * 
     * @param entity 顯示實體
     * @param rotation 旋轉四元數
     */
    protected void setRotation(WrapperEntity entity, Quaternion4f rotation) {
        if (entity.getEntityMeta() instanceof AbstractDisplayMeta) {
            AbstractDisplayMeta meta = (AbstractDisplayMeta) entity.getEntityMeta();
            meta.setLeftRotation(rotation);
        }
    }
    
    /**
     * 批量更新實體 metadata
     * 用於避免多次發送封包
     * 
     * @param entity 顯示實體
     * @param updater 更新操作
     */
    protected void batchUpdate(WrapperEntity entity, Runnable updater) {
        if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta)) {
            updater.run();
            return;
        }
        
        AbstractDisplayMeta meta = (AbstractDisplayMeta) entity.getEntityMeta();
        
        // 暫時關閉自動通知
        meta.getMetadata().setNotifyAboutChanges(false);
        
        // 執行更新
        updater.run();
        
        // 重新啟用並手動發送
        meta.getMetadata().setNotifyAboutChanges(true);
        entity.sendPacketToViewers(meta.createPacket());
    }
    
    /**
     * 計算兩點之間的插值點
     * 
     * @param start 起點
     * @param end 終點
     * @param segments 分段數量
     * @return 插值點列表(不包括起點和終點)
     */
    protected List<Location> interpolate(Location start, Location end, int segments) {
        List<Location> points = new ArrayList<>();
        
        if (segments <= 0) {
            return points;
        }
        
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
    
    /**
     * 將世界坐標轉換為 Bukkit Location
     * 
     * @param x X 坐標
     * @param y Y 坐標
     * @param z Z 坐標
     * @return Location
     */
    protected Location toLocation(double x, double y, double z) {
        return new Location(player.getWorld(), x, y, z);
    }
    
    /**
     * 渲染一條線段
     * 
     * 使用 ItemDisplay 實體來渲染一條從起點到終點的線段。
     * 此方法會自動計算線段的長度、方向和位置，並創建一個適當縮放和旋轉的實體。
     * 
     * @param start 起點座標 (JOML Vector3f)
     * @param end 終點座標 (JOML Vector3f)
     * @param material 線條使用的材質
     * @param thickness 線條粗細
     */
    protected void renderLine(org.joml.Vector3f start, org.joml.Vector3f end, Material material, float thickness) {
        // 1. 計算線條的長度和中點
        float length = start.distance(end) + thickness;
        org.joml.Vector3f midpoint = new org.joml.Vector3f(
                (start.x + end.x) / 2,
                (start.y + end.y) / 2,
                (start.z + end.z) / 2
        );

        // 2. 在玩家腳下生成實體
        // 注意：我們需要一個 "乾淨" 的 Location (yaw/pitch = 0)
        Location spawnLoc = new Location(
                player.getWorld(), 
                player.getLocation().x(), 
                player.getLocation().y(), 
                player.getLocation().z()
        );


        WrapperEntity entity = createEntity(EntityTypes.ITEM_DISPLAY, spawnLoc);

        // 3. 設置實體的元數據
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();

        // 4. 計算從玩家位置(生成點)到線條中點(視覺目標點)的偏移量
        org.joml.Vector3f playerPos = new org.joml.Vector3f(
                (float) spawnLoc.getX(),
                (float) spawnLoc.getY(),
                (float) spawnLoc.getZ()
        );

        // 這是唯一需要的 Translation
        org.joml.Vector3f translation = new org.joml.Vector3f(midpoint).sub(playerPos);

        // 5. 計算方向向量用於旋轉
        org.joml.Vector3f direction = new org.joml.Vector3f(end).sub(start).normalize();

        // 6. 設置物品和顯示屬性
        meta.setItem(SpigotConversionUtil.fromBukkitItemStack(new ItemStack(material)));
        meta.setDisplayType(ItemDisplayMeta.DisplayType.NONE);

        // 7. 設置縮放 (沿 Z 軸縮放)
        meta.setScale(new Vector3f(thickness, thickness, length));

        // 8. 計算旋轉四元數
        // 因為我們沿 Z 軸縮放 (length)，所以我們的預設方向必須是 Z 軸
        org.joml.Vector3f defaultDir = new org.joml.Vector3f(0, 0, 1);
        org.joml.Quaternionf rotation = new org.joml.Quaternionf();
        rotation.rotationTo(defaultDir, direction);

        meta.setLeftRotation(new Quaternion4f(rotation.x, rotation.y, rotation.z, rotation.w));

        // 9. 設置最終的平移
        meta.setTranslation(new Vector3f(translation.x, translation.y, translation.z));
        
        // 10. 應用渲染配置
        setupDisplayMeta(entity);
    
        // 11. 添加觀察者
        entity.addViewer(playerUUID);
    }
    
    /**
     * 渲染一個立方體標記
     * 
     * @param center 中心點座標 (JOML Vector3f)
     * @param size 立方體大小
     * @param material 材質
     * @param thickness 線條粗細
     */
    protected void renderCube(org.joml.Vector3f center, float size, Material material, float thickness) {
        // 計算以 center 為中心的方塊的邊界
        float halfSize = size / 2.0f;
        double minX = center.x - halfSize;
        double minY = center.y - halfSize;
        double minZ = center.z - halfSize;
        double maxX = center.x + halfSize;
        double maxY = center.y + halfSize;
        double maxZ = center.z + halfSize;
        
        // 使用 renderBoxFrame 渲染方塊邊框
        renderBoxFrame(minX, minY, minZ, maxX, maxY, maxZ, material, thickness);
    }
    
    /**
     * 渲染一個方塊的邊框（使用 12 條線）
     * 
     * @param minX 最小 X 座標
     * @param minY 最小 Y 座標
     * @param minZ 最小 Z 座標
     * @param maxX 最大 X 座標
     * @param maxY 最大 Y 座標
     * @param maxZ 最大 Z 座標
     * @param material 線條材質
     * @param thickness 線條粗細
     */
    protected void renderBoxFrame(double minX, double minY, double minZ, 
                                  double maxX, double maxY, double maxZ,
                                  Material material, float thickness) {
        // 定義8個頂點
        org.joml.Vector3f v000 = new org.joml.Vector3f((float) minX, (float) minY, (float) minZ);
        org.joml.Vector3f v001 = new org.joml.Vector3f((float) minX, (float) minY, (float) maxZ);
        org.joml.Vector3f v010 = new org.joml.Vector3f((float) minX, (float) maxY, (float) minZ);
        org.joml.Vector3f v011 = new org.joml.Vector3f((float) minX, (float) maxY, (float) maxZ);
        org.joml.Vector3f v100 = new org.joml.Vector3f((float) maxX, (float) minY, (float) minZ);
        org.joml.Vector3f v101 = new org.joml.Vector3f((float) maxX, (float) minY, (float) maxZ);
        org.joml.Vector3f v110 = new org.joml.Vector3f((float) maxX, (float) maxY, (float) minZ);
        org.joml.Vector3f v111 = new org.joml.Vector3f((float) maxX, (float) maxY, (float) maxZ);
        
        // 渲染12條邊
        
        // 底面4條邊 (Y = minY)
        renderLine(v000, v001, material, thickness); // Z方向
        renderLine(v000, v100, material, thickness); // X方向
        renderLine(v001, v101, material, thickness); // X方向
        renderLine(v100, v101, material, thickness); // Z方向
        
        // 頂面4條邊 (Y = maxY)
        renderLine(v010, v011, material, thickness); // Z方向
        renderLine(v010, v110, material, thickness); // X方向
        renderLine(v011, v111, material, thickness); // X方向
        renderLine(v110, v111, material, thickness); // Z方向
        
        // 4條垂直邊 (Y方向)
        renderLine(v000, v010, material, thickness); // 左前
        renderLine(v001, v011, material, thickness); // 左後
        renderLine(v100, v110, material, thickness); // 右前
        renderLine(v101, v111, material, thickness); // 右後
    }
    
    /**
     * 渲染選取點標記（帶有 padding 的方塊）
     * 
     * @param point 點座標
     * @param material 材質
     * @param thickness 線條粗細
     */
    protected void renderPointMarker(dev.twme.worldeditdisplay.region.Vector3 point, Material material, float thickness) {
        final double PADDING = 0.03;
        double minX = point.getX() - PADDING;
        double minY = point.getY() - PADDING;
        double minZ = point.getZ() - PADDING;
        double maxX = point.getX() + 1.0 + PADDING;
        double maxY = point.getY() + 1.0 + PADDING;
        double maxZ = point.getZ() + 1.0 + PADDING;
        
        renderBoxFrame(minX, minY, minZ, maxX, maxY, maxZ, material, thickness);
    }
    
    /**
     * 設定渲染配置
     * 
     * @param config 渲染配置
     */
    public void setConfig(RenderConfig config) {
        this.config = config;
    }
    
    /**
     * 獲取當前實體數量
     * 
     * @return 實體數量
     */
    public int getEntityCount() {
        return entities.size();
    }
    
    /**
     * 獲取玩家
     * 
     * @return 玩家實例
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * 獲取材質，優先使用 CUI 顏色覆寫
     * 
     * 注意：只有多重選區才會使用顏色覆寫，一般選區永遠使用預設材質
     * 
     * 此方法實現了材質選擇的優先順序：
     * - 多重選區：CUI覆寫 > 預設材質
     * - 一般選區：預設材質（忽略 CUI 覆寫）
     * 
     * @param region 選區（必須有 getColorMaterial 方法）
     * @param colorIndex CUI 顏色索引 (0-3)，對應不同的渲染元件
     * @param defaultMaterial 預設材質（玩家設定或伺服器預設）
     * @param isMultiSelection 是否為多重選區
     * @return 最終使用的材質
     */
    protected Material getMaterialWithOverride(Region region, int colorIndex, 
                                              Material defaultMaterial, boolean isMultiSelection) {
        // 只有多重選區才使用顏色覆寫
        if (!isMultiSelection) {
            return defaultMaterial;
        }
        
        Material override = region.getColorMaterial(colorIndex);
        return override != null ? override : defaultMaterial;
    }
    
    /**
     * 檢查選區是否為多重選區
     * 
     * 透過 PlayerData 檢查該選區是否在多重選區映射中
     * 
     * @param region 要檢查的選區
     * @return 是否為多重選區
     */
    protected boolean isMultiSelection(Region region) {
        if (player == null) {
            return false;
        }
        
        dev.twme.worldeditdisplay.player.PlayerData playerData =
            dev.twme.worldeditdisplay.player.PlayerData.getPlayerData(player);
        
        if (playerData == null) {
            return false;
        }
        
        // 檢查該選區是否存在於多重選區映射中
        return playerData.getMultiRegions().containsValue(region);
    }
}
