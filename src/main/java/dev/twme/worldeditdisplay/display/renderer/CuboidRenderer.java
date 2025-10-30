package dev.twme.worldeditdisplay.display.renderer;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.BoundingBox;
import dev.twme.worldeditdisplay.region.CuboidRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * 長方體選區渲染器
 * 
 * 渲染邏輯（完全模擬 WorldEditCUI）:
 * 1. Box: 在邊界顯示邊緣線（12條邊）
 * 2. Grid: 在選區表面顯示網格線（6個面）
 * 3. Point1: 第一個選取點標記
 * 4. Point2: 第二個選取點標記
 * 
 * 使用 ItemDisplay 來渲染，以獲得更好的視覺效果
 * 所有設定值現在從 PlayerRenderSettings 讀取
 * 
 * @author TWME-TW
 * @version 3.0 (配置整合版本)
 */
public class CuboidRenderer extends RegionRenderer<CuboidRegion> {
    
    private static final double MIN_SPACING = 1.0;
    private static final double SKIP_THRESHOLD = 0.25;
    
    // 配置
    private boolean renderGrid = true;
    private boolean renderBox = true;
    
    public CuboidRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }
    
    @Override
    public void render(CuboidRegion region) {
        // 先清除舊的渲染實體
        clear();
        
        // 判斷是否為多重選區
        boolean isMultiSelection = isMultiSelection(region);
        
        // 獲取兩個選取點
        Vector3 point1 = region.getPoint1();
        Vector3 point2 = region.getPoint2();
        
        // 至少需要一個點才能渲染
        if (point1 == null && point2 == null) {
            return;
        }
        
        // 獲取材質（只有多重選區才使用 CUI 顏色覆寫）
        Material point1Material = getMaterialWithOverride(region, 2, settings.getCuboidPoint1Material(), isMultiSelection);
        Material point2Material = getMaterialWithOverride(region, 3, settings.getCuboidPoint2Material(), isMultiSelection);
        Material boxMaterial = getMaterialWithOverride(region, 0, settings.getCuboidEdgeMaterial(), isMultiSelection);
        Material gridMaterial = getMaterialWithOverride(region, 1, settings.getCuboidGridMaterial(), isMultiSelection);
        
        // 渲染選取點標記
        if (point1 != null) {
            renderPointMarker(point1, point1Material, settings.getCuboidEdgeThickness());
        }
        
        if (point2 != null) {
            renderPointMarker(point2, point2Material, settings.getCuboidEdgeThickness());
        }
        
        // 只有當兩個點都存在時，才渲染選區
        if (region.isDefined()) {
            // 獲取邊界框
            BoundingBox regionBox = region.getBoundingBox();
            if (regionBox == null) {
                return;
            }
            
            // 獲取最小和最大點座標
            Vector3 min = regionBox.getMin();
            Vector3 max = regionBox.getMax();
            
            // 注意：為了框住整個方塊，最大點的座標需要 + 1
            double minX = min.getX();
            double minY = min.getY();
            double minZ = min.getZ();
            double maxX = max.getX() + 1.0;
            double maxY = max.getY() + 1.0;
            double maxZ = max.getZ() + 1.0;
            
            // 渲染邊框
            if (renderBox) {
                renderBoxFrame(minX, minY, minZ, maxX, maxY, maxZ, 
                    boxMaterial, 
                    settings.getCuboidEdgeThickness());
            }
            
            // 渲染網格
            if (renderGrid) {
                renderGrid(minX, minY, minZ, maxX, maxY, maxZ, region, gridMaterial);
            }
        } else {
            // 只有一個點
        }
    }
    
    /**
     * 渲染網格(只在選區的 6 個表面)
     * 
     * 使用 Region 設定的 gridSpacing 來控制網格間距
     * 如果 gridSpacing <= 0，則根據選區大小自動計算間距
     * 
     * @param x1 最小 X
     * @param y1 最小 Y
     * @param z1 最小 Z
     * @param x2 最大 X
     * @param y2 最大 Y
     * @param z2 最大 Z
     * @param region 選區
     * @param gridMaterial 網格材質
     */
    private void renderGrid(double x1, double y1, double z1, double x2, double y2, double z2, 
                           CuboidRegion region, Material gridMaterial) {
        // 計算選區尺寸
        double sizeX = x2 - x1;
        double sizeY = y2 - y1;
        double sizeZ = z2 - z1;
        
        // 獲取 Region 的 gridSpacing 設定
        double gridSpacing = region.getGridSpacing();
        
        double spacingX, spacingY, spacingZ;
        
        if (gridSpacing > 0) {
            // 使用 CUI 設定的網格間距 (例如: +grid|10.0|cull)
            spacingX = spacingY = spacingZ = gridSpacing;
        } else {
            // 自動計算網格間距（使用玩家設定）
            int gridDivision = settings.getCuboidHeightGridDivision();
            int maxGridSpacing = settings.getCuboidMaxGridSpacing();
            
            spacingX = Math.max(MIN_SPACING, (int)(sizeX / gridDivision));
            spacingY = Math.max(MIN_SPACING, (int)(sizeY / gridDivision));
            spacingZ = Math.max(MIN_SPACING, (int)(sizeZ / gridDivision));
            
            // 如果設定了最大間隔限制,則限制間距不超過此值
            if (maxGridSpacing != -1) {
                spacingX = Math.min(spacingX, maxGridSpacing);
                spacingY = Math.min(spacingY, maxGridSpacing);
                spacingZ = Math.min(spacingZ, maxGridSpacing);
            }
        }
        
        // 如果選區太小，不渲染網格
        if (sizeX < MIN_SPACING && sizeY < MIN_SPACING && sizeZ < MIN_SPACING) {
            return;
        }
        
        // ========== 渲染 6 個表面的網格 ==========
        
        // 1. 底面 (Y = y1) - XZ 平面
        renderXZPlane(x1, y1, z1, x2, z2, spacingX, spacingZ, gridMaterial);
        
        // 2. 頂面 (Y = y2) - XZ 平面
        renderXZPlane(x1, y2, z1, x2, z2, spacingX, spacingZ, gridMaterial);
        
        // 3. 前面 (Z = z1) - XY 平面
        renderXYPlane(x1, y1, z1, x2, y2, spacingX, spacingY, gridMaterial);
        
        // 4. 後面 (Z = z2) - XY 平面
        renderXYPlane(x1, y1, z2, x2, y2, spacingX, spacingY, gridMaterial);
        
        // 5. 左面 (X = x1) - YZ 平面
        renderYZPlane(x1, y1, z1, y2, z2, spacingY, spacingZ, gridMaterial);
        
        // 6. 右面 (X = x2) - YZ 平面
        renderYZPlane(x2, y1, z1, y2, z2, spacingY, spacingZ, gridMaterial);
    }
    
    /**
     * 渲染 XZ 平面的網格(水平面,例如頂面和底面)
     * 
     * @param x1 最小 X
     * @param y 固定 Y(平面高度)
     * @param z1 最小 Z
     * @param x2 最大 X
     * @param z2 最大 Z
     * @param spacingX X 方向的間距
     * @param spacingZ Z 方向的間距
     * @param material 網格材質
     */
    private void renderXZPlane(double x1, double y, double z1, double x2, double z2, 
                              double spacingX, double spacingZ, Material material) {
        // X 方向的線(平行於 X 軸)
        for (double z = z1; z <= z2; z += spacingZ) {
            if (z > z1 && z2 - z < SKIP_THRESHOLD) {
                continue;
            }
            org.joml.Vector3f start = new org.joml.Vector3f((float) x1, (float) y, (float) z);
            org.joml.Vector3f end = new org.joml.Vector3f((float) x2, (float) y, (float) z);
            renderLine(start, end, material, settings.getCuboidGridThickness());
        }
        
        // Z 方向的線(平行於 Z 軸)
        for (double x = x1; x <= x2; x += spacingX) {
            if (x > x1 && x2 - x < SKIP_THRESHOLD) {
                continue;
            }
            org.joml.Vector3f start = new org.joml.Vector3f((float) x, (float) y, (float) z1);
            org.joml.Vector3f end = new org.joml.Vector3f((float) x, (float) y, (float) z2);
            renderLine(start, end, material, settings.getCuboidGridThickness());
        }
    }
    
    /**
     * 渲染 XY 平面的網格(垂直面,例如前面和後面)
     * 
     * @param x1 最小 X
     * @param y1 最小 Y
     * @param z 固定 Z(平面位置)
     * @param x2 最大 X
     * @param y2 最大 Y
     * @param spacingX X 方向的間距
     * @param spacingY Y 方向的間距
     * @param material 網格材質
     */
    private void renderXYPlane(double x1, double y1, double z, double x2, double y2, 
                              double spacingX, double spacingY, Material material) {
        // X 方向的線(水平線,平行於 X 軸)
        for (double y = y1; y <= y2; y += spacingY) {
            if (y > y1 && y2 - y < SKIP_THRESHOLD) {
                continue;
            }
            org.joml.Vector3f start = new org.joml.Vector3f((float) x1, (float) y, (float) z);
            org.joml.Vector3f end = new org.joml.Vector3f((float) x2, (float) y, (float) z);
            renderLine(start, end, material, settings.getCuboidGridThickness());
        }
        
        // Y 方向的線(垂直線,平行於 Y 軸)
        for (double x = x1; x <= x2; x += spacingX) {
            if (x > x1 && x2 - x < SKIP_THRESHOLD) {
                continue;
            }
            org.joml.Vector3f start = new org.joml.Vector3f((float) x, (float) y1, (float) z);
            org.joml.Vector3f end = new org.joml.Vector3f((float) x, (float) y2, (float) z);
            renderLine(start, end, material, settings.getCuboidGridThickness());
        }
    }
    
    /**
     * 渲染 YZ 平面的網格(垂直面,例如左面和右面)
     * 
     * @param x 固定 X(平面位置)
     * @param y1 最小 Y
     * @param z1 最小 Z
     * @param y2 最大 Y
     * @param z2 最大 Z
     * @param spacingY Y 方向的間距
     * @param spacingZ Z 方向的間距
     * @param material 網格材質
     */
    private void renderYZPlane(double x, double y1, double z1, double y2, double z2, 
                              double spacingY, double spacingZ, Material material) {
        // Y 方向的線(垂直線,平行於 Y 軸)
        for (double z = z1; z <= z2; z += spacingZ) {
            if (z > z1 && z2 - z < SKIP_THRESHOLD) {
                continue;
            }
            org.joml.Vector3f start = new org.joml.Vector3f((float) x, (float) y1, (float) z);
            org.joml.Vector3f end = new org.joml.Vector3f((float) x, (float) y2, (float) z);
            renderLine(start, end, material, settings.getCuboidGridThickness());
        }
        
        // Z 方向的線(水平線,平行於 Z 軸)
        for (double y = y1; y <= y2; y += spacingY) {
            if (y > y1 && y2 - y < SKIP_THRESHOLD) {
                continue;
            }
            org.joml.Vector3f start = new org.joml.Vector3f((float) x, (float) y, (float) z1);
            org.joml.Vector3f end = new org.joml.Vector3f((float) x, (float) y, (float) z2);
            renderLine(start, end, material, settings.getCuboidGridThickness());
        }
    }
    
    /**
     * 設定是否渲染網格
     * 
     * @param render 是否渲染
     */
    public void setRenderGrid(boolean render) {
        this.renderGrid = render;
    }
    
    /**
     * 設定是否渲染邊框
     * 
     * @param render 是否渲染
     */
    public void setRenderBox(boolean render) {
        this.renderBox = render;
    }


    
    @Override
    public Class<CuboidRegion> getRegionType() {
        return CuboidRegion.class;
    }
}
