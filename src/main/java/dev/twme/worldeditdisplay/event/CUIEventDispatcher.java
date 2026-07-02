package dev.twme.worldeditdisplay.event;

import java.util.logging.Level;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Dispatcher for CUI events
 */
public class CUIEventDispatcher {
    private static final long FAILURE_LOG_INTERVAL_MS = 10_000L;

    private final PlayerData playerData;
    private long lastFailureLogTime;

    public CUIEventDispatcher(PlayerData playerData) {
        this.playerData = playerData;
    }

    public void raiseEvent(CUIEventArgs eventArgs) {
        try {
            final CUIEventType type = CUIEventType.named(eventArgs.getType());
            if (type == null) {
                return;
            }

            CUIEvent event = type.make(eventArgs);
            event.prepare();

            String response = event.execute();  // 使用 execute() 來自動觸發渲染更新
            if (response != null) {
                this.handleEventResponse(response);
            }
        } catch (Exception ex) {
            logDispatchFailure(eventArgs, ex);
        }
    }

    private void logDispatchFailure(CUIEventArgs eventArgs, Exception ex) {
        long now = System.currentTimeMillis();
        if (now - lastFailureLogTime < FAILURE_LOG_INTERVAL_MS) return;
        lastFailureLogTime = now;

        WorldEditDisplay plugin = WorldEditDisplay.getPlugin();
        if (plugin == null) return;

        String playerName = playerData.getPlayer() != null ? playerData.getPlayer().getName() : "unknown";
        plugin.getLogger().log(Level.WARNING,
                "Failed to dispatch CUI event type=" + eventArgs.getType()
                        + ", multi=" + eventArgs.isMulti()
                        + ", params=" + eventArgs.getParams().size()
                        + ", player=" + playerName,
                ex);
    }

    private void handleEventResponse(String response) {
        // Not implemented yet
    }
}
