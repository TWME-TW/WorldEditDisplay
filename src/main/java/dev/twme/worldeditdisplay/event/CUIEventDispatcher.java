package dev.twme.worldeditdisplay.event;

import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Dispatcher for CUI events
 */
public class CUIEventDispatcher {
    private final PlayerData playerData;

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
            // Exception occurred, but we'll just ignore it
        }
    }

    private void handleEventResponse(String response) {
        // Not implemented yet
    }
}
