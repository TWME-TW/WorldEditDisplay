package dev.twme.worldeditdisplay.event.events;

import org.bukkit.Color;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;
import dev.twme.worldeditdisplay.util.ColorUtil;

/**
 * Called when style/colour event is received
 * 
 * 此事件將 CUI 協議的顏色直接轉換為 Color 物件，
 * 用於 TextDisplay 的顏色渲染
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
        
        Color[] overrideColors = new Color[4];
        
        overrideColors[0] = parseColor(primaryColor);
        overrideColors[1] = parseColor(secondaryColor);
        overrideColors[2] = parseColor(gridColor);
        overrideColors[3] = parseColor(backgroundColor);
        
        // 將顏色設定套用到玩家資料（保留舊的行為）
        playerData.setSelectionColors(primaryColor, secondaryColor, gridColor, backgroundColor);
        
        // 將顏色覆寫套用到選區
        selection.setOverrideColors(overrideColors);

        return null;
    }
    
    /**
     * 解析十六進制顏色字串為 Color 物件
     * 
     * @param hexColor 十六進制顏色字串，格式: #RRGGBB 或 #RRGGBBAA
     * @return Color 物件，如果解析失敗則返回 null
     */
    private Color parseColor(String hexColor) {
        if (hexColor == null || hexColor.trim().isEmpty()) {
            return null;
        }
        if (!hexColor.startsWith("#")) {
            hexColor = "#" + hexColor;
        }
        return ColorUtil.parseHexColor(hexColor);
    }
}
