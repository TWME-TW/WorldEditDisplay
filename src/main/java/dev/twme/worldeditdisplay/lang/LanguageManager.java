package dev.twme.worldeditdisplay.lang;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages multi-language support.
 * Loads, stores, and provides translations for different languages.
 */
public class LanguageManager {

    private final WorldEditDisplay plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLanguage = "en_us";

    public LanguageManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the language system
     */
    public void initialize() {
        // Make sure the lang folder exists
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        // Load default language files
        loadDefaultLanguages();

        // Get default language from config
        defaultLanguage = plugin.getConfig().getString("language.default", "en_us");

        plugin.getLogger().info("Language system initialized, default language: " + defaultLanguage);
    }

    /**
     * Load default languages (like zh_tw, en_us)
     */
    private void loadDefaultLanguages() {
        String[] defaultLanguages = {"zh_tw", "en_us"};
        for (String lang : defaultLanguages) {
            saveDefaultLanguage(lang);
            loadLanguage(lang);
        }
    }

    /**
     * Save default language file if it doesn't exist
     */
    private void saveDefaultLanguage(String lang) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            try {
                InputStream in = plugin.getResource("lang/" + lang + ".yml");
                if (in != null) {
                    java.nio.file.Files.copy(in, langFile.toPath());
                    in.close();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save default language file: " + lang, e);
            }
        }
    }

    /**
     * Load a language file from disk
     */
    public void loadLanguage(String lang) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file does not exist: " + lang);
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);

            // load defaults from resource
            InputStream defConfigStream = plugin.getResource("lang/" + lang + ".yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defConfig);
            }

            languages.put(lang, config);
            plugin.getLogger().info("Loaded language: " + lang);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load language file: " + lang, e);
        }
    }

    /**
     * Reload all languages
     */
    public void reload() {
        languages.clear();
        loadDefaultLanguages();
    }

    /**
     * Get player's language
     */
    public String getPlayerLanguage(Player player) {
        return playerLanguages.getOrDefault(player.getUniqueId(), getClientLanguage(player));
    }

    /**
     * Set a player's language
     */
    public void setPlayerLanguage(UUID uuid, String language) {
        playerLanguages.put(uuid, language);
    }

    /**
     * Remove a player's language record
     */
    public void removePlayerLanguage(UUID uuid) {
        playerLanguages.remove(uuid);
    }

    /**
     * Get the client's language
     */
    private String getClientLanguage(Player player) {
        try {
            String clientLocale = player.locale().toString();
            clientLocale = clientLocale.toLowerCase().replace("-", "_");

            // check if exact match exists
            if (languages.containsKey(clientLocale)) return clientLocale;

            // fallback: just use language code (like zh)
            String langCode = clientLocale.split("_")[0];
            for (String lang : languages.keySet()) {
                if (lang.startsWith(langCode)) return lang;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get player language settings", e);
        }

        return defaultLanguage; // fallback to default
    }

    /**
     * Get translated message for a player
     */
    public String getMessage(Player player, String key, Object... args) {
        String lang = getPlayerLanguage(player);
        return getMessage(lang, key, args);
    }

    /**
     * Get translated message for a specific language
     */
    public String getMessage(String lang, String key, Object... args) {
        YamlConfiguration config = languages.get(lang);

        // fallback to default language if missing
        if (config == null) config = languages.get(defaultLanguage);

        // still missing? return key
        if (config == null) return key;

        String message = config.getString(key);
        if (message == null) return key;

        if (args.length > 0) message = String.format(message, args);

        return message;
    }

    /**
     * Check if a language exists
     */
    public boolean hasLanguage(String lang) {
        return languages.containsKey(lang);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public Map<String, YamlConfiguration> getLanguages() {
        return new HashMap<>(languages);
    }

    public boolean isLanguageAvailable(String lang) {
        return languages.containsKey(lang);
    }

    public java.util.Set<String> getAvailableLanguages() {
        return languages.keySet();
    }
}
