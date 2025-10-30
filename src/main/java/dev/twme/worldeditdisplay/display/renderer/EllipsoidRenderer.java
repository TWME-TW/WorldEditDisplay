package dev.twme.worldeditdisplay.display.renderer;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.EllipsoidRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * 橢球體選區渲染器
 * 
 * 渲染邏輯 (參考 WorldEditCUI):
 * 1. 在中心點顯示標記
 * 2. 渲染三個平面的多層圓環網格:
 *    - XZ 平面 (水平切面): 在不同 Y 高度繪製橢圓
 *    - YZ 平面 (側視切面): 在不同 X 位置繪製橢圓
 *    - XY 平面 (正視切面): 在不同 Z 位置繪製橢圓
 * 3. 使用參數化方程計算橢球表面點
 * 
 * 數學原理:
 * - 橢球方程: (x/rx)² + (y/ry)² + (z/rz)² = 1
 * - 在每個軸向切面,計算該高度的橢圓縮放係數
 * 
 * 所有設定值現在從 PlayerRenderSettings 讀取
 * @version 3.0 (配置整合版本)
 */
public class EllipsoidRenderer extends RegionRenderer<EllipsoidRegion> {
    
    // 渲染參數 (參考 WorldEditCUI)
    private static final double TAU = Math.PI * 2.0;                  // 完整圓周
    
    public EllipsoidRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }
    
    @Override
    public void render(EllipsoidRegion region) {
        clear();
        
        // 判斷是否為多重選區
        boolean isMultiSelection = isMultiSelection(region);
        
        // 檢查選區是否完整定義
        if (!region.isDefined()) {
            return;
        }
        
        Vector3 center = region.getCenter();
        Vector3 radii = region.getRadii();
        
        // 轉換為 JOML Vector3f (對齊方塊中心 +0.5)
        org.joml.Vector3f centerPos = new org.joml.Vector3f(
            (float) center.getX() + 0.5f,
            (float) center.getY() + 0.5f,
            (float) center.getZ() + 0.5f
        );
        
        // 獲取材質（只有多重選區才使用 CUI 顏色覆寫）
        // CUI 顏色索引對應 Ellipsoid:
        // - colorIndex 0 (styles[0]): 主要顏色 -> Line (線條)
        // - colorIndex 1 (styles[1]): 次要顏色 -> (未使用)
        // - colorIndex 2 (styles[2]): 網格顏色 -> Center (中心標記)
        // - colorIndex 3 (styles[3]): 背景顏色 -> (未使用)
        Material lineMaterial = getMaterialWithOverride(region, 0, settings.getEllipsoidLineMaterial(), isMultiSelection);
        Material centerMaterial = getMaterialWithOverride(region, 2, settings.getEllipsoidCenterMaterial(), isMultiSelection);
        Material centerLineMaterial = settings.getEllipsoidCenterLineMaterial(); // 中心線不使用 CUI 覆寫
        
        // 1. 渲染中心點標記
        renderCube(centerPos, settings.getEllipsoidCenterMarkerSize(), centerMaterial, settings.getEllipsoidCenterThickness());
        
        // 2. 根據每個軸的半徑分別計算網格密度
        int xStep = calculateGridStep(radii.getX());
        int yStep = calculateGridStep(radii.getY());
        int zStep = calculateGridStep(radii.getZ());
        
        // 3. 渲染三個平面的網格
        renderXZPlane(centerPos, radii, yStep, lineMaterial, centerLineMaterial);  // XZ 平面使用 Y 軸步長
        renderYZPlane(centerPos, radii, xStep, lineMaterial, centerLineMaterial);  // YZ 平面使用 X 軸步長
        renderXYPlane(centerPos, radii, zStep, lineMaterial, centerLineMaterial);  // XY 平面使用 Z 軸步長
    }
    
    /**
     * 根據軸的半徑計算網格步長
     * 使用該軸半徑 / RADIUS_GRID_DIVISION 的整數值來決定間隔,最小為 1
     * 如果設定了最大間隔 (MAX_GRID_SPACING != -1),則限制步長不超過此值
     * 這樣可以確保每個軸都有適當的網格密度
     * 
     * @param radius 該軸的半徑
     * @return 網格步長 (每隔多少層繪製一個圓環)
     */
    private int calculateGridStep(double radius) {
        int step = (int) (radius / settings.getEllipsoidRadiusGridDivision());
        step = Math.max(1, step); // 最小間隔為 1
        
        // 如果設定了最大間隔限制
        if (settings.getEllipsoidMaxGridSpacing() != -1) {
            step = Math.min(step, settings.getEllipsoidMaxGridSpacing());
        }
        
        return step;
    }
    
    /**
     * 混合式自適應分段計算
     * 根據橢圓的兩個半徑動態計算分段數
     * 
     * 演算法:
     * 1. 基於周長: 使用 Ramanujan 近似公式計算橢圓周長,確保每段弧長接近目標長度
     * 2. 基於半徑: 使用平方根比例提供平滑的非線性增長
     * 3. 取兩者較大值: 確保小圓和大圓都有良好表現
     * 4. 限制範圍: 在 MIN_ELLIPSE_SEGMENTS 和 MAX_ELLIPSE_SEGMENTS 之間
     * 
     * @param radius1 第一個半徑
     * @param radius2 第二個半徑
     * @return 橢圓分段數
     */
    private int calculateEllipseSegments(double radius1, double radius2) {
        // 方案 1: 基於橢圓周長（Ramanujan 近似公式）
        double a = Math.max(radius1, radius2);
        double b = Math.min(radius1, radius2);
        double h = Math.pow((a - b) / (a + b), 2);
        double circumference = Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h)));
        int segmentsByLength = (int) Math.ceil(circumference / settings.getEllipsoidTargetSegmentLength());
        
        // 方案 2: 基於平均半徑，使用平方根比例
        double avgRadius = (radius1 + radius2) / 2.0;
        int segmentsByRadius = (int) (settings.getEllipsoidMinSegments() + settings.getEllipsoidSqrtScaleFactor() * Math.sqrt(avgRadius));
        
        // 取兩者較大值，但限制在設定範圍內
        int segments = Math.max(segmentsByLength, segmentsByRadius);
        return Math.max(settings.getEllipsoidMinSegments(), Math.min(segments, settings.getEllipsoidMaxSegments()));
    }
    
    /**
     * 渲染 XZ 平面 (水平切面)
     * 在不同 Y 高度繪製橢圓環
     * 
     * @param center 中心點
     * @param radii 三軸半徑
     * @param step Y 軸步長
     * @param lineMaterial 線條材質
     * @param centerLineMaterial 中心線材質
     */
    private void renderXZPlane(org.joml.Vector3f center, Vector3 radii, int step,
                              Material lineMaterial, Material centerLineMaterial) {
        float rx = (float) radii.getX();
        float ry = (float) radii.getY();
        float rz = (float) radii.getZ();
        
        // 當 Y 半徑過小時,只繪製主軸環
        if (ry < 0.5) {
            drawEllipseXZ(center, rx, ry, rz, 0, centerLineMaterial, settings.getEllipsoidCenterLineThickness());
            return;
        }
        
        int yRad = (int) Math.floor(ry);
        
        // 繪製多層水平橢圓（跳過主軸環）
        for (int yOffset = -yRad; yOffset < yRad; yOffset += step) {
            if (yOffset == 0) continue; // 主軸環單獨繪製
            
            drawEllipseXZ(center, rx, ry, rz, yOffset, lineMaterial, settings.getEllipsoidLineThickness());
        }
        
        // 獨立繪製主軸環 (y = 0, 赤道線) - 使用中心線材質
        drawEllipseXZ(center, rx, ry, rz, 0, centerLineMaterial, settings.getEllipsoidCenterLineThickness());
    }
    
    /**
     * 繪製 XZ 平面上的橢圓 (給定 Y 偏移)
     */
    private void drawEllipseXZ(org.joml.Vector3f center, float rx, float ry, float rz, int yOffset, 
                               Material material, float thickness) {
        // 特殊情況: Y 半徑為 0 或極小,繪製扁平橢圓
        double scaleFactor;
        if (ry < 0.01) {
            scaleFactor = 1.0; // 完整的 XZ 平面橢圓
        } else {
            // 計算該高度的橢圓縮放係數
            // cos(asin(y/ry)) = sqrt(1 - (y/ry)²)
            double yNorm = yOffset / ry;
            if (Math.abs(yNorm) >= 1.0) return; // 超出範圍
            scaleFactor = Math.sqrt(1.0 - yNorm * yNorm);
        }
        
        // 計算當前橢圓的實際半徑並動態決定分段數
        double scaledRx = rx * scaleFactor;
        double scaledRz = rz * scaleFactor;
        int segments = calculateEllipseSegments(scaledRx, scaledRz);
        
        // 繪製橢圓環
        for (int i = 0; i < segments; i++) {
            double theta1 = i * TAU / segments;
            double theta2 = (i + 1) * TAU / segments;
            
            float x1 = (float) (rx * Math.cos(theta1) * scaleFactor);
            float z1 = (float) (rz * Math.sin(theta1) * scaleFactor);
            float x2 = (float) (rx * Math.cos(theta2) * scaleFactor);
            float z2 = (float) (rz * Math.sin(theta2) * scaleFactor);
            
            org.joml.Vector3f p1 = new org.joml.Vector3f(
                center.x + x1,
                center.y + yOffset,
                center.z + z1
            );
            org.joml.Vector3f p2 = new org.joml.Vector3f(
                center.x + x2,
                center.y + yOffset,
                center.z + z2
            );
            
            renderLine(p1, p2, material, thickness);
        }
    }
    
    /**
     * 渲染 YZ 平面 (側視切面)
     * 在不同 X 位置繪製橢圓環
     * 
     * @param center 中心點
     * @param radii 三軸半徑
     * @param step X 軸步長
     * @param lineMaterial 線條材質
     * @param centerLineMaterial 中心線材質
     */
    private void renderYZPlane(org.joml.Vector3f center, Vector3 radii, int step,
                              Material lineMaterial, Material centerLineMaterial) {
        float rx = (float) radii.getX();
        float ry = (float) radii.getY();
        float rz = (float) radii.getZ();
        
        // 當 X 半徑過小時,只繪製主軸環
        if (rx < 0.5) {
            drawEllipseYZ(center, rx, ry, rz, 0, centerLineMaterial, settings.getEllipsoidCenterLineThickness());
            return;
        }
        
        int xRad = (int) Math.floor(rx);
        
        // 繪製多層側視橢圓（跳過主軸環）
        for (int xOffset = -xRad; xOffset < xRad; xOffset += step) {
            if (xOffset == 0) continue; // 主軸環單獨繪製
            
            drawEllipseYZ(center, rx, ry, rz, xOffset, lineMaterial, settings.getEllipsoidLineThickness());
        }
        
        // 獨立繪製主軸環 (x = 0) - 使用中心線材質
        drawEllipseYZ(center, rx, ry, rz, 0, centerLineMaterial, settings.getEllipsoidCenterLineThickness());
    }
    
    /**
     * 繪製 YZ 平面上的橢圓 (給定 X 偏移)
     */
    private void drawEllipseYZ(org.joml.Vector3f center, float rx, float ry, float rz, int xOffset,
                               Material material, float thickness) {
        // 特殊情況: X 半徑為 0 或極小,繪製扁平橢圓
        double scaleFactor;
        if (rx < 0.01) {
            scaleFactor = 1.0; // 完整的 YZ 平面橢圓
        } else {
            // 計算該位置的橢圓縮放係數
            double xNorm = xOffset / rx;
            if (Math.abs(xNorm) >= 1.0) return;
            scaleFactor = Math.sqrt(1.0 - xNorm * xNorm);
        }
        
        // 計算當前橢圓的實際半徑並動態決定分段數
        double scaledRy = ry * scaleFactor;
        double scaledRz = rz * scaleFactor;
        int segments = calculateEllipseSegments(scaledRy, scaledRz);
        
        // 繪製橢圓環
        for (int i = 0; i < segments; i++) {
            double theta1 = i * TAU / segments;
            double theta2 = (i + 1) * TAU / segments;
            
            float y1 = (float) (ry * Math.cos(theta1) * scaleFactor);
            float z1 = (float) (rz * Math.sin(theta1) * scaleFactor);
            float y2 = (float) (ry * Math.cos(theta2) * scaleFactor);
            float z2 = (float) (rz * Math.sin(theta2) * scaleFactor);
            
            org.joml.Vector3f p1 = new org.joml.Vector3f(
                center.x + xOffset,
                center.y + y1,
                center.z + z1
            );
            org.joml.Vector3f p2 = new org.joml.Vector3f(
                center.x + xOffset,
                center.y + y2,
                center.z + z2
            );
            
            renderLine(p1, p2, material, thickness);
        }
    }
    
    /**
     * 渲染 XY 平面 (正視切面)
     * 在不同 Z 位置繪製橢圓環
     * 
     * @param center 中心點
     * @param radii 三軸半徑
     * @param step Z 軸步長
     * @param lineMaterial 線條材質
     * @param centerLineMaterial 中心線材質
     */
    private void renderXYPlane(org.joml.Vector3f center, Vector3 radii, int step,
                              Material lineMaterial, Material centerLineMaterial) {
        float rx = (float) radii.getX();
        float ry = (float) radii.getY();
        float rz = (float) radii.getZ();
        
        // 當 Z 半徑過小時,只繪製主軸環
        if (rz < 0.5) {
            drawEllipseXY(center, rx, ry, rz, 0, centerLineMaterial, settings.getEllipsoidCenterLineThickness());
            return;
        }
        
        int zRad = (int) Math.floor(rz);
        
        // 繪製多層正視橢圓（跳過主軸環）
        for (int zOffset = -zRad; zOffset < zRad; zOffset += step) {
            if (zOffset == 0) continue; // 主軸環單獨繪製
            
            drawEllipseXY(center, rx, ry, rz, zOffset, lineMaterial, settings.getEllipsoidLineThickness());
        }
        
        // 獨立繪製主軸環 (z = 0) - 使用中心線材質
        drawEllipseXY(center, rx, ry, rz, 0, centerLineMaterial, settings.getEllipsoidCenterLineThickness());
    }
    
    /**
     * 繪製 XY 平面上的橢圓 (給定 Z 偏移)
     */
    private void drawEllipseXY(org.joml.Vector3f center, float rx, float ry, float rz, int zOffset,
                               Material material, float thickness) {
        // 特殊情況: Z 半徑為 0 或極小,繪製扁平橢圓
        double scaleFactor;
        if (rz < 0.01) {
            scaleFactor = 1.0; // 完整的 XY 平面橢圓
        } else {
            // 計算該位置的橢圓縮放係數
            double zNorm = zOffset / rz;
            if (Math.abs(zNorm) >= 1.0) return;
            scaleFactor = Math.sqrt(1.0 - zNorm * zNorm);
        }
        
        // 計算當前橢圓的實際半徑並動態決定分段數
        double scaledRx = rx * scaleFactor;
        double scaledRy = ry * scaleFactor;
        int segments = calculateEllipseSegments(scaledRx, scaledRy);
        
        // 繪製橢圓環
        for (int i = 0; i < segments; i++) {
            double theta1 = i * TAU / segments;
            double theta2 = (i + 1) * TAU / segments;
            
            float x1 = (float) (rx * Math.cos(theta1) * scaleFactor);
            float y1 = (float) (ry * Math.sin(theta1) * scaleFactor);
            float x2 = (float) (rx * Math.cos(theta2) * scaleFactor);
            float y2 = (float) (ry * Math.sin(theta2) * scaleFactor);
            
            org.joml.Vector3f p1 = new org.joml.Vector3f(
                center.x + x1,
                center.y + y1,
                center.z + zOffset
            );
            org.joml.Vector3f p2 = new org.joml.Vector3f(
                center.x + x2,
                center.y + y2,
                center.z + zOffset
            );
            
            renderLine(p1, p2, material, thickness);
        }
    }
    
    @Override
    public Class<EllipsoidRegion> getRegionType() {
        return EllipsoidRegion.class;
    }
}
