package dev.twme.worldeditdisplay.util;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles sending MiniMessage formatted messages.
 * Supports translations via LanguageManager.
 */
public class MessageUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static WorldEditDisplay plugin;

    // set the plugin instance
    public static void initialize(WorldEditDisplay instance) {
        plugin = instance;
    }

    // send a raw MiniMessage string to any sender
    public static void sendMessage(CommandSender sender, String message) {
        Component component = miniMessage.deserialize(message);
        sender.sendMessage(component);
    }

    // send a raw MiniMessage string to a player
    public static void sendMessage(Player player, String message) {
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }

    // send translated message to player
    public static void sendTranslated(Player player, String key, Object... args) {
        if (plugin == null) {
            sendMessage(player, key);
            return;
        }

        LanguageManager langManager = plugin.getLanguageManager();
        String message = langManager.getMessage(player, key, args);
        sendMessage(player, message);
    }

    // send translated message to sender (player or console)
    public static void sendTranslated(CommandSender sender, String key, Object... args) {
        if (sender instanceof Player) {
            sendTranslated((Player) sender, key, args);
        } else {
            if (plugin == null) {
                sendMessage(sender, key);
                return;
            }

            LanguageManager langManager = plugin.getLanguageManager();
            String message = langManager.getMessage(langManager.getDefaultLanguage(), key, args);
            sendMessage(sender, message);
        }
    }

    // get translated message without sending
    public static String getTranslated(Player player, String key, Object... args) {
        if (plugin == null) return key;

        LanguageManager langManager = plugin.getLanguageManager();
        return langManager.getMessage(player, key, args);
    }

    // helper to format MiniMessage strings
    public static String format(String message, Object... args) {
        return String.format(message, args);
    }
}
