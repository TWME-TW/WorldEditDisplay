package dev.twme.worldeditdisplay.listener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

        WrapperPlayServerPluginMessage packet;
        try {
            packet = new WrapperPlayServerPluginMessage(event);
        } catch (Exception e) {
            return;
        }
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

        ParsedCuiMessage parsed = parseCuiMessage(message);
        if (parsed == null) return;

        // Dispatch CUI event
        CUIEventArgs eventArgs = new CUIEventArgs(playerData, parsed.multi(), parsed.type(), parsed.params());
        playerData.getDispatcher().raiseEvent(eventArgs);
    }

    static ParsedCuiMessage parseCuiMessage(String message) {
        int firstSeparator = message.indexOf('|');
        int typeEnd = firstSeparator == -1 ? message.length() : firstSeparator;
        boolean multi = typeEnd > 0 && message.charAt(0) == '+';
        int typeStart = multi ? 1 : 0;
        if (typeStart == typeEnd) return null;

        String type = message.substring(typeStart, typeEnd);
        if (firstSeparator == -1) {
            return new ParsedCuiMessage(multi, type, List.of());
        }

        List<String> params = new ArrayList<>();
        int paramStart = firstSeparator + 1;
        int separator;
        while ((separator = message.indexOf('|', paramStart)) != -1) {
            params.add(message.substring(paramStart, separator));
            paramStart = separator + 1;
        }
        params.add(message.substring(paramStart));

        return new ParsedCuiMessage(multi, type, params);
    }

    record ParsedCuiMessage(boolean multi, String type, List<String> params) {}
}
