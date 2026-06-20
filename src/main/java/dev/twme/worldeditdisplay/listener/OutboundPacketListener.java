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
import dev.twme.worldeditdisplay.util.MessageUtil;

/**
 * Listens to outgoing plugin messages.
 * Captures CUI messages and dispatches CUI events for the plugin.
 */
public class OutboundPacketListener implements PacketListener {

    /**
     * Guard flag: set to {@code false} by {@link #deactivate()} during
     * {@code onDisable()} so that in-flight Netty dispatches bail out
     * at the earliest possible point — before any plugin class loading
     * that would hit the already-closed PluginClassLoader ZipFile.
     */
    private volatile boolean active = true;

    /** Called from onDisable to prevent the classloader-zip-closed race. */
    public void deactivate() {
        this.active = false;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!active) return;

        if (event.getPacketType() != PacketType.Play.Server.PLUGIN_MESSAGE) return;

        WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage(event);
        String channel = packet.getChannelName();

        if (!Constants.CUI_CHANNEL.equals(channel)) return;

        byte[] data = packet.getData();
        String message = new String(data, StandardCharsets.UTF_8);
        Player player = event.getPlayer();

        // Skip if player lacks permission
        if (!player.hasPermission("worldeditdisplay.use")) return;

        PlayerData playerData = PlayerData.getPlayerData(player);

        // Bedrock (Floodgate) players: cancel CUI packets (they don't support CUI)
        // but still parse the message so we can create Region objects for particle rendering.
        if (playerData.isBedrockPlayer()) {
            event.setCancelled(true);
        }

        // If debug mode, show the received WECUI message
        if (playerData.isDebugEnabled()) {
            MessageUtil.sendTranslated(player, "command.wedisplay.debug.cui_message", message);
        }

        // If CUI already enabled, let the packet go through (unless bedrock, already cancelled)
        if (playerData.isCuiEnabled()) return;

        event.setCancelled(true); // cancel packet sending

        // Parse CUI message
        String[] split = message.split("\\|", -1); // preserve trailing empty strings
        boolean multi = split[0].startsWith("+");
        String type = split[0].substring(multi ? 1 : 0);
        List<String> params = split.length > 1
                ? Arrays.asList(Arrays.copyOfRange(split, 1, split.length))
                : List.of();

        // Dispatch CUI event
        CUIEventArgs eventArgs = new CUIEventArgs(playerData, multi, type, params);
        playerData.getDispatcher().raiseEvent(eventArgs);
    }
}
