package dev.twme.worldeditdisplay;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.plugin.java.JavaPlugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;

import dev.twme.worldeditdisplay.command.PlayerSettingsCommand;
import dev.twme.worldeditdisplay.command.ReloadCommand;
import dev.twme.worldeditdisplay.config.PlayerSettingsManager;
import dev.twme.worldeditdisplay.config.RenderSettings;
import dev.twme.worldeditdisplay.display.RenderManager;
import dev.twme.worldeditdisplay.lang.LanguageManager;
import dev.twme.worldeditdisplay.listener.InboundPacketListener;
import dev.twme.worldeditdisplay.listener.OutboundPacketListener;
import dev.twme.worldeditdisplay.listener.PlayerJoinListener;
import dev.twme.worldeditdisplay.listener.PlayerLocaleChangeListener;
import dev.twme.worldeditdisplay.listener.PlayerQuitListener;
import dev.twme.worldeditdisplay.share.ShareManager;
import dev.twme.worldeditdisplay.util.MessageUtil;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;

public final class WorldEditDisplay extends JavaPlugin {
    private static WorldEditDisplay plugin;
    private RenderManager renderManager;
    private RenderSettings renderSettings;
    private PlayerSettingsManager playerSettingsManager;
    private LanguageManager languageManager;
    private ShareManager shareManager;
    private final Set<UUID> viewAllPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void onLoad() {
        plugin = this;

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .debug(false)
                .checkForUpdates(false);

        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {

        PacketEvents.getAPI().init();

        PacketEvents.getAPI().getEventManager().registerListener(new InboundPacketListener(), PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().getEventManager().registerListener(new OutboundPacketListener(), PacketListenerPriority.NORMAL);

        SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(this);
        APIConfig settings = new APIConfig(PacketEvents.getAPI())
                .usePlatformLogger();
        EntityLib.init(platform, settings);

        // Load default configuration
        saveDefaultConfig();
        
        // Initialize language manager
        this.languageManager = new LanguageManager(this);
        this.languageManager.initialize();
        
        // Initialize MessageUtil with plugin instance
        MessageUtil.initialize(this);
        
        // Initialize render settings manager
        this.renderSettings = new RenderSettings(this);
        this.renderSettings.reload();
        
        // Initialize player settings manager
        this.playerSettingsManager = new PlayerSettingsManager(this);
        
        // Initialize managers
        this.renderManager = new RenderManager(this);

        // Initialize share manager
        this.shareManager = new ShareManager(this);

        // Schedule periodic share save and expiry purge
        int saveIntervalMinutes = Math.max(1, getConfig().getInt("share.auto_save_interval", 5));
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(this, task -> {
            if (shareManager != null) shareManager.save();
        }, saveIntervalMinutes, saveIntervalMinutes, TimeUnit.MINUTES);
        // Purge expired invites every 10 seconds
        FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            if (shareManager != null) shareManager.purgeAllExpiredRequests();
        }, 200L, 200L);

        // Schedule periodic player settings save every 5 minutes
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(this, task -> {
            if (playerSettingsManager != null) playerSettingsManager.saveAllDirty();
        }, 5, 5, TimeUnit.MINUTES);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLocaleChangeListener(this), this);
        
        // Register commands
        getCommand("wedisplayreload").setExecutor(new ReloadCommand(this));
        getCommand("wedisplay").setExecutor(new PlayerSettingsCommand(this));
        
        getLogger().info("WorldEditDisplay enabled - Visualization rendering system ready");
    }

    @Override
    public void onDisable() {
        // Save share data
        if (shareManager != null) {
            shareManager.save();
        }

        // Save any unsaved player render settings
        if (playerSettingsManager != null) {
            playerSettingsManager.saveAllDirty();
        }

        // Clean up all renders
        if (renderManager != null) {
            renderManager.shutdown();
        }
        
        getLogger().info("WorldEditDisplay disabled");
    }

    public static WorldEditDisplay getPlugin() {
        return plugin;
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }
    
    public RenderSettings getRenderSettings() {
        return renderSettings;
    }
    
    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ShareManager getShareManager() {
        return shareManager;
    }

    public Set<UUID> getViewAllPlayers() {
        return viewAllPlayers;
    }
}
