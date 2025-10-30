package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when grid spacing event is received
 */
public class CUIEventGrid extends CUIEvent {
    public CUIEventGrid(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.GRID;
    }

    @Override
    public void prepare() {
        if (!this.multi) {
            throw new IllegalStateException("GRID event is not valid for non-multi selections");
        }

        super.prepare();
    }

    @Override
    public String raise() {
        double spacing = this.getDouble(0);
        // 備註: 原本有 renderType 邏輯，但目前未被使用
        
        // Get current multi-region (grid event only applies to multi-selections)
        Region region = playerData.getCurrentMultiRegion();
        
        if (region != null) {
            // 設定網格間距
            region.setGridSpacing(spacing);
            // TODO: 可在此處添加日誌記錄功能 (例如使用 Logger 替代 System.out.println)
        }
        // NOTE: 若無多選區域，grid spacing 更新會被忽略

        return null;
    }
}
