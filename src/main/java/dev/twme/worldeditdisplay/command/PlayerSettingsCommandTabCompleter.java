package dev.twme.worldeditdisplay.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.twme.worldeditdisplay.WorldEditDisplay;

/**
 * 玩家設定命令的 Tab 補全
 */
public class PlayerSettingsCommandTabCompleter implements TabCompleter {
    
    private final WorldEditDisplay plugin;
    private static final List<String> SUB_COMMANDS = Arrays.asList("set", "reset", "show", "reloadplayer", "lang", "language");
    private static final List<String> RENDERERS = Arrays.asList("cuboid", "cylinder", "ellipsoid", "polygon", "polyhedron");
    
    public PlayerSettingsCommandTabCompleter(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        
        if (!(sender instanceof Player)) {
            return null;
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 第一個參數：子命令
            completions = SUB_COMMANDS.stream()
                .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
                
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("set") || subCommand.equals("reset") || subCommand.equals("show")) {
                // 第二個參數：渲染器類型
                completions = RENDERERS.stream()
                    .filter(renderer -> renderer.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (subCommand.equals("lang") || subCommand.equals("language")) {
                // 第二個參數：語言代碼
                completions = new ArrayList<>(plugin.getLanguageManager().getAvailableLanguages());
                completions = completions.stream()
                    .filter(lang -> lang.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String renderer = args[1].toLowerCase();
            
            if (subCommand.equals("set") || subCommand.equals("reset")) {
                // 第三個參數：設定項
                completions = getSettingKeys(renderer).stream()
                    .filter(key -> key.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String setting = args[2].toLowerCase();
            
            if (subCommand.equals("set")) {
                // 第四個參數：設定值
                if (setting.contains("material")) {
                    // 材質類型：提供材質列表
                    completions = getMaterialSuggestions().stream()
                        .filter(mat -> mat.startsWith(args[3].toUpperCase()))
                        .collect(Collectors.toList());
                } else {
                    // 數值類型：提供範例值
                    completions = Arrays.asList("<value>", "1", "0.05", "0.04", "0.03");
                }
            }
        }
        
        return completions;
    }
    
    /**
     * 取得指定渲染器的設定項列表
     */
    private List<String> getSettingKeys(String renderer) {
        List<String> keys = new ArrayList<>();
        
        switch (renderer) {
            case "cuboid":
                keys.addAll(Arrays.asList(
                    "edge_material", "point1_material", "point2_material", "grid_material",
                    "edge_thickness", "grid_thickness", "vertex_marker_size",
                    "height_grid_division"
                ));
                break;
                
            case "cylinder":
                keys.addAll(Arrays.asList(
                    "circle_material", "grid_material", "center_material", "center_line_material",
                    "circle_thickness", "grid_thickness", "center_line_thickness", "center_thickness",
                    "min_circle_segments", "max_circle_segments", "target_segment_length",
                    "height_grid_division", "radius_grid_division"
                ));
                break;
                
            case "ellipsoid":
                keys.addAll(Arrays.asList(
                    "line_material", "center_line_material", "center_material",
                    "line_thickness", "center_line_thickness", "center_marker_size", "center_thickness",
                    "min_segments", "max_segments", "target_segment_length",
                    "radius_grid_division"
                ));
                break;
                
            case "polygon":
                keys.addAll(Arrays.asList(
                    "edge_material", "vertex_material", "vertical_material",
                    "edge_thickness", "vertical_thickness",
                    "height_grid_division"
                ));
                break;
                
            case "polyhedron":
                keys.addAll(Arrays.asList(
                    "line_material", "vertex0_material", "vertex_material",
                    "line_thickness", "vertex_size", "vertex_thickness"
                ));
                break;
        }
        
        return keys;
    }
    
    /**
     * 取得常用材質建議
     */
    private List<String> getMaterialSuggestions() {
        return Arrays.stream(Material.values())
            .filter(Material::isSolid)
            .map(Material::name)
            .sorted()
            .collect(Collectors.toList());
    }
}
