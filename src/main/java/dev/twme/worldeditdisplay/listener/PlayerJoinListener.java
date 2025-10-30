package dev.twme.worldeditdisplay.listener;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.common.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.nio.charset.StandardCharsets;

public class PlayerJoinListener implements Listener {
    WorldEditDisplay plugin;

    public PlayerJoinListener(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 獲取並設置玩家語言
        plugin.getLanguageManager().getPlayerLanguage(player);
        
        // 延遲一秒，讓有 CUI 的玩家先註冊
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // 檢查玩家是否仍在線
            if (!player.isOnline()) {
                return;
            }
            
            String cuiVersionMessage = "v|4";

            WrapperPlayClientPluginMessage registerPacket = new WrapperPlayClientPluginMessage(Constants.REGISTER_CHANNEL, Constants.CUI_CHANNEL.getBytes(StandardCharsets.UTF_8));
            WrapperPlayClientPluginMessage cuiVersionPacket = new WrapperPlayClientPluginMessage(Constants.CUI_CHANNEL, cuiVersionMessage.getBytes(StandardCharsets.UTF_8));

            PacketEvents.getAPI().getPlayerManager().receivePacketSilently(player, registerPacket);
            PacketEvents.getAPI().getPlayerManager().receivePacketSilently(player, cuiVersionPacket);
        }, 20L); // 20 ticks = 1 second
    }
}
