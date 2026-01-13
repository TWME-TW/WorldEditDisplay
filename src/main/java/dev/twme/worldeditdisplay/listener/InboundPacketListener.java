package dev.twme.worldeditdisplay.listener;

import java.nio.charset.StandardCharsets;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import dev.twme.worldeditdisplay.common.Constants;
import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Listens to incoming plugin messages.
 * Detects CUI registration and marks players as having CUI enabled.
 */
public class InboundPacketListener implements PacketListener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

        // Catch exceptions for oversized packets (e.g., mods like Axiom)
        WrapperPlayClientPluginMessage packet;
        try {
            packet = new WrapperPlayClientPluginMessage(event);
        } catch (Exception e) {
            return; // ignore invalid packets
        }

        // Listen for REGISTER channel to detect CUI
        if (Constants.REGISTER_CHANNEL.equals(packet.getChannelName())) {
            String registerMessage = new String(packet.getData(), StandardCharsets.UTF_8);

            if (registerMessage.contains(Constants.CUI_CHANNEL)) {
                Player player = event.getPlayer();
                PlayerData playerData = PlayerData.getPlayerData(player);
                playerData.setCuiEnabled(true); // mark player as having CUI
            }
        }
    }
}