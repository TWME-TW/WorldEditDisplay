package dev.twme.worldeditdisplay.common;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

/**
 * 顏色匹配工具類，用於找到最接近指定顏色的方塊材質
 */
public class ColorMatcher {
    
    private static final Map<Material, Color> MATERIAL_COLOR_CACHE = new HashMap<>();
    private static boolean initialized = false;
    
    /**
     * 初始化顏色快取
     * 預先計算所有可用材質的顏色以提升效能
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        for (Material material : Constants.COLOR_MATERIALS) {
            try {
                BlockData blockData = material.createBlockData();
                // 獲取地圖顏色
                Color mapColor = blockData.getMapColor();
                MATERIAL_COLOR_CACHE.put(material, mapColor);
            } catch (Exception e) {
                // 如果無法獲取顏色，使用預設值
                MATERIAL_COLOR_CACHE.put(material, Color.fromRGB(128, 128, 128));
            }
        }
        
        initialized = true;
    }
    
    /**
     * 找到最接近指定 RGBA 顏色的方塊材質
     * 
     * @param red 紅色分量 (0-255)
     * @param green 綠色分量 (0-255)
     * @param blue 藍色分量 (0-255)
     * @param alpha 透明度分量 (0-255)，目前未使用
     * @return 最接近的材質
     */
    public static Material findClosestMaterial(int red, int green, int blue, int alpha) {
        if (!initialized) {
            initialize();
        }
        
        Color targetColor = Color.fromRGB(red, green, blue);
        return findClosestMaterial(targetColor);
    }
    
    /**
     * 找到最接近指定 RGB 顏色的方塊材質
     * 
     * @param red 紅色分量 (0-255)
     * @param green 綠色分量 (0-255)
     * @param blue 藍色分量 (0-255)
     * @return 最接近的材質
     */
    public static Material findClosestMaterial(int red, int green, int blue) {
        return findClosestMaterial(red, green, blue, 255);
    }
    
    /**
     * 找到最接近指定顏色的方塊材質
     * 
     * @param targetColor 目標顏色
     * @return 最接近的材質
     */
    public static Material findClosestMaterial(Color targetColor) {
        if (!initialized) {
            initialize();
        }
        
        Material closestMaterial = Constants.COLOR_MATERIALS[0];
        double minDistance = Double.MAX_VALUE;
        
        for (Map.Entry<Material, Color> entry : MATERIAL_COLOR_CACHE.entrySet()) {
            double distance = calculateColorDistance(targetColor, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                closestMaterial = entry.getKey();
            }
        }
        
        return closestMaterial;
    }
    
    /**
     * 計算兩個顏色之間的歐幾里得距離
     * 
     * @param color1 第一個顏色
     * @param color2 第二個顏色
     * @return 顏色距離
     */
    private static double calculateColorDistance(Color color1, Color color2) {
        int redDiff = color1.getRed() - color2.getRed();
        int greenDiff = color1.getGreen() - color2.getGreen();
        int blueDiff = color1.getBlue() - color2.getBlue();
        
        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }
    
    /**
     * 計算加權的顏色距離（考慮人眼對不同顏色的敏感度）
     * 此方法保留供未來可能的優化使用
     * 
     * @param color1 第一個顏色
     * @param color2 第二個顏色
     * @return 加權顏色距離
     */
    @SuppressWarnings("unused")
    private static double calculateWeightedColorDistance(Color color1, Color color2) {
        int redDiff = color1.getRed() - color2.getRed();
        int greenDiff = color1.getGreen() - color2.getGreen();
        int blueDiff = color1.getBlue() - color2.getBlue();
        
        // 使用加權公式，因為人眼對綠色最敏感，對藍色最不敏感
        double redWeight = 0.30;
        double greenWeight = 0.59;
        double blueWeight = 0.11;
        
        return Math.sqrt(
            redWeight * redDiff * redDiff +
            greenWeight * greenDiff * greenDiff +
            blueWeight * blueDiff * blueDiff
        );
    }
    
    /**
     * 獲取指定材質的快取顏色
     * 
     * @param material 材質
     * @return 顏色，如果未快取則返回 null
     */
    public static Color getMaterialColor(Material material) {
        if (!initialized) {
            initialize();
        }
        return MATERIAL_COLOR_CACHE.get(material);
    }
    
    /**
     * 獲取所有快取的材質顏色映射
     * 
     * @return 材質顏色映射的副本
     */
    public static Map<Material, Color> getAllMaterialColors() {
        if (!initialized) {
            initialize();
        }
        return new HashMap<>(MATERIAL_COLOR_CACHE);
    }
    
    /**
     * 清除快取並重新初始化
     */
    public static void refresh() {
        MATERIAL_COLOR_CACHE.clear();
        initialized = false;
        initialize();
    }
}
