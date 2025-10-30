package dev.twme.worldeditdisplay.display.renderer;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.PolygonRegion;
import dev.twme.worldeditdisplay.region.Vector2;

/**
 * 多邊形選區渲染器
 * 
 * 渲染邏輯(符合 WorldEditCUI 顯示方式):
 * 1. 在每個整數 Y 層渲染多邊形邊框 (從 minY 到 maxY+1)
 * 2. 渲染垂直連接線 (從每個頂點的底部到頂部)
 * 3. 渲染頂點標記 (從底部到頂部的長方體邊框)
 * 
 * 採用分層渲染方式:
 * - 水平網格: 每個 Y 層都有一個完整的多邊形邊框
 * - 垂直邊: 從 minY 到 maxY+1,連接所有 Y 層
 * - 頂點標記: 垂直長方體邊框標記每個頂點位置
 * 
 * 所有設定值現在從 PlayerRenderSettings 讀取
 * @version 3.0 (配置整合版本)
 */
public class PolygonRenderer extends RegionRenderer<PolygonRegion> {
    
    public PolygonRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }
    
    @Override
    public void render(PolygonRegion region) {
        clear();
        
        // 判斷是否為多重選區
        boolean isMultiSelection = isMultiSelection(region);
        
        // 檢查選區是否有效
        if (!region.isDefined()) {
            return;
        }
        
        List<Vector2> points = region.getPoints();
        int minY = region.getMinY();
        int maxY = region.getMaxY();
        
        // 過濾掉 null 點
        List<Vector2> validPoints = points.stream()
            .filter(p -> p != null)
            .toList();
        
        if (validPoints.isEmpty()) {
            return;
        }
        
        // 如果只有一個點,只渲染頂點標記
        if (validPoints.size() == 1) {
            // 獲取材質（只有多重選區才使用 CUI 顏色覆寫）
            // CUI 顏色索引對應 Polygon:
            // - colorIndex 0 (styles[0]): 主要顏色 -> Edge (邊框)
            // - colorIndex 1 (styles[1]): 次要顏色 -> (未使用)
            // - colorIndex 2 (styles[2]): 網格顏色 -> Vertex (頂點)
            // - colorIndex 3 (styles[3]): 背景顏色 -> (未使用)
            Material vertexMaterial = getMaterialWithOverride(region, 2, settings.getPolygonVertexMaterial(), isMultiSelection);
            
            renderVertexMarkers(validPoints, minY, maxY, vertexMaterial);
            return;
        }
        
        // 計算高度並決定網格密度
        int height = maxY - minY + 1;
        int gridStep = calculateGridStep(height);
        
        // 獲取材質（只有多重選區才使用 CUI 顏色覆寫）
        Material edgeMaterial = getMaterialWithOverride(region, 0, settings.getPolygonEdgeMaterial(), isMultiSelection);
        Material verticalMaterial = settings.getPolygonVerticalMaterial(); // 垂直線不使用 CUI 覆寫
        Material vertexMaterial = getMaterialWithOverride(region, 2, settings.getPolygonVertexMaterial(), isMultiSelection);
        
        // 1. 渲染多邊形邊框(根據網格密度)
        for (int y = minY; y <= maxY + 1; y += gridStep) {
            renderPolygonEdges(validPoints, y, edgeMaterial, settings.getPolygonEdgeThickness());
        }
        // 確保頂部邊框一定會被渲染
        if ((maxY + 1 - minY) % gridStep != 0) {
            renderPolygonEdges(validPoints, maxY + 1, edgeMaterial, settings.getPolygonEdgeThickness());
        }
        
        // 2. 渲染垂直連接線 (從每個頂點的底部到頂部)
        renderVerticalEdges(validPoints, minY, maxY, verticalMaterial);
        
        // 3. 渲染頂點標記
        renderVertexMarkers(validPoints, minY, maxY, vertexMaterial);
    }
    
    /**
     * 根據多邊形高度計算網格步長
     * 使用高度 / HEIGHT_GRID_DIVISION 的整數值來決定間隔,最小為 1
     * 如果設定了最大間隔 (MAX_GRID_SPACING != -1),則限制步長不超過此值
     * 
     * @param height 多邊形高度
     * @return 網格步長 (每隔多少層繪製一個邊框)
     */
    private int calculateGridStep(int height) {
        int step = height / settings.getPolygonHeightGridDivision();
        step = Math.max(1, step); // 最小間隔為 1
        
        // 如果設定了最大間隔限制
        if (settings.getPolygonMaxGridSpacing() != -1) {
            step = Math.min(step, settings.getPolygonMaxGridSpacing());
        }
        
        return step;
    }
    
    /**
     * 渲染多邊形的邊框(在指定 Y 平面上)
     * 
     * @param points 多邊形頂點列表
     * @param y Y 座標(整數層)
     * @param material 線條材質
     * @param thickness 線條粗細
     */
    private void renderPolygonEdges(List<Vector2> points, int y, Material material, float thickness) {
        int size = points.size();
        
        for (int i = 0; i < size; i++) {
            Vector2 current = points.get(i);
            Vector2 next = points.get((i + 1) % size); // 循環到第一個點
            
            // 頂點在方塊中心 (X+0.5, Z+0.5),Y 在整數層
            Vector3f start = new Vector3f(current.getX() + 0.5f, (float) y, current.getZ() + 0.5f);
            Vector3f end = new Vector3f(next.getX() + 0.5f, (float) y, next.getZ() + 0.5f);
            
            renderLine(start, end, material, thickness);
        }
    }
    
    /**
     * 渲染垂直連接線(從 minY 到 maxY)
     * 與頂點標記完全對齊,從底部到頂部
     * 
     * @param points 多邊形頂點列表
     * @param minY 最小 Y 座標
     * @param maxY 最大 Y 座標
     * @param material 線條材質
     */
    private void renderVerticalEdges(List<Vector2> points, int minY, int maxY, Material material) {
        for (Vector2 point : points) {
            // 垂直線從 minY 到 maxY + 1
            // X, Z 座標在方塊中心 (與多邊形邊框一致)
            Vector3f start = new Vector3f(point.getX() + 0.5f, (float) minY, point.getZ() + 0.5f);
            Vector3f end = new Vector3f(point.getX() + 0.5f, maxY + 1.0f, point.getZ() + 0.5f);
            
            renderLine(start, end, material, settings.getPolygonVerticalThickness());
        }
    }
    
    /**
     * 渲染頂點標記
     * 渲染一個從底部到頂部的垂直長方體邊框
     * 
     * @param points 多邊形頂點列表
     * @param minY 最小 Y 座標
     * @param maxY 最大 Y 座標
     * @param material 材質
     */
    private void renderVertexMarkers(List<Vector2> points, int minY, int maxY, Material material) {
        float thickness = 0.05f;
        
        for (Vector2 point : points) {
            // 計算長方體邊框的範圍 (標準方塊大小)
            double minX = point.getX();
            double minYPos = minY;
            double minZ = point.getZ();
            double maxX = point.getX() + 1.0;
            double maxYPos = maxY + 1.0;
            double maxZ = point.getZ() + 1.0;
            
            // 渲染垂直長方體邊框 (12條邊)
            renderBoxFrame(minX, minYPos, minZ, maxX, maxYPos, maxZ, material, thickness);
        }
    }
    
    @Override
    public Class<PolygonRegion> getRegionType() {
        return PolygonRegion.class;
    }
}
