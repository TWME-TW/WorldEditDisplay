package dev.twme.worldeditdisplay.display.renderer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.region.PolyhedronRegion;
import dev.twme.worldeditdisplay.region.Vector3;

/**
 * 多面體選區渲染器
 * 
 * 渲染邏輯:
 * 1. 在所有頂點顯示標記（第一個頂點使用特殊顏色）
 * 2. 渲染所有面的邊緣線條
 * 3. 自動去除重複邊緣，確保視覺簡潔
 * 
 * 參考 WorldEditCUI 的實作方式，提供完整的多面體選區視覺化
 * 
 * 所有設定值現在從 PlayerRenderSettings 讀取
 * @version 3.0 (配置整合版本)
 */
public class PolyhedronRenderer extends RegionRenderer<PolyhedronRegion> {
    
    public PolyhedronRenderer(WorldEditDisplay plugin, Player player, PlayerRenderSettings settings) {
        super(plugin, player, settings);
    }
    
    @Override
    public void render(PolyhedronRegion region) {
        // 先清除舊的渲染實體
        clear();
        
        // 判斷是否為多重選區
        boolean isMultiSelection = isMultiSelection(region);
        
        // 獲取頂點和面
        List<Vector3> vertices = region.getVertices();
        List<int[]> faces = region.getFaces();
        
        // 檢查是否至少有頂點可以顯示
        if (vertices.isEmpty()) {
            return;
        }
        
        // 計算有效頂點數量（非 null 的頂點）
        long validVertexCount = vertices.stream().filter(v -> v != null).count();
        if (validVertexCount == 0) {
            return;
        }
        
        // 獲取材質（只有多重選區才使用 CUI 顏色覆寫）
        // CUI 顏色索引對應 Polyhedron:
        // - colorIndex 0 (styles[0]): 主要顏色 -> Line (邊緣線)
        // - colorIndex 1 (styles[1]): 次要顏色 -> (未使用)
        // - colorIndex 2 (styles[2]): 網格顏色 -> Vertex (頂點)
        // - colorIndex 3 (styles[3]): 背景顏色 -> Vertex0 (第一個頂點)
        Material lineMaterial = getMaterialWithOverride(region, 0, settings.getPolyhedronLineMaterial(), isMultiSelection);
        Material vertexMaterial = getMaterialWithOverride(region, 2, settings.getPolyhedronVertexMaterial(), isMultiSelection);
        Material vertex0Material = getMaterialWithOverride(region, 3, settings.getPolyhedronVertex0Material(), isMultiSelection);
        
        // 1. 渲染所有頂點標記
        renderVertices(vertices, vertexMaterial, vertex0Material);
        
        // 2. 渲染所有面的邊緣（如果有面的話）
        if (!faces.isEmpty()) {
            renderFaceEdges(vertices, faces, lineMaterial);
        }
    }
    
    /**
     * 渲染所有頂點標記
     * 第一個頂點使用特殊顏色，其他頂點使用普通顏色
     * 
     * @param vertices 頂點列表
     * @param vertexMaterial 普通頂點材質
     * @param vertex0Material 第一個頂點材質
     */
    private void renderVertices(List<Vector3> vertices, Material vertexMaterial, Material vertex0Material) {
        for (int i = 0; i < vertices.size(); i++) {
            Vector3 vertex = vertices.get(i);
            if (vertex == null) {
                continue; // 跳過 null 頂點
            }
            
            // 第一個頂點使用特殊顏色
            Material material = (i == 0) ? vertex0Material : vertexMaterial;
            
            // 渲染頂點標記（小立方體）
            // 在頂點中心偏移 0.5，因為 Minecraft 方塊座標從角落開始
            org.joml.Vector3f center = new org.joml.Vector3f(
                (float) (vertex.getX() + 0.5),
                (float) (vertex.getY() + 0.5),
                (float) (vertex.getZ() + 0.5)
            );
            
            renderCube(center, settings.getPolyhedronVertexSize(), material, settings.getPolyhedronVertexThickness());
        }
    }
    
    /**
     * 渲染所有面的邊緣
     * 使用 Set 去重，避免重複渲染相鄰面共享的邊
     * 
     * @param vertices 頂點列表
     * @param faces 面列表（每個面是一組頂點索引）
     * @param material 線條材質
     */
    private void renderFaceEdges(List<Vector3> vertices, List<int[]> faces, Material material) {
        // 使用 Set 儲存已渲染的邊，避免重複
        // 邊以 "min_index-max_index" 格式表示（確保順序一致）
        Set<String> renderedEdges = new HashSet<>();
        
        for (int[] face : faces) {
            if (face == null || face.length < 2) {
                continue; // 面至少需要 2 個頂點
            }
            
            // 渲染面的所有邊（連接相鄰頂點）
            for (int i = 0; i < face.length; i++) {
                int vertexIndex1 = face[i];
                int vertexIndex2 = face[(i + 1) % face.length]; // 環繞到第一個頂點
                
                // 檢查頂點索引有效性
                if (vertexIndex1 < 0 || vertexIndex1 >= vertices.size() ||
                    vertexIndex2 < 0 || vertexIndex2 >= vertices.size()) {
                    continue;
                }
                
                Vector3 vertex1 = vertices.get(vertexIndex1);
                Vector3 vertex2 = vertices.get(vertexIndex2);
                
                // 檢查頂點是否為 null
                if (vertex1 == null || vertex2 == null) {
                    continue;
                }
                
                // 生成邊的唯一標識符（較小索引在前）
                String edgeKey = getEdgeKey(vertexIndex1, vertexIndex2);
                
                // 如果這條邊已經渲染過，跳過
                if (renderedEdges.contains(edgeKey)) {
                    continue;
                }
                
                // 渲染邊緣線
                renderEdge(vertex1, vertex2, material);
                
                // 標記為已渲染
                renderedEdges.add(edgeKey);
            }
        }
    }
    
    /**
     * 渲染一條邊緣線
     * 在頂點座標上偏移 0.5，使線條位於方塊中心
     * 
     * @param vertex1 起點
     * @param vertex2 終點
     * @param material 線條材質
     */
    private void renderEdge(Vector3 vertex1, Vector3 vertex2, Material material) {
        // 轉換為 JOML Vector3f，並偏移到方塊中心
        org.joml.Vector3f start = new org.joml.Vector3f(
            (float) (vertex1.getX() + 0.5),
            (float) (vertex1.getY() + 0.5),
            (float) (vertex1.getZ() + 0.5)
        );
        
        org.joml.Vector3f end = new org.joml.Vector3f(
            (float) (vertex2.getX() + 0.5),
            (float) (vertex2.getY() + 0.5),
            (float) (vertex2.getZ() + 0.5)
        );
        
        // 使用基類提供的 renderLine 方法
        renderLine(start, end, material, settings.getPolyhedronLineThickness());
    }
    
    /**
     * 生成邊的唯一標識符
     * 確保無論頂點順序如何，相同的邊有相同的 key
     * 
     * @param index1 第一個頂點索引
     * @param index2 第二個頂點索引
     * @return 邊的唯一標識符
     */
    private String getEdgeKey(int index1, int index2) {
        int min = Math.min(index1, index2);
        int max = Math.max(index1, index2);
        return min + "-" + max;
    }
    
    @Override
    public Class<PolyhedronRegion> getRegionType() {
        return PolyhedronRegion.class;
    }
}
