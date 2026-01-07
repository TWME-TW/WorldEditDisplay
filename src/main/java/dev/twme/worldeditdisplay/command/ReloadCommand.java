package dev.twme.worldeditdisplay.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.util.MessageUtil;

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
            MessageUtil.sendTranslated(sender, "general.reload_success");
            return true;
            
        } catch (Exception e) {
            MessageUtil.sendTranslated(sender, "general.reload_failed");
            return true;
        }
    }
}
