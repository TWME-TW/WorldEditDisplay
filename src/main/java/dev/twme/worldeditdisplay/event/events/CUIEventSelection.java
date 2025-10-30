package dev.twme.worldeditdisplay.event.events;

import java.util.UUID;

import dev.twme.worldeditdisplay.WorldEditDisplay;
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
                
                // 標記需要更新渲染（因為是清除操作）
                this.shouldTriggerRender = true;
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
                
                // 如果是設定新的選區（不是清除），則更新當前多重選區 ID
                if (region != null) {
                    playerData.setCurrentMultiRegionId(id);
                    
                    // 創建新選區時不觸發渲染（等待點的資料）
                    this.shouldTriggerRender = false;
                } else {
                    
                    // 如果清除的是當前多重選區，則重置當前多重選區 ID
                    if (id.equals(playerData.getCurrentMultiRegionId())) {
                        playerData.setCurrentMultiRegionId(null);
                    }
                    
                    // 清除選區時需要觸發渲染
                    this.shouldTriggerRender = true;
                }
            }
        } else {
            // 非多重選區模式：只影響一般選區
            // 在設定新選區之前，先清除舊選區的渲染
            Region oldRegion = playerData.getSelection();
            if (oldRegion != null && region != null) {
                // 如果有舊選區且要設定新選區，先清除舊選區的渲染
                WorldEditDisplay plugin = WorldEditDisplay.getPlugin();
                if (plugin != null && plugin.getRenderManager() != null) {
                    plugin.getRenderManager().clearRender(playerData.getPlayer().getUniqueId());
                }
            }
            
            playerData.setSelection(region);
            if (region == null) {
                
                // 清除選區時需要觸發渲染
                this.shouldTriggerRender = true;
            } else {
                // 創建新選區時不觸發渲染（等待點的資料）
                this.shouldTriggerRender = false;
            }
        }
        
        return null;
    }
    
    private boolean shouldTriggerRender = false;
    
    @Override
    protected boolean shouldUpdateRender() {
        // 只有在清除選區時才觸發渲染，創建新選區時等待點的資料
        return shouldTriggerRender;
    }
}
