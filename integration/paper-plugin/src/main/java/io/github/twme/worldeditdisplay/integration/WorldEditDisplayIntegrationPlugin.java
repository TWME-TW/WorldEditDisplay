package io.github.twme.worldeditdisplay.integration;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Paper fixture that drives WorldEditDisplay through its WorldEdit CUI input. */
public final class WorldEditDisplayIntegrationPlugin extends JavaPlugin {
    private static final String CUI_CHANNEL = "worldedit:cui";
    private final Map<UUID, List<String>> currentSelections = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        // WorldEditDisplay is a hard dependency, so its PacketEvents listeners are ready first.
        // Emulate WorldEdit's behaviour: a CUI version handshake requests the current selection.
        getServer().getMessenger().registerIncomingPluginChannel(this, CUI_CHANNEL, (channel, player, message) -> {
            String payload = new String(message, StandardCharsets.UTF_8);
            if (!payload.startsWith("v|")) return;

            List<String> selection = currentSelections.get(player.getUniqueId());
            if (selection == null || selection.isEmpty()) return;
            getServer().getScheduler().runTask(this, () -> selection.forEach(cui -> sendCui(player, cui)));
        });
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] arguments
    ) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        Plugin worldEditDisplay = getServer().getPluginManager().getPlugin("WorldEditDisplay");
        if (worldEditDisplay == null || !worldEditDisplay.isEnabled()) {
            player.sendMessage("WED_ERROR:plugin unavailable");
            return true;
        }

        try {
            Method managerGetter = worldEditDisplay.getClass().getMethod("getVirtualEntityManager");
            Method factoryGetter = worldEditDisplay.getClass().getMethod("getPacketShapeFactory");
            if (managerGetter.invoke(worldEditDisplay) == null || factoryGetter.invoke(worldEditDisplay) == null) {
                player.sendMessage("WED_ERROR:VirtualEntities lifecycle unavailable");
                return true;
            }
        } catch (ReflectiveOperationException exception) {
            getLogger().severe("Unable to inspect the WorldEditDisplay VirtualEntities lifecycle: " + exception);
            player.sendMessage("WED_ERROR:VirtualEntities lifecycle inspection failed");
            return true;
        }

        if (arguments.length > 0 && arguments[0].equalsIgnoreCase("cui-state")) {
            long delayTicks = arguments.length > 1 && arguments[1].equalsIgnoreCase("settled") ? 5L : 1L;
            getServer().getScheduler().runTaskLater(
                    this,
                    () -> reportCuiState(player, worldEditDisplay),
                    delayTicks
            );
            return true;
        }
        if (arguments.length > 0 && arguments[0].equalsIgnoreCase("cui-forward")) {
            sendCui(player, "u|0");
            return true;
        }

        int x = player.getLocation().getBlockX() + 2;
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ() + 2;
        String selection = "s|cuboid";
        String firstPoint = "p|0|" + x + "|" + y + "|" + z + "|27";
        String secondPoint = "p|1|" + (x + 2) + "|" + (y + 2) + "|" + (z + 2) + "|27";
        currentSelections.put(player.getUniqueId(), List.of(selection, firstPoint, secondPoint));
        sendCui(player, selection);
        sendCui(player, firstPoint);
        sendCui(player, secondPoint);

        getServer().getScheduler().runTaskLater(this, () -> {
            // Re-send an unchanged point after the first render has settled. This
            // forces a second retained render pass through the real CUI path.
            sendCui(player, secondPoint);
            getServer().getScheduler().runTaskLater(
                    this,
                    () -> reportRendererState(player, worldEditDisplay),
                    20L
            );
        }, 20L);
        return true;
    }

    private void sendCui(Player player, String message) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                new WrapperPlayServerPluginMessage(CUI_CHANNEL, message.getBytes(StandardCharsets.UTF_8))
        );
    }

    private void reportRendererState(Player player, Plugin worldEditDisplay) {
        try {
            Object renderManager = worldEditDisplay.getClass().getMethod("getRenderManager").invoke(worldEditDisplay);
            int entityCount = (int) renderManager.getClass()
                    .getMethod("getPlayerEntityCount", java.util.UUID.class)
                    .invoke(renderManager, player.getUniqueId());
            int retainedLineCount = (int) renderManager.getClass()
                    .getMethod("getPlayerRetainedLineCount", java.util.UUID.class)
                    .invoke(renderManager, player.getUniqueId());
            int retainedLineEntityCount = (int) renderManager.getClass()
                    .getMethod("getPlayerRetainedLineEntityCount", java.util.UUID.class)
                    .invoke(renderManager, player.getUniqueId());
            Object retainedLineStats = renderManager.getClass()
                    .getMethod("getPlayerRetainedLineStats", java.util.UUID.class)
                    .invoke(renderManager, player.getUniqueId());
            int reusedLines = (int) retainedLineStats.getClass().getMethod("reusedLines").invoke(retainedLineStats);
            int spawnedLines = (int) retainedLineStats.getClass().getMethod("spawnedLines").invoke(retainedLineStats);
            int removedLines = (int) retainedLineStats.getClass().getMethod("removedLines").invoke(retainedLineStats);
            player.sendMessage("WED_READY:" + entityCount
                    + ":" + retainedLineCount
                    + ":" + retainedLineEntityCount
                    + ":" + reusedLines
                    + ":" + spawnedLines
                    + ":" + removedLines);
        } catch (ReflectiveOperationException exception) {
            getLogger().severe("Unable to inspect the WorldEditDisplay renderer: " + exception);
            player.sendMessage("WED_ERROR:renderer inspection failed");
        }
    }

    private void reportCuiState(Player player, Plugin worldEditDisplay) {
        try {
            ClassLoader pluginClassLoader = worldEditDisplay.getClass().getClassLoader();
            Class<?> playerDataClass = Class.forName(
                    "dev.twme.worldeditdisplay.player.PlayerData",
                    true,
                    pluginClassLoader
            );
            Object playerData = playerDataClass
                    .getMethod("getPlayerData", Player.class)
                    .invoke(null, player);
            boolean cuiEnabled = (boolean) playerDataClass
                    .getMethod("isCuiEnabled")
                    .invoke(playerData);
            boolean renderingEnabled = (boolean) playerDataClass
                    .getMethod("isRenderingEnabled")
                    .invoke(playerData);
            boolean serverRendererForced = (boolean) playerDataClass
                    .getMethod("isServerRendererForced")
                    .invoke(playerData);

            Object renderManager = worldEditDisplay.getClass().getMethod("getRenderManager").invoke(worldEditDisplay);
            int entityCount = (int) renderManager.getClass()
                    .getMethod("getPlayerEntityCount", java.util.UUID.class)
                    .invoke(renderManager, player.getUniqueId());
            player.sendMessage("WED_CUI_STATE:" + cuiEnabled + ":" + renderingEnabled + ":" + serverRendererForced + ":" + entityCount);
        } catch (ReflectiveOperationException exception) {
            getLogger().severe("Unable to inspect WorldEditDisplay CUI state: " + exception);
            player.sendMessage("WED_ERROR:CUI state inspection failed");
        }
    }
}
