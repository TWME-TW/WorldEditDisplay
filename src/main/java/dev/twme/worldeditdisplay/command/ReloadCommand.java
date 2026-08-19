package dev.twme.worldeditdisplay.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.util.MessageUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;

public class ReloadCommand implements CommandExecutor {
    
    private final WorldEditDisplay plugin;
    
    public ReloadCommand(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("worldeditdisplay.reload")) {
            MessageUtil.sendTranslated(sender, "general.no_permission");
            return true;
        }
        
        try {
            plugin.getRenderSettings().reload();
            plugin.getLanguageManager().reload();
            if (plugin.getShareManager() != null) plugin.getShareManager().reloadConfig();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                FoliaScheduler.getEntityScheduler().execute(player, plugin, () -> {
                    if (player.isOnline()) plugin.getRenderManager().refreshPlayerRenderer(player);
                }, null, 1L);
            }
            MessageUtil.sendTranslated(sender, "general.reload_success");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to reload WorldEditDisplay", e);
            MessageUtil.sendTranslated(sender, "general.reload_failed");
            return true;
        }
    }
}
