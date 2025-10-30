package dev.twme.worldeditdisplay.listener;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

/**
 * 玩家語言變更監聽器
 * 監聽玩家客戶端語言變更事件
 */
public class PlayerLocaleChangeListener implements Listener {
    
    private final WorldEditDisplay plugin;
    
    public PlayerLocaleChangeListener(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        String newLocale = event.locale().toString().toLowerCase().replace("-", "_");
        
        // 更新玩家的語言設定
        plugin.getLanguageManager().setPlayerLanguage(
            event.getPlayer().getUniqueId(),
            newLocale
        );
    }
}
