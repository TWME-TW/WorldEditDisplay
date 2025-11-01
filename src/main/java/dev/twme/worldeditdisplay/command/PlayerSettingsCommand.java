package dev.twme.worldeditdisplay.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.util.MessageUtil;

/**
 * 玩家個人設定命令
 * 
 * 用法:
 * /wedisplay set <renderer> <setting> <value> - 設定個人參數
 * /wedisplay reset <renderer> [setting] - 重置為伺服器預設
 * /wedisplay show [renderer] - 顯示當前設定
 * /wedisplay reloadplayer - 重載自己的設定
 */
public class PlayerSettingsCommand implements CommandExecutor {
    
    private final WorldEditDisplay plugin;
    
    public PlayerSettingsCommand(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                           @NotNull String label, @NotNull String[] args) {
        
        if (!(sender instanceof Player)) {
            MessageUtil.sendTranslated(sender, "general.player_only");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 檢查玩家是否有使用設定權限
        if (!player.hasPermission("worldeditdisplay.use.settings")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "set":
                return handleSet(player, args);
            case "reset":
                return handleReset(player, args);
            case "show":
                return handleShow(player, args);
            case "reloadplayer":
                return handleReload(player);
            case "lang":
            case "language":
                return handleLanguage(player, args);
            case "toggle":
                return handleToggle(player);
            default:
                sendHelp(player);
                return true;
        }
    }
    
    /**
     * 處理 set 命令
     */
    private boolean handleSet(Player player, String[] args) {
        if (args.length < 4) {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.usage");
            MessageUtil.sendTranslated(player, "command.wedisplay.set.example");
            return true;
        }
        
        String renderer = args[1].toLowerCase();
        String setting = args[2].toLowerCase();
        String value = args[3];
        
        // 驗證渲染器類型
        if (!isValidRenderer(renderer)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.invalid_renderer");
            MessageUtil.sendTranslated(player, "command.wedisplay.set.available_renderers");
            return true;
        }
        
        // 建構設定路徑
        String path = String.format("renderer.%s.%s", renderer, setting);
        
        // 取得玩家設定
        PlayerRenderSettings settings = plugin.getPlayerSettingsManager().getSettings(player);
        
        // 嘗試設定值
        Object parsedValue = parseValue(setting, value);
        if (parsedValue == null) {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.invalid_value");
            return true;
        }
        
        boolean success = settings.set(path, parsedValue);
        
        if (success) {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.success", renderer, setting, value);
            MessageUtil.sendTranslated(player, "command.wedisplay.set.success_hint");
            
            // 重新渲染玩家的選區
            plugin.getRenderManager().refreshPlayerRenderer(player);
        } else {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.failed");
            MessageUtil.sendTranslated(player, "command.wedisplay.set.failed_hint");
        }
        
        return true;
    }
    
    /**
     * 處理 reset 命令
     */
    private boolean handleReset(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.usage");
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.example1");
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.example2");
            return true;
        }
        
        String renderer = args[1].toLowerCase();
        
        // 驗證渲染器類型
        if (!isValidRenderer(renderer)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.invalid_renderer");
            return true;
        }
        
        PlayerRenderSettings settings = plugin.getPlayerSettingsManager().getSettings(player);
        
        if (args.length >= 3) {
            // 重置特定設定項
            String setting = args[2].toLowerCase();
            String path = String.format("renderer.%s.%s", renderer, setting);
            settings.reset(path);
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.success_setting", renderer, setting);
        } else {
            // 重置整個渲染器
            String path = String.format("renderer.%s", renderer);
            settings.reset(path);
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.success_all", renderer);
        }
        
        // 重新渲染玩家的選區
        plugin.getRenderManager().refreshPlayerRenderer(player);
        
        return true;
    }
    
    /**
     * 處理 show 命令
     */
    private boolean handleShow(Player player, String[] args) {
        PlayerRenderSettings settings = plugin.getPlayerSettingsManager().getSettings(player);
        
        if (args.length >= 2) {
            // 顯示特定渲染器的設定
            String renderer = args[1].toLowerCase();
            if (!isValidRenderer(renderer)) {
                MessageUtil.sendTranslated(player, "command.wedisplay.show.invalid_renderer");
                return true;
            }
            
            showRendererSettings(player, renderer, settings);
        } else {
            // 顯示所有設定摘要
            MessageUtil.sendTranslated(player, "command.wedisplay.show.title");
            MessageUtil.sendTranslated(player, "command.wedisplay.show.hint");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendTranslated(player, "command.wedisplay.show.available");
            MessageUtil.sendTranslated(player, "command.wedisplay.show.cuboid");
            MessageUtil.sendTranslated(player, "command.wedisplay.show.cylinder");
            MessageUtil.sendTranslated(player, "command.wedisplay.show.ellipsoid");
            MessageUtil.sendTranslated(player, "command.wedisplay.show.polygon");
            MessageUtil.sendTranslated(player, "command.wedisplay.show.polyhedron");
        }
        
        return true;
    }
    
    /**
     * 處理 reload 命令
     */
    private boolean handleReload(Player player) {
        plugin.getPlayerSettingsManager().reloadSettings(player);
        MessageUtil.sendTranslated(player, "command.wedisplay.reload.success");
        
        // 重新渲染玩家的選區
        plugin.getRenderManager().refreshPlayerRenderer(player);
        
        return true;
    }
    
    /**
     * 處理 language 命令
     */
    private boolean handleLanguage(Player player, String[] args) {
        // 檢查是否允許玩家更改語言
        if (!plugin.getConfig().getBoolean("language.allow_player_change", true)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.disabled");
            return true;
        }
        
        // 如果沒有參數，顯示當前語言
        if (args.length < 2) {
            String currentLang = plugin.getLanguageManager().getPlayerLanguage(player);
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.current", currentLang);
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.usage");
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.example");
            return true;
        }
        
        String targetLang = args[1].toLowerCase();
        
        // 驗證語言是否存在
        if (!plugin.getLanguageManager().isLanguageAvailable(targetLang)) {
            String availableLangs = String.join(", ", plugin.getLanguageManager().getAvailableLanguages());
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.invalid", availableLangs);
            return true;
        }
        
        // 設定語言
        plugin.getLanguageManager().setPlayerLanguage(player.getUniqueId(), targetLang);
        MessageUtil.sendTranslated(player, "command.wedisplay.lang.success", targetLang);
        
        return true;
    }
    
    /**
     * 處理 toggle 命令 - 切換渲染開關
     */
    private boolean handleToggle(Player player) {
        PlayerData playerData = PlayerData.getPlayerData(player);
        
        // 切換渲染狀態
        boolean newState = !playerData.isRenderingEnabled();
        playerData.setRenderingEnabled(newState);
        
        // 發送相應訊息
        if (newState) {
            MessageUtil.sendTranslated(player, "command.wedisplay.toggle.enabled");
        } else {
            MessageUtil.sendTranslated(player, "command.wedisplay.toggle.disabled");
            // 關閉渲染時清除所有現有渲染
            plugin.getRenderManager().clearRender(player.getUniqueId());
        }
        
        return true;
    }
    
    /**
     * 顯示特定渲染器的設定
     */
    private void showRendererSettings(Player player, String renderer, PlayerRenderSettings settings) {
        MessageUtil.sendTranslated(player, "command.wedisplay.show.renderer_title", renderer.toUpperCase());
        
        switch (renderer) {
            case "cuboid":
                MessageUtil.sendTranslated(player, "settings.edge_material", settings.getCuboidEdgeMaterial());
                MessageUtil.sendTranslated(player, "settings.point1_material", settings.getCuboidPoint1Material());
                MessageUtil.sendTranslated(player, "settings.point2_material", settings.getCuboidPoint2Material());
                MessageUtil.sendTranslated(player, "settings.grid_material", settings.getCuboidGridMaterial());
                MessageUtil.sendTranslated(player, "settings.edge_thickness", settings.getCuboidEdgeThickness());
                MessageUtil.sendTranslated(player, "settings.grid_thickness", settings.getCuboidGridThickness());
                MessageUtil.sendTranslated(player, "settings.vertex_marker_size", settings.getCuboidVertexMarkerSize());
                MessageUtil.sendTranslated(player, "settings.height_grid_division", settings.getCuboidHeightGridDivision());
                break;
                
            case "cylinder":
                MessageUtil.sendTranslated(player, "settings.circle_material", settings.getCylinderCircleMaterial());
                MessageUtil.sendTranslated(player, "settings.grid_material", settings.getCylinderGridMaterial());
                MessageUtil.sendTranslated(player, "settings.center_material", settings.getCylinderCenterMaterial());
                MessageUtil.sendTranslated(player, "settings.center_line_material", settings.getCylinderCenterLineMaterial());
                MessageUtil.sendTranslated(player, "settings.circle_thickness", settings.getCylinderCircleThickness());
                MessageUtil.sendTranslated(player, "settings.grid_thickness", settings.getCylinderGridThickness());
                MessageUtil.sendTranslated(player, "settings.center_line_thickness", settings.getCylinderCenterLineThickness());
                MessageUtil.sendTranslated(player, "settings.center_thickness", settings.getCylinderCenterThickness());
                MessageUtil.sendTranslated(player, "settings.min_circle_segments", settings.getCylinderMinCircleSegments());
                MessageUtil.sendTranslated(player, "settings.max_circle_segments", settings.getCylinderMaxCircleSegments());
                MessageUtil.sendTranslated(player, "settings.target_segment_length", settings.getCylinderTargetSegmentLength());
                MessageUtil.sendTranslated(player, "settings.height_grid_division", settings.getCylinderHeightGridDivision());
                MessageUtil.sendTranslated(player, "settings.radius_grid_division", settings.getCylinderRadiusGridDivision());
                break;
                
            case "ellipsoid":
                MessageUtil.sendTranslated(player, "settings.line_material", settings.getEllipsoidLineMaterial());
                MessageUtil.sendTranslated(player, "settings.center_line_material", settings.getEllipsoidCenterLineMaterial());
                MessageUtil.sendTranslated(player, "settings.center_material", settings.getEllipsoidCenterMaterial());
                MessageUtil.sendTranslated(player, "settings.line_thickness", settings.getEllipsoidLineThickness());
                MessageUtil.sendTranslated(player, "settings.center_line_thickness", settings.getEllipsoidCenterLineThickness());
                MessageUtil.sendTranslated(player, "settings.center_marker_size", settings.getEllipsoidCenterMarkerSize());
                MessageUtil.sendTranslated(player, "settings.center_thickness", settings.getEllipsoidCenterThickness());
                MessageUtil.sendTranslated(player, "settings.min_segments", settings.getEllipsoidMinSegments());
                MessageUtil.sendTranslated(player, "settings.max_segments", settings.getEllipsoidMaxSegments());
                MessageUtil.sendTranslated(player, "settings.target_segment_length", settings.getEllipsoidTargetSegmentLength());
                MessageUtil.sendTranslated(player, "settings.radius_grid_division", settings.getEllipsoidRadiusGridDivision());
                break;
                
            case "polygon":
                MessageUtil.sendTranslated(player, "settings.edge_material", settings.getPolygonEdgeMaterial());
                MessageUtil.sendTranslated(player, "settings.vertex_material", settings.getPolygonVertexMaterial());
                MessageUtil.sendTranslated(player, "settings.vertical_material", settings.getPolygonVerticalMaterial());
                MessageUtil.sendTranslated(player, "settings.edge_thickness", settings.getPolygonEdgeThickness());
                MessageUtil.sendTranslated(player, "settings.vertical_thickness", settings.getPolygonVerticalThickness());
                MessageUtil.sendTranslated(player, "settings.height_grid_division", settings.getPolygonHeightGridDivision());
                break;
                
            case "polyhedron":
                MessageUtil.sendTranslated(player, "settings.line_material", settings.getPolyhedronLineMaterial());
                MessageUtil.sendTranslated(player, "settings.vertex0_material", settings.getPolyhedronVertex0Material());
                MessageUtil.sendTranslated(player, "settings.vertex_material", settings.getPolyhedronVertexMaterial());
                MessageUtil.sendTranslated(player, "settings.line_thickness", settings.getPolyhedronLineThickness());
                MessageUtil.sendTranslated(player, "settings.vertex_size", settings.getPolyhedronVertexSize());
                MessageUtil.sendTranslated(player, "settings.vertex_thickness", settings.getPolyhedronVertexThickness());
                break;
        }
        
        MessageUtil.sendTranslated(player, "command.wedisplay.show.modify_hint");
    }
    
    /**
     * 顯示幫助訊息
     */
    private void sendHelp(Player player) {
        MessageUtil.sendTranslated(player, "command.wedisplay.help.title");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.set");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.set_desc");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.reset");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.reset_desc");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.show");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.show_desc");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.reload");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.reload_desc");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.lang");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.lang_desc");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.toggle");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.toggle_desc");
    }
    
    /**
     * 驗證渲染器類型
     */
    private boolean isValidRenderer(String renderer) {
        return renderer.equals("cuboid") || renderer.equals("cylinder") ||
               renderer.equals("ellipsoid") || renderer.equals("polygon") ||
               renderer.equals("polyhedron");
    }
    
    /**
     * 解析設定值
     */
    private Object parseValue(String setting, String value) {
        if (setting.contains("material")) {
            // 材質類型
            try {
                return Material.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else if (setting.contains("thickness") || setting.contains("size") || 
                   setting.contains("length") || setting.contains("factor")) {
            // 浮點數類型
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            // 整數類型
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
