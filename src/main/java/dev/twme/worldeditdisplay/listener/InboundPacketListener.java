package dev.twme.worldeditdisplay.listener;

import java.nio.charset.StandardCharsets;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import dev.twme.worldeditdisplay.common.Constants;
import dev.twme.worldeditdisplay.player.PlayerData;

public class InboundPacketListener implements PacketListener {


    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) {
            return;
        }
        WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);

        // 監聽 minecraft:register 頻道，檢查是否包含 worldedit:cui
        if (Constants.REGISTER_CHANNEL.equals(packet.getChannelName())) {
            byte[] data = packet.getData();
            String registerMessage = new String(data, StandardCharsets.UTF_8);
            
            // 檢查是否包含 worldedit:cui
            if (registerMessage.contains(Constants.CUI_CHANNEL)) {
                Player player = (Player) event.getPlayer();
                PlayerData playerData = PlayerData.getPlayerData(player);
                // 標記玩家已經有 CUI
                playerData.setCuiEnabled(true);
            }
        }
    }
}
