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

/** Paper fixture that drives WorldEditDisplay through its WorldEdit CUI input. */
public final class WorldEditDisplayIntegrationPlugin extends JavaPlugin {
    private static final String CUI_CHANNEL = "worldedit:cui";

    @Override
    public void onEnable() {
        // WorldEditDisplay is a hard dependency, so its PacketEvents listeners are ready first.
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

        int x = player.getLocation().getBlockX() + 2;
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ() + 2;
        String secondPoint = "p|1|" + (x + 2) + "|" + (y + 2) + "|" + (z + 2) + "|27";
        sendCui(player, "s|cuboid");
        sendCui(player, "p|0|" + x + "|" + y + "|" + z + "|27");
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
}
