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
 * 多語言管理器
 * 負責載入、管理和提供多語言翻譯
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
     * 初始化語言系統
     */
    public void initialize() {
        // 建立語言資料夾
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        // 載入預設語言檔案
        loadDefaultLanguages();
        
        // 從設定載入預設語言
        defaultLanguage = plugin.getConfig().getString("language.default", "en_us");
        
        plugin.getLogger().info("Language system initialized, default language: " + defaultLanguage);
    }
    
    /**
     * 載入預設語言檔案
     */
    private void loadDefaultLanguages() {
        String[] defaultLanguages = {"zh_tw", "en_us"};
        
        for (String lang : defaultLanguages) {
            saveDefaultLanguage(lang);
            loadLanguage(lang);
        }
    }
    
    /**
     * 儲存預設語言檔案
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
     * 載入語言檔案
     */
    public void loadLanguage(String lang) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file does not exist: " + lang);
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            
            // 同時從資源中載入預設值
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
     * 重新載入所有語言檔案
     */
    public void reload() {
        languages.clear();
        loadDefaultLanguages();
    }
    
    /**
     * 取得玩家的語言
     */
    public String getPlayerLanguage(Player player) {
        return playerLanguages.getOrDefault(player.getUniqueId(), getClientLanguage(player));
    }
    
    /**
     * 設定玩家的語言
     */
    public void setPlayerLanguage(UUID uuid, String language) {
        playerLanguages.put(uuid, language);
    }
    
    /**
     * 移除玩家的語言記錄
     */
    public void removePlayerLanguage(UUID uuid) {
        playerLanguages.remove(uuid);
    }
    
    /**
     * 取得玩家客戶端語言
     */
    private String getClientLanguage(Player player) {
        try {
            String clientLocale = player.locale().toString();
            // 轉換格式: zh_TW -> zh_tw
            clientLocale = clientLocale.toLowerCase().replace("-", "_");
            
            // 檢查是否有對應的語言檔案
            if (languages.containsKey(clientLocale)) {
                return clientLocale;
            }
            
            // 嘗試只用語言代碼 (例如 zh)
            String langCode = clientLocale.split("_")[0];
            for (String lang : languages.keySet()) {
                if (lang.startsWith(langCode)) {
                    return lang;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get player language settings", e);
        }
        
        return defaultLanguage;
    }
    
    /**
     * 取得翻譯訊息
     */
    public String getMessage(Player player, String key, Object... args) {
        String lang = getPlayerLanguage(player);
        return getMessage(lang, key, args);
    }
    
    /**
     * 取得翻譯訊息（指定語言）
     */
    public String getMessage(String lang, String key, Object... args) {
        YamlConfiguration config = languages.get(lang);
        
        // 如果找不到語言檔，使用預設語言
        if (config == null) {
            config = languages.get(defaultLanguage);
        }
        
        // 如果還是找不到，返回鍵值
        if (config == null) {
            return key;
        }
        
        String message = config.getString(key);
        
        // 如果找不到翻譯，返回鍵值
        if (message == null) {
            return key;
        }
        
        // 格式化參數
        if (args.length > 0) {
            message = String.format(message, args);
        }
        
        return message;
    }
    
    /**
     * 檢查語言是否存在
     */
    public boolean hasLanguage(String lang) {
        return languages.containsKey(lang);
    }
    
    /**
     * 取得預設語言
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * 取得所有可用語言
     */
    public Map<String, YamlConfiguration> getLanguages() {
        return new HashMap<>(languages);
    }
    
    /**
     * 檢查語言是否可用
     */
    public boolean isLanguageAvailable(String lang) {
        return languages.containsKey(lang);
    }
    
    /**
     * 取得所有可用語言代碼
     */
    public java.util.Set<String> getAvailableLanguages() {
        return languages.keySet();
    }
}
