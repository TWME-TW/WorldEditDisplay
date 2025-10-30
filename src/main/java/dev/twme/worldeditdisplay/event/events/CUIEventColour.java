package dev.twme.worldeditdisplay.event.events;

import org.bukkit.Material;

import dev.twme.worldeditdisplay.common.ColorMatcher;
import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when style/colour event is received
 * 
 * 此事件將 CUI 協議的顏色轉換為對應的 Minecraft 方塊材質，
 * 以模擬 WorldEditCUI 的顏色渲染行為
 */
public class CUIEventColour extends CUIEvent {
    public CUIEventColour(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.COLOUR;
    }

    @Override
    public void prepare() {
        if (!this.multi) {
            throw new IllegalStateException("COLOUR event is not valid for non-multi selections");
        }

        super.prepare();
    }

    @Override
    public String raise() {
        Region selection = playerData.getSelection(true);
        if (selection == null) {
            return null;
        }
        
        String primaryColor = !params.isEmpty() ? getString(0) : null;
        String secondaryColor = params.size() > 1 ? getString(1) : null;
        String gridColor = params.size() > 2 ? getString(2) : null;
        String backgroundColor = params.size() > 3 ? getString(3) : null;
        
        // 初始化材質陣列
        Material[] colorMaterials = new Material[4];
        
        // 處理主要顏色 (styles[0])
        if (primaryColor != null && !primaryColor.trim().isEmpty()) {
            if (!primaryColor.startsWith("#")) {
                primaryColor = "#" + primaryColor;
            }
            colorMaterials[0] = parseColorToMaterial(primaryColor);
        }
        
        // 處理次要顏色 (styles[1])
        if (secondaryColor != null && !secondaryColor.trim().isEmpty()) {
            if (!secondaryColor.startsWith("#")) {
                secondaryColor = "#" + secondaryColor;
            }
            colorMaterials[1] = parseColorToMaterial(secondaryColor);
        }
        
        // 處理網格顏色 (styles[2]) - 空值表示不顯示網格
        if (gridColor != null && !gridColor.trim().isEmpty()) {
            if (!gridColor.startsWith("#")) {
                gridColor = "#" + gridColor;
            }
            colorMaterials[2] = parseColorToMaterial(gridColor);
        }
        
        // 處理背景顏色 (styles[3]) - 空值表示不顯示背景
        if (backgroundColor != null && !backgroundColor.trim().isEmpty()) {
            if (!backgroundColor.startsWith("#")) {
                backgroundColor = "#" + backgroundColor;
            }
            colorMaterials[3] = parseColorToMaterial(backgroundColor);
        }
        
        // 將顏色設定套用到玩家資料（保留舊的行為）
        playerData.setSelectionColors(primaryColor, secondaryColor, gridColor, backgroundColor);
        
        // 將材質覆寫套用到選區
        selection.setColorMaterials(colorMaterials);

        return null;
    }
    
    /**
     * 解析十六進制顏色字串並轉換為最接近的 Minecraft 方塊材質
     * 
     * @param hexColor 十六進制顏色字串，格式: #RRGGBB 或 #RRGGBBAA
     * @return 最接近的材質，如果解析失敗則返回 null
     */
    private Material parseColorToMaterial(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#")) {
            return null;
        }
        
        try {
            // 移除 # 符號
            String hex = hexColor.substring(1);
            
            // 解析 RGB 值
            int r, g, b;
            
            if (hex.length() >= 6) {
                r = Integer.parseInt(hex.substring(0, 2), 16);
                g = Integer.parseInt(hex.substring(2, 4), 16);
                b = Integer.parseInt(hex.substring(4, 6), 16);
                
                // Alpha 值（如果提供的話）目前不使用
                // int a = hex.length() >= 8 ? Integer.parseInt(hex.substring(6, 8), 16) : 255;
                
                // 確保 ColorMatcher 已初始化
                ColorMatcher.initialize();
                
                // 使用 ColorMatcher 找到最接近的材質
                return ColorMatcher.findClosestMaterial(r, g, b);
                
            } else {
                return null;
            }
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
