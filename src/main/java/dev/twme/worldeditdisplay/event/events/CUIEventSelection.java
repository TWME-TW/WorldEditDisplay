package dev.twme.worldeditdisplay.event.events;

import java.util.UUID;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.display.RenderManager;
import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when selection event is received
 */
public class CUIEventSelection extends CUIEvent {
    public CUIEventSelection(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.SELECTION;
    }

    @Override
    public String raise() {
        String key = this.getString(0);
        
        // Handle "clear" as a special case - it means clear/remove the selection
        Region region = null;
        boolean isClearing = "clear".equals(key);
        
        if (!isClearing) {
            region = playerData.createRegion(key);
            
            if (region == null) {
                return null;
            }
        }
        
        // Handle multi-selection mode
        if (this.multi) {
            UUID id = null;
            
            // If it's a clear event without UUID, clear all multi-selections only
            if (region == null && this.params.size() < 2) {
                playerData.clearRegions(true); // 只清除多重選區
                
                // 直接清除所有 multi renderer，不觸發全量 updateRender
                RenderManager rm = getRenderManager();
                if (rm != null) {
                    rm.clearAllMultiRenderers(playerData.getPlayer().getUniqueId());
                }
                this.shouldTriggerRender = false; // 已直接處理，不需要再觸發
                return null;
            }
            
            // Get the UUID for this multi-selection
            if (this.params.size() >= 2) {
                try {
                    id = UUID.fromString(this.getString(1));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            
            // 處理多重選區
            if (id != null) {
                playerData.setSelection(id, region);
                
                if (region != null) {
                    // 設定新的選區，更新當前多重選區 ID
                    playerData.setCurrentMultiRegionId(id);
                    // 創建新選區時不觸發渲染（等待點的資料）
                    this.shouldTriggerRender = false;
                } else {
                    // 清除特定選區：直接移除該 renderer，不觸發全量更新
                    RenderManager rm = getRenderManager();
                    if (rm != null) {
                        rm.removeMultiRenderer(playerData.getPlayer().getUniqueId(), id);
                    }
                    
                    if (id.equals(playerData.getCurrentMultiRegionId())) {
                        playerData.setCurrentMultiRegionId(null);
                    }
                    
                    this.shouldTriggerRender = false; // 已直接處理
                }
            }
        } else {
            // 非多重選區模式：只影響一般選區
            // 在設定新選區之前，先清除舊選區的渲染（僅清除主選區 renderer，不影響 multi）
            Region oldRegion = playerData.getSelection();
            if (oldRegion != null && region != null) {
                RenderManager rm = getRenderManager();
                if (rm != null) {
                    rm.clearMainRender(playerData.getPlayer().getUniqueId());
                }
            }
            
            playerData.setSelection(region);
            if (region == null) {
                this.shouldTriggerRender = true;
            } else {
                // 創建新選區時不觸發渲染（等待點的資料）
                this.shouldTriggerRender = false;
            }
        }
        
        return null;
    }
    
    private RenderManager getRenderManager() {
        WorldEditDisplay plugin = WorldEditDisplay.getPlugin();
        return (plugin != null) ? plugin.getRenderManager() : null;
    }
    
    private boolean shouldTriggerRender = false;
    
    @Override
    protected boolean shouldUpdateRender() {
        return shouldTriggerRender;
    }
}
