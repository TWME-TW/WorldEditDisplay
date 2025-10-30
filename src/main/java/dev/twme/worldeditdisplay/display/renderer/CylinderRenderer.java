package dev.twme.worldeditdisplay.display.renderer;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.CylinderRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * 圓柱體選區渲染器
 * 
 * 渲染邏輯(參考 WorldEditCUI):
 * 1. 渲染頂部和底部圓環
 * 2. 渲染網格線(X 和 Z 方向的垂直面)
 * 3. 渲染中心軸線標記
 * 
 * 實作參考:
 * - RenderCylinderCircles: 圓環渲染
 * - RenderCylinderGrid: 網格線渲染
 * 
 * 所有設定值現在從 PlayerRenderSettings 讀取
 * @version 3.0 (配置整合版本)
 */
public class CylinderRenderer extends RegionRenderer<CylinderRegion> {
    
    public CylinderRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }
    
    @Override
    public void render(CylinderRegion region) {
        clear();
        
        // 判斷是否為多重選區
        boolean isMultiSelection = isMultiSelection(region);
        
        // 檢查中心點是否已定義
        Vector3 center = region.getCenter();
        if (center == null) {
            return;
        }
        
        double radiusX = region.getRadiusX();
        double radiusZ = region.getRadiusZ();
        int minY = region.getMinY();
        int maxY = region.getMaxY();
        
        // WorldEditCUI 中使用方塊中心點 (+0.5) 用於圓環
        double centerXCircle = center.getX() + 0.5;
        double centerZCircle = center.getZ() + 0.5;
        
        // 網格線也使用方塊中心座標 (+0.5)
        double centerXGrid = center.getX() + 0.5;
        double centerZGrid = center.getZ() + 0.5;
        
        // 獲取材質（只有多重選區才使用 CUI 顏色覆寫）
        // CUI 顏色索引對應 Cylinder:
        // - colorIndex 0 (styles[0]): 主要顏色 -> Circle (圓環)
        // - colorIndex 1 (styles[1]): 次要顏色 -> Grid (網格)
        // - colorIndex 2 (styles[2]): 網格顏色 -> Center (中心標記)
        // - colorIndex 3 (styles[3]): 背景顏色 -> (未使用)
        Material circleMaterial = getMaterialWithOverride(region, 0, settings.getCylinderCircleMaterial(), isMultiSelection);
        Material gridMaterial = getMaterialWithOverride(region, 1, settings.getCylinderGridMaterial(), isMultiSelection);
        Material centerMaterial = getMaterialWithOverride(region, 2, settings.getCylinderCenterMaterial(), isMultiSelection);
        Material centerLineMaterial = settings.getCylinderCenterLineMaterial(); // 中心線不使用 CUI 覆寫
        
        // 情況1: 只有中心點 (兩個半徑都為 0)
        if (radiusX == 0 && radiusZ == 0) {
            // 渲染包覆整個方塊的中心標記
            // 傳入方塊的幾何中心座標（角落 + 0.5）並使用 size = 1.03
            org.joml.Vector3f centerPos = new org.joml.Vector3f(
                (float) (center.getX() + 0.5),
                (float) (center.getY() + 0.5),
                (float) (center.getY() + 0.5)
            );
            renderCube(centerPos, 1.03f, centerMaterial, settings.getCylinderCenterThickness());
            
            return;
        }
        
        // 情況2: 其中一個半徑為 0 (渲染長方形網格)
        if (radiusX == 0 || radiusZ == 0) {
            renderRectangularGrid(centerXGrid, centerZGrid, radiusX, radiusZ, minY, maxY, gridMaterial, centerLineMaterial);
            
            // 渲染包覆整個方塊的中心標記
            // 傳入方塊的幾何中心座標（角落 + 0.5）並使用 size = 1.03
            org.joml.Vector3f centerPos = new org.joml.Vector3f(
                (float) (center.getX() + 0.5),
                (float) (center.getY() + 0.5),
                (float) (center.getY() + 0.5)
            );
            renderCube(centerPos, 1.03f, centerMaterial, settings.getCylinderCenterThickness());
            
            return;
        }
        
        // 情況3: 完整圓柱體 (兩個半徑都大於 0)
        // 計算圓柱體高度並決定網格密度
        int height = maxY - minY + 1;
        int gridStep = calculateGridStep(height);
        
        // 1. 渲染每個 Y 層的圓環(使用方塊中心,根據網格密度)
        for (int y = minY; y <= maxY + 1; y += gridStep) {
            // 跳過中心層，稍後單獨渲染
            if (y == center.getY() || y == center.getY() + 1) {
                continue;
            }
            renderCircle(centerXCircle, y, centerZCircle, radiusX, radiusZ, circleMaterial, settings.getCylinderCircleThickness());
        }
        // 確保頂部圓環一定會被渲染
        if ((maxY + 1 - minY) % gridStep != 0 && maxY + 1 != center.getY() && maxY + 1 != center.getY() + 1) {
            renderCircle(centerXCircle, maxY + 1, centerZCircle, radiusX, radiusZ, circleMaterial, settings.getCylinderCircleThickness());
        }
        
        // 1.1 獨立渲染中心圓環（Y 軸方向的中心線）
        if (center.getY() >= minY && center.getY() <= maxY + 1) {
            renderCircle(centerXCircle, center.getY(), centerZCircle, radiusX, radiusZ, centerLineMaterial, settings.getCylinderCenterLineThickness());
        }
        if (center.getY() + 1 >= minY && center.getY() + 1 <= maxY + 1 && center.getY() + 1 != center.getY()) {
            renderCircle(centerXCircle, center.getY() + 1, centerZCircle, radiusX, radiusZ, centerLineMaterial, settings.getCylinderCenterLineThickness());
        }
        
        // 2. 渲染網格線(使用方塊中心座標)
        renderGrid(centerXGrid, centerZGrid, radiusX, radiusZ, minY, maxY + 1, gridMaterial, centerLineMaterial);
        
        // 3. 渲染包覆整個方塊的中心標記
        // 傳入方塊的幾何中心座標（角落 + 0.5）並使用 size = 1.03
        org.joml.Vector3f centerPos = new org.joml.Vector3f(
            (float) (center.getX() + 0.5),
            (float) (center.getY() + 0.5),
            (float) (center.getZ() + 0.5)
        );
        renderCube(centerPos, 1.03f, centerMaterial, settings.getCylinderCenterThickness());
        
    }
    
    /**
     * 渲染長方形網格(當其中一個半徑為 0 時使用)
     * 
     * @param centerX 中心 X 座標
     * @param centerZ 中心 Z 座標
     * @param radiusX X 方向半徑
     * @param radiusZ Z 方向半徑
     * @param minY 最小 Y 座標
     * @param maxY 最大 Y 座標
     * @param gridMaterial 網格材質
     * @param centerLineMaterial 中心線材質
     */
    private void renderRectangularGrid(double centerX, double centerZ,
                                      double radiusX, double radiusZ,
                                      int minY, int maxY,
                                      Material gridMaterial, Material centerLineMaterial) {
        int height = maxY - minY + 1;
        int gridStep = calculateGridStep(height);
        
        // 確定哪個半徑為 0
        if (radiusX == 0) {
            // X 方向為 0,渲染一個在 YZ 平面上的矩形
            double x = centerX;
            double zMin = centerZ - radiusZ;
            double zMax = centerZ + radiusZ;
            
            // 渲染矩形的四條邊
            org.joml.Vector3f v1 = new org.joml.Vector3f((float) x, (float) minY, (float) zMin);
            org.joml.Vector3f v2 = new org.joml.Vector3f((float) x, (float) minY, (float) zMax);
            org.joml.Vector3f v3 = new org.joml.Vector3f((float) x, (float) (maxY + 1), (float) zMax);
            org.joml.Vector3f v4 = new org.joml.Vector3f((float) x, (float) (maxY + 1), (float) zMin);
            
            renderLine(v1, v2, gridMaterial, settings.getCylinderGridThickness());
            renderLine(v2, v3, gridMaterial, settings.getCylinderGridThickness());
            renderLine(v3, v4, gridMaterial, settings.getCylinderGridThickness());
            renderLine(v4, v1, gridMaterial, settings.getCylinderGridThickness());
            
            // 水平網格線 (Z 方向)
            for (int y = minY; y <= maxY + 1; y += gridStep) {
                org.joml.Vector3f start = new org.joml.Vector3f((float) x, (float) y, (float) zMin);
                org.joml.Vector3f end = new org.joml.Vector3f((float) x, (float) y, (float) zMax);
                renderLine(start, end, gridMaterial, settings.getCylinderGridThickness());
            }
            
            // 垂直網格線 (Y 方向，每隔整數 Z 座標繪製一條)
            int posRadiusZ = (int) Math.ceil(radiusZ);
            int negRadiusZ = (int) -Math.ceil(radiusZ);
            for (int tempZ = negRadiusZ; tempZ <= posRadiusZ; tempZ++) {
                double gridZ = centerZ + tempZ;
                
                // 判斷是否為中心線
                boolean isCenterLine = (tempZ == 0);
                Material lineMaterial = isCenterLine ? centerLineMaterial : gridMaterial;
                float lineThickness = isCenterLine ? settings.getCylinderCenterLineThickness() : settings.getCylinderGridThickness();
                
                org.joml.Vector3f start = new org.joml.Vector3f((float) x, (float) minY, (float) gridZ);
                org.joml.Vector3f end = new org.joml.Vector3f((float) x, (float) (maxY + 1), (float) gridZ);
                renderLine(start, end, lineMaterial, lineThickness);
            }
            
        } else if (radiusZ == 0) {
            // Z 方向為 0,渲染一個在 XY 平面上的矩形
            double z = centerZ;
            double xMin = centerX - radiusX;
            double xMax = centerX + radiusX;
            
            // 渲染矩形的四條邊
            org.joml.Vector3f v1 = new org.joml.Vector3f((float) xMin, (float) minY, (float) z);
            org.joml.Vector3f v2 = new org.joml.Vector3f((float) xMax, (float) minY, (float) z);
            org.joml.Vector3f v3 = new org.joml.Vector3f((float) xMax, (float) (maxY + 1), (float) z);
            org.joml.Vector3f v4 = new org.joml.Vector3f((float) xMin, (float) (maxY + 1), (float) z);
            
            renderLine(v1, v2, gridMaterial, settings.getCylinderGridThickness());
            renderLine(v2, v3, gridMaterial, settings.getCylinderGridThickness());
            renderLine(v3, v4, gridMaterial, settings.getCylinderGridThickness());
            renderLine(v4, v1, gridMaterial, settings.getCylinderGridThickness());
            
            // 水平網格線 (X 方向)
            for (int y = minY; y <= maxY + 1; y += gridStep) {
                org.joml.Vector3f start = new org.joml.Vector3f((float) xMin, (float) y, (float) z);
                org.joml.Vector3f end = new org.joml.Vector3f((float) xMax, (float) y, (float) z);
                renderLine(start, end, gridMaterial, settings.getCylinderGridThickness());
            }
            
            // 垂直網格線 (Y 方向，每隔整數 X 座標繪製一條)
            int posRadiusX = (int) Math.ceil(radiusX);
            int negRadiusX = (int) -Math.ceil(radiusX);
            for (int tempX = negRadiusX; tempX <= posRadiusX; tempX++) {
                double gridX = centerX + tempX;
                
                // 判斷是否為中心線
                boolean isCenterLine = (tempX == 0);
                Material lineMaterial = isCenterLine ? centerLineMaterial : gridMaterial;
                float lineThickness = isCenterLine ? settings.getCylinderCenterLineThickness() : settings.getCylinderGridThickness();
                
                org.joml.Vector3f start = new org.joml.Vector3f((float) gridX, (float) minY, (float) z);
                org.joml.Vector3f end = new org.joml.Vector3f((float) gridX, (float) (maxY + 1), (float) z);
                renderLine(start, end, lineMaterial, lineThickness);
            }
        }
    }
    
    /**
     * 根據圓柱體高度計算網格步長
     * 使用高度 / HEIGHT_GRID_DIVISION 的整數值來決定間隔,最小為 1
     * 如果設定了最大間隔 (MAX_GRID_SPACING != -1),則限制步長不超過此值
     * 
     * @param height 圓柱體高度
     * @return 網格步長 (每隔多少層繪製一個圓環)
     */
    private int calculateGridStep(int height) {
        int step = height / settings.getCylinderHeightGridDivision();
        step = Math.max(1, step); // 最小間隔為 1
        
        // 如果設定了最大間隔限制
        if (settings.getCylinderMaxGridSpacing() != -1) {
            step = Math.min(step, settings.getCylinderMaxGridSpacing());
        }
        
        return step;
    }
    
    /**
     * 混合式自適應分段計算
     * 根據橢圓的平均半徑動態計算圓環分段數
     * 
     * 演算法:
     * 1. 基於周長: 確保每段弧長接近目標長度 (TARGET_SEGMENT_LENGTH)
     * 2. 基於半徑: 使用平方根比例提供平滑的非線性增長
     * 3. 取兩者較大值: 確保小圓和大圓都有良好表現
     * 4. 限制範圍: 在 MIN_CIRCLE_SEGMENTS 和 MAX_CIRCLE_SEGMENTS 之間
     * 
     * @param radiusX X 方向半徑
     * @param radiusZ Z 方向半徑
     * @return 圓環分段數
     */
    private int calculateCircleSegments(double radiusX, double radiusZ) {
        double avgRadius = (radiusX + radiusZ) / 2.0;
        
        // 方案 1: 基於周長，目標每段固定長度
        double circumference = 2 * Math.PI * avgRadius;
        int segmentsByLength = (int) Math.ceil(circumference / settings.getCylinderTargetSegmentLength());
        
        // 方案 2: 基於半徑，使用平方根比例
        int segmentsByRadius = (int) (settings.getCylinderMinCircleSegments() + settings.getCylinderSqrtScaleFactor() * Math.sqrt(avgRadius));
        
        // 取兩者較大值，但限制在設定範圍內
        int segments = Math.max(segmentsByLength, segmentsByRadius);
        return Math.max(settings.getCylinderMinCircleSegments(), Math.min(segments, settings.getCylinderMaxCircleSegments()));
    }
    
    /**
     * 根據 X 軸半徑計算網格步長
     * 使用半徑 / RADIUS_GRID_DIVISION 的整數值來決定間隔,最小為 1
     * 如果設定了最大間隔 (MAX_GRID_SPACING != -1),則限制步長不超過此值
     * 
     * @param radiusX X 方向半徑
     * @return X 軸網格步長
     */
    private int calculateXGridStep(double radiusX) {
        int step = (int) (radiusX / settings.getCylinderRadiusGridDivision());
        step = Math.max(1, step);
        
        // 如果設定了最大間隔限制
        if (settings.getCylinderMaxGridSpacing() != -1) {
            step = Math.min(step, settings.getCylinderMaxGridSpacing());
        }
        
        return step;
    }
    
    /**
     * 根據 Z 軸半徑計算網格步長
     * 使用半徑 / RADIUS_GRID_DIVISION 的整數值來決定間隔,最小為 1
     * 如果設定了最大間隔 (MAX_GRID_SPACING != -1),則限制步長不超過此值
     * 
     * @param radiusZ Z 方向半徑
     * @return Z 軸網格步長
     */
    private int calculateZGridStep(double radiusZ) {
        int step = (int) (radiusZ / settings.getCylinderRadiusGridDivision());
        step = Math.max(1, step);
        
        // 如果設定了最大間隔限制
        if (settings.getCylinderMaxGridSpacing() != -1) {
            step = Math.min(step, settings.getCylinderMaxGridSpacing());
        }
        
        return step;
    }
    
    /**
     * 渲染橢圓形圓環
     * 
     * 參考 WorldEditCUI 的 RenderCylinderCircles 實作
     * 
     * @param centerX 中心 X 座標(已加 0.5)
     * @param y Y 座標(高度)
     * @param centerZ 中心 Z 座標(已加 0.5)
     * @param radiusX X 方向半徑
     * @param radiusZ Z 方向半徑
     * @param material 線條材質
     * @param thickness 線條粗細
     */
    private void renderCircle(double centerX, double y, double centerZ, 
                             double radiusX, double radiusZ, Material material, float thickness) {
        // 動態計算分段數
        int segments = calculateCircleSegments(radiusX, radiusZ);
        
        org.joml.Vector3f[] points = new org.joml.Vector3f[segments];
        
        // 計算圓周上的點
        double twoPi = Math.PI * 2;
        for (int i = 0; i < segments; i++) {
            double angle = i * twoPi / segments;
            double x = centerX + radiusX * Math.cos(angle);
            double z = centerZ + radiusZ * Math.sin(angle);
            
            points[i] = new org.joml.Vector3f((float) x, (float) y, (float) z);
        }
        
        // 連接相鄰的點形成圓環
        for (int i = 0; i < segments; i++) {
            org.joml.Vector3f start = points[i];
            org.joml.Vector3f end = points[(i + 1) % segments];
            
            renderLine(start, end, material, thickness);
        }
    }
    
    /**
     * 渲染網格線(參考 WorldEditCUI 的 RenderCylinderGrid 實作)
     * 
     * 網格線邏輯:
     * 1. X 方向: 對於每個 X 值,計算對應的 Z 邊界,繪製垂直矩形
     * 2. Z 方向: 對於每個 Z 值,計算對應的 X 邊界,繪製垂直矩形
     * 3. 中心線獨立渲染: 確保中心線(tempX=0, tempZ=0)一定會被渲染
     * 
     * 關鍵: 
     * - 網格線 X/Z 在方塊中心座標 (+0.5)
     * - 計算邊界時,使用相對於圓環中心的偏移量
     * - 根據半徑大小動態調整網格密度
     * 
     * @param centerX 中心 X 座標(方塊中心,含 +0.5)
     * @param centerZ 中心 Z 座標(方塊中心,含 +0.5)
     * @param radiusX X 方向半徑
     * @param radiusZ Z 方向半徑
     * @param minY 最小 Y 座標
     * @param maxY 最大 Y 座標
     */
    /**
     * 渲染網格線(參考 WorldEditCUI 的 RenderCylinderGrid 實作)
     * 
     * 網格線邏輯:
     * 1. X 方向: 對於每個 X 值,計算對應的 Z 邊界,繪製垂直矩形
     * 2. Z 方向: 對於每個 Z 值,計算對應的 X 邊界,繪製垂直矩形
     * 3. 中心線獨立渲染: 確保中心線(tempX=0, tempZ=0)一定會被渲染
     * 
     * 關鍵: 
     * - 網格線 X/Z 在方塊中心座標 (+0.5)
     * - 計算邊界時,使用相對於圓環中心的偏移量
     * - 根據半徑大小動態調整網格密度
     * 
     * @param centerX 中心 X 座標(方塊中心,含 +0.5)
     * @param centerZ 中心 Z 座標(方塊中心,含 +0.5)
     * @param radiusX X 方向半徑
     * @param radiusZ Z 方向半徑
     * @param minY 最小 Y 座標
     * @param maxY 最大 Y 座標
     * @param gridMaterial 網格材質
     * @param centerLineMaterial 中心線材質
     */
    private void renderGrid(double centerX, double centerZ, 
                           double radiusX, double radiusZ, 
                           int minY, int maxY,
                           Material gridMaterial, Material centerLineMaterial) {
        int posRadiusX = (int) Math.ceil(radiusX);
        int negRadiusX = (int) -Math.ceil(radiusX);
        int posRadiusZ = (int) Math.ceil(radiusZ);
        int negRadiusZ = (int) -Math.ceil(radiusZ);
        
        // 計算 X 和 Z 軸的網格步長
        int xGridStep = calculateXGridStep(radiusX);
        int zGridStep = calculateZGridStep(radiusZ);
        
        // 1. 獨立渲染 X 軸中心線 (tempX = 0)
        renderCenterGridLineX(centerX, centerZ, radiusX, radiusZ, minY, maxY, centerLineMaterial);
        
        // 2. X 方向的其他網格線
        // 對於每個整數偏移量,計算對應的網格線(從方塊中心切過)
        for (double tempX = negRadiusX; tempX <= posRadiusX; tempX += xGridStep) {
            // 跳過中心線（已經獨立渲染）
            if (Math.abs(tempX) < 0.01) {
                continue;
            }
            
            // 計算網格線 X 座標(方塊中心 + 偏移量)
            double gridX = centerX + tempX;
            
            // 計算相對於圓環中心的偏移
            double offsetX = tempX;
            
            // 檢查偏移是否在橢圓範圍內
            double ratio = offsetX / radiusX;
            if (Math.abs(ratio) > 1.0) {
                continue;
            }
            
            // 使用橢圓方程計算對應的 Z 偏移: (x/rx)^2 + (z/rz)^2 = 1
            // => z = rz * cos(asin(x/rx))
            double offsetZ = radiusZ * Math.cos(Math.asin(ratio));
            
            // 計算實際的 Z 座標(中心 ± Z 偏移)
            double gridZPos = centerZ + offsetZ;
            double gridZNeg = centerZ - offsetZ;
            
            // 繪製矩形的四條邊
            renderGridLineWithThickness(gridX, gridZPos, maxY, gridX, gridZNeg, maxY, gridMaterial, settings.getCylinderGridThickness());
            renderGridLineWithThickness(gridX, gridZNeg, maxY, gridX, gridZNeg, minY, gridMaterial, settings.getCylinderGridThickness());
            renderGridLineWithThickness(gridX, gridZNeg, minY, gridX, gridZPos, minY, gridMaterial, settings.getCylinderGridThickness());
            renderGridLineWithThickness(gridX, gridZPos, minY, gridX, gridZPos, maxY, gridMaterial, settings.getCylinderGridThickness());
        }
        
        // 3. 獨立渲染 Z 軸中心線 (tempZ = 0)
        renderCenterGridLineZ(centerX, centerZ, radiusX, radiusZ, minY, maxY, centerLineMaterial);
        
        // 4. Z 方向的其他網格線
        // 對於每個整數偏移量,計算對應的網格線(從方塊中心切過)
        for (double tempZ = negRadiusZ; tempZ <= posRadiusZ; tempZ += zGridStep) {
            // 跳過中心線（已經獨立渲染）
            if (Math.abs(tempZ) < 0.01) {
                continue;
            }
            
            // 計算網格線 Z 座標(方塊中心 + 偏移量)
            double gridZ = centerZ + tempZ;
            
            // 計算相對於圓環中心的偏移
            double offsetZ = tempZ;
            
            // 檢查偏移是否在橢圓範圍內
            double ratio = offsetZ / radiusZ;
            if (Math.abs(ratio) > 1.0) {
                continue;
            }
            
            // 使用橢圓方程計算對應的 X 偏移: x = rx * sin(acos(z/rz))
            double offsetX = radiusX * Math.sin(Math.acos(ratio));
            
            // 計算實際的 X 座標(中心 ± X 偏移)
            double gridXPos = centerX + offsetX;
            double gridXNeg = centerX - offsetX;
            
            // 繪製矩形的四條邊
            renderGridLineWithThickness(gridXPos, gridZ, maxY, gridXNeg, gridZ, maxY, gridMaterial, settings.getCylinderGridThickness());
            renderGridLineWithThickness(gridXNeg, gridZ, maxY, gridXNeg, gridZ, minY, gridMaterial, settings.getCylinderGridThickness());
            renderGridLineWithThickness(gridXNeg, gridZ, minY, gridXPos, gridZ, minY, gridMaterial, settings.getCylinderGridThickness());
            renderGridLineWithThickness(gridXPos, gridZ, minY, gridXPos, gridZ, maxY, gridMaterial, settings.getCylinderGridThickness());
        }
    }
    
    /**
     * 獨立渲染 X 軸中心線 (tempX = 0)
     * 確保無論網格密度如何,中心線都會被渲染
     * 
     * @param centerX 中心 X 座標(方塊中心,含 +0.5)
     * @param centerZ 中心 Z 座標(方塊中心,含 +0.5)
     * @param radiusX X 方向半徑
     * @param radiusZ Z 方向半徑
     * @param minY 最小 Y 座標
     * @param maxY 最大 Y 座標
     * @param centerLineMaterial 中心線材質
     */
    private void renderCenterGridLineX(double centerX, double centerZ,
                                       double radiusX, double radiusZ,
                                       int minY, int maxY,
                                       Material centerLineMaterial) {
        // tempX = 0 表示切過中心點的 X 網格線
        double gridX = centerX; // centerX + 0
        
        // 對於 X = 0 的位置，Z 偏移就是完整的 radiusZ
        double offsetZ = radiusZ;
        
        // 計算實際的 Z 座標(中心 ± Z 偏移)
        double gridZPos = centerZ + offsetZ;
        double gridZNeg = centerZ - offsetZ;
        
        // 使用中心線材質和粗細
        // 繪製矩形的四條邊
        renderGridLineWithThickness(gridX, gridZPos, maxY, gridX, gridZNeg, maxY, centerLineMaterial, settings.getCylinderCenterLineThickness());
        renderGridLineWithThickness(gridX, gridZNeg, maxY, gridX, gridZNeg, minY, centerLineMaterial, settings.getCylinderCenterLineThickness());
        renderGridLineWithThickness(gridX, gridZNeg, minY, gridX, gridZPos, minY, centerLineMaterial, settings.getCylinderCenterLineThickness());
        renderGridLineWithThickness(gridX, gridZPos, minY, gridX, gridZPos, maxY, centerLineMaterial, settings.getCylinderCenterLineThickness());
    }
    
    /**
     * 獨立渲染 Z 軸中心線 (tempZ = 0)
     * 確保無論網格密度如何,中心線都會被渲染
     * 
     * @param centerX 中心 X 座標(方塊中心,含 +0.5)
     * @param centerZ 中心 Z 座標(方塊中心,含 +0.5)
     * @param radiusX X 方向半徑
     * @param radiusZ Z 方向半徑
     * @param minY 最小 Y 座標
     * @param maxY 最大 Y 座標
     * @param centerLineMaterial 中心線材質
     */
    private void renderCenterGridLineZ(double centerX, double centerZ,
                                       double radiusX, double radiusZ,
                                       int minY, int maxY,
                                       Material centerLineMaterial) {
        // tempZ = 0 表示切過中心點的 Z 網格線
        double gridZ = centerZ; // centerZ + 0
        
        // 對於 Z = 0 的位置，X 偏移就是完整的 radiusX
        double offsetX = radiusX;
        
        // 計算實際的 X 座標(中心 ± X 偏移)
        double gridXPos = centerX + offsetX;
        double gridXNeg = centerX - offsetX;
        
        // 使用中心線材質和粗細
        // 繪製矩形的四條邊
        renderGridLineWithThickness(gridXPos, gridZ, maxY, gridXNeg, gridZ, maxY, centerLineMaterial, settings.getCylinderCenterLineThickness());
        renderGridLineWithThickness(gridXNeg, gridZ, maxY, gridXNeg, gridZ, minY, centerLineMaterial, settings.getCylinderCenterLineThickness());
        renderGridLineWithThickness(gridXNeg, gridZ, minY, gridXPos, gridZ, minY, centerLineMaterial, settings.getCylinderCenterLineThickness());
        renderGridLineWithThickness(gridXPos, gridZ, minY, gridXPos, gridZ, maxY, centerLineMaterial, settings.getCylinderCenterLineThickness());
    }
    
    /**
     * 渲染單條網格線(支援自訂粗細和材質)
     * 
     * @param x1 起點 X 座標
     * @param z1 起點 Z 座標
     * @param y1 起點 Y 座標
     * @param x2 終點 X 座標
     * @param z2 終點 Z 座標
     * @param y2 終點 Y 座標
     * @param material 線條材質
     * @param thickness 線條粗細
     */
    private void renderGridLineWithThickness(double x1, double z1, double y1,
                                            double x2, double z2, double y2,
                                            Material material, float thickness) {
        org.joml.Vector3f start = new org.joml.Vector3f((float) x1, (float) y1, (float) z1);
        org.joml.Vector3f end = new org.joml.Vector3f((float) x2, (float) y2, (float) z2);
        renderLine(start, end, material, thickness);
    }
    
    @Override
    public Class<CylinderRegion> getRegionType() {
        return CylinderRegion.class;
    }
}
