package dev.twme.worldeditdisplay.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.util.MessageUtil;

/**
 * 重新載入配置命令
 */
public class ReloadCommand implements CommandExecutor {
    
    private final WorldEditDisplay plugin;
    
    public ReloadCommand(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        // 檢查權限
        if (!sender.hasPermission("worldeditdisplay.reload")) {
            MessageUtil.sendTranslated(sender, "general.no_permission");
            return true;
        }
        
        try {
            // 重新載入配置
            plugin.getRenderSettings().reload();
            // 重新載入語言檔案
            plugin.getLanguageManager().reload();
            MessageUtil.sendTranslated(sender, "general.reload_success");
            return true;
            
        } catch (Exception e) {
            MessageUtil.sendTranslated(sender, "general.reload_failed");
            return true;
        }
    }
}
