package dev.twme.worldeditdisplay.listener;

import java.nio.charset.StandardCharsets;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.common.Constants;
import dev.twme.worldeditdisplay.player.PlayerData;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;

/**
 * Listens to incoming plugin messages.
 * Detects CUI registration or version handshakes and marks players as having CUI enabled.
 */
public class InboundPacketListener implements PacketListener {
    private final WorldEditDisplay plugin;

    /**
     * Guard flag: set to {@code false} by {@link #deactivate()} during
     * {@code onDisable()} so that in-flight Netty dispatches bail out
     * at the earliest possible point — before any plugin class loading
     * that would hit the already-closed PluginClassLoader ZipFile.
     */
    private volatile boolean active = true;

    public InboundPacketListener(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    /** Called from onDisable to prevent the classloader-zip-closed race. */
    public void deactivate() {
        this.active = false;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!active) return;

        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

        // Catch exceptions for oversized packets (e.g., mods like Axiom)
        WrapperPlayClientPluginMessage packet;
        try {
            packet = new WrapperPlayClientPluginMessage(event);
        } catch (Exception e) {
            return; // ignore invalid packets
        }

        if (!isCuiHandshake(packet.getChannelName(), packet.getData())) return;

        Player player = event.getPlayer();
        PlayerData playerData = PlayerData.getPlayerData(player);
        if (playerData.isCuiEnabled()) return;

        playerData.setCuiEnabled(true);
        playerData.cancelPendingRender();
        FoliaScheduler.getEntityScheduler().run(player, plugin, ignored -> {
            if (!active || plugin.getRenderManager() == null) return;
            plugin.getRenderManager().clearRender(player.getUniqueId());
        }, null);
    }

    static boolean isCuiHandshake(String channel, byte[] data) {
        if (data == null) return false;

        if (Constants.CUI_CHANNEL.equals(channel)) {
            String message = new String(data, StandardCharsets.UTF_8);
            return message.startsWith("v|") && message.length() > 2;
        }

        return Constants.REGISTER_CHANNEL.equals(channel)
                && containsRegisteredChannel(data, Constants.CUI_CHANNEL);
    }

    private static boolean containsRegisteredChannel(byte[] data, String expectedChannel) {
        byte[] expected = expectedChannel.getBytes(StandardCharsets.UTF_8);
        int channelStart = 0;

        for (int i = 0; i <= data.length; i++) {
            if (i < data.length && data[i] != 0) continue;

            int channelLength = i - channelStart;
            if (channelLength == expected.length) {
                boolean matches = true;
                for (int j = 0; j < expected.length; j++) {
                    if (data[channelStart + j] != expected[j]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) return true;
            }
            channelStart = i + 1;
        }
        return false;
    }
}
