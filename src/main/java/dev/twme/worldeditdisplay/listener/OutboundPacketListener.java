package dev.twme.worldeditdisplay.listener;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;

import dev.twme.worldeditdisplay.common.Constants;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.player.PlayerData;

public class OutboundPacketListener implements PacketListener {
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PLUGIN_MESSAGE) {
            return;
        }

        WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage(event);
        String channel = packet.getChannelName();

        if (!Constants.CUI_CHANNEL.equals(channel)) {
            return;
        }

        byte[] data = packet.getData();
        String message = new String(data, StandardCharsets.UTF_8);
        Player player = (Player) event.getPlayer();

        // 檢查玩家是否有使用權限
        if (!player.hasPermission("worldeditdisplay.use")) {
            return; // 沒有權限，直接返回不處理
        }

        // Get player data
        PlayerData playerData = PlayerData.getPlayerData(player);
        
        // 如果玩家已經有 CUI，直接傳出封包不處理
        if (playerData.isCuiEnabled()) {
            return;
        }

        event.setCancelled(true); // 取消封包傳送

        // Parse the CUI message
        String[] split = message.split("\\|", -1); // 使用 -1 保留尾部空字串
        boolean multi = split[0].startsWith("+");
        String type = split[0].substring(multi ? 1 : 0);
        List<String> params = split.length > 1 ? Arrays.asList(Arrays.copyOfRange(split, 1, split.length)) : List.of();
        
        // Create event args and dispatch
        CUIEventArgs eventArgs = new CUIEventArgs(playerData, multi, type, params);
        playerData.getDispatcher().raiseEvent(eventArgs);
    }
}
