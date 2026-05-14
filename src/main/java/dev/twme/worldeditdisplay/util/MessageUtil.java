package dev.twme.worldeditdisplay.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Handles sending MiniMessage formatted messages.
 * Supports translations via LanguageManager.
 */
public class MessageUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    /** Fallback for console on non-Paper servers (no click events needed for console). */
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private static WorldEditDisplay plugin;

    /**
     * True when running on Paper/Folia: native Adventure sendMessage supports click events.
     * False on Spigot: must use BungeeCord BaseComponent[] to preserve click/hover events.
     */
    private static final boolean IS_PAPER;
    static {
        boolean paper = false;
        try {
            Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            paper = true;
        } catch (ClassNotFoundException ignored) {
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig");
                paper = true;
            } catch (ClassNotFoundException ignored2) {}
        }
        IS_PAPER = paper;
    }

    // set the plugin instance
    public static void initialize(WorldEditDisplay instance) {
        plugin = instance;
    }

    // send a raw MiniMessage string to any sender
    public static void sendMessage(CommandSender sender, String message) {
        sendComponent(sender, miniMessage.deserialize(message));
    }

    // send a raw MiniMessage string to a player
    public static void sendMessage(Player player, String message) {
        sendComponent(player, miniMessage.deserialize(message));
    }

    /**
     * Sends an Adventure Component while preserving click/hover events on all platforms.
     * - Paper/Folia: native Adventure API (full support).
     * - Spigot players: BungeeCord BaseComponent[] via player.spigot().sendMessage().
     * - Spigot console: legacy string (console has no interactive events anyway).
     */
    private static void sendComponent(CommandSender sender, Component component) {
        if (IS_PAPER) {
            // Paper/Folia: CommandSender implements Adventure Audience natively; full click/hover support.
            ((net.kyori.adventure.audience.Audience) sender).sendMessage(component);
        } else if (sender instanceof Player player) {
            player.spigot().sendMessage(BungeeComponentSerializer.get().serialize(component));
        } else {
            sender.sendMessage(legacySerializer.serialize(component));
        }
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
