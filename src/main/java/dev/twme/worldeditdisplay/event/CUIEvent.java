package dev.twme.worldeditdisplay.event;

import java.util.List;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Base event for CUI events, handles parameter validation and running the logic
 */
public abstract class CUIEvent {
    protected final PlayerData playerData;
    protected final List<String> params;
    protected final boolean multi;

    public CUIEvent(CUIEventArgs args) {
        this.playerData = args.getPlayerData();
        this.params = args.getParams();
        this.multi = args.isMulti();
    }

    public abstract String raise();

    public abstract CUIEventType getEventType();

    public String getEventName() {
        return this.getEventType().getName();
    }
    
    /**
     * 在事件處理完成後觸發渲染更新
     * 子類別可以覆寫此方法以跳過渲染更新
     * 
     * @return 是否需要更新渲染
     */
    protected boolean shouldUpdateRender() {
        return true;
    }
    
    /**
     * 執行事件並觸發渲染更新
     * 
     * @return 事件處理結果
     */
    public String execute() {
        String result = raise();
        
        // 如果事件需要更新渲染,則觸發更新
        if (shouldUpdateRender() && playerData != null && playerData.getPlayer() != null) {
            WorldEditDisplay plugin = WorldEditDisplay.getPlugin();
            if (plugin != null && plugin.getRenderManager() != null) {
                plugin.getRenderManager().updateRender(playerData.getPlayer());
            }
        }
        
        return result;
    }

    /**
     * Checks if the parameters match the required length.
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        int max = this.getEventType().getMaxParameters();
        int min = this.getEventType().getMinParameters();

        if (max == min) {
            if (this.params.size() != max) {
                return false;
            }
        } else {
            if (this.params.size() > max || this.params.size() < min) {
                return false;
            }
        }

        return true;
    }

    public void prepare() {
        if (this.playerData == null || this.params == null) {
            throw new IllegalStateException("PlayerData and parameters must both be set.");
        }

        if (!this.isValid()) {
            String message = String.format("Invalid number of parameters. %s event requires %s parameters but received %s",
                    this.getEventName(), this.getRequiredParameterString(), this.params.size());
            throw new IllegalArgumentException(message);
        }
    }

    private String getRequiredParameterString() {
        if (this.getEventType().getMaxParameters() == this.getEventType().getMinParameters()) {
            return String.valueOf(this.getEventType().getMaxParameters());
        }

        return String.format("between %d and %d", this.getEventType().getMinParameters(), this.getEventType().getMaxParameters());
    }

    public int getInt(int index) {
        return (int) Float.parseFloat(this.params.get(index));
    }

    public double getDouble(int index) {
        return Double.parseDouble(this.params.get(index));
    }

    public String getString(int index) {
        return this.params.get(index);
    }
}
