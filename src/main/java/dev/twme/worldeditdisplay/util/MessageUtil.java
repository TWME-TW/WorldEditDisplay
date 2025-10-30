package dev.twme.worldeditdisplay.util;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * MiniMessage 訊息工具類
 * 提供統一的訊息格式化和發送方法
 * 支援多語言系統
 */
public class MessageUtil {
    
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static WorldEditDisplay plugin;
    
    /**
     * 初始化 MessageUtil
     */
    public static void initialize(WorldEditDisplay instance) {
        plugin = instance;
    }
    
    /**
     * 發送 MiniMessage 格式的訊息給命令發送者
     * 
     * @param sender 接收者
     * @param message MiniMessage 格式的訊息
     */
    public static void sendMessage(CommandSender sender, String message) {
        Component component = miniMessage.deserialize(message);
        sender.sendMessage(component);
    }
    
    /**
     * 發送 MiniMessage 格式的訊息給玩家
     * 
     * @param player 接收者
     * @param message MiniMessage 格式的訊息
     */
    public static void sendMessage(Player player, String message) {
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }
    
    /**
     * 發送多語言訊息給玩家
     * 
     * @param player 接收者
     * @param key 語言鍵值
     * @param args 格式化參數
     */
    public static void sendTranslated(Player player, String key, Object... args) {
        if (plugin == null) {
            sendMessage(player, key);
            return;
        }
        
        LanguageManager langManager = plugin.getLanguageManager();
        String message = langManager.getMessage(player, key, args);
        sendMessage(player, message);
    }
    
    /**
     * 發送多語言訊息給命令發送者
     * 
     * @param sender 接收者
     * @param key 語言鍵值
     * @param args 格式化參數
     */
    public static void sendTranslated(CommandSender sender, String key, Object... args) {
        if (sender instanceof Player) {
            sendTranslated((Player) sender, key, args);
        } else {
            // 非玩家使用預設語言
            if (plugin == null) {
                sendMessage(sender, key);
                return;
            }
            
            LanguageManager langManager = plugin.getLanguageManager();
            String message = langManager.getMessage(langManager.getDefaultLanguage(), key, args);
            sendMessage(sender, message);
        }
    }
    
    /**
     * 取得翻譯後的訊息（不發送）
     * 
     * @param player 玩家
     * @param key 語言鍵值
     * @param args 格式化參數
     * @return 翻譯後的訊息
     */
    public static String getTranslated(Player player, String key, Object... args) {
        if (plugin == null) {
            return key;
        }
        
        LanguageManager langManager = plugin.getLanguageManager();
        return langManager.getMessage(player, key, args);
    }
    
    /**
     * 格式化 MiniMessage 字串
     * 
     * @param message MiniMessage 格式的訊息
     * @param args 格式化參數
     * @return 格式化後的訊息
     */
    public static String format(String message, Object... args) {
        return String.format(message, args);
    }
}
