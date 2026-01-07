package dev.twme.worldeditdisplay.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.util.MessageUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * command for players to manage their personal settings
 */
public class PlayerSettingsCommand implements TabExecutor {

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
            case "set" -> handleSet(player, args);
            case "reset" -> handleReset(player, args);
            case "show" -> handleShow(player, args);
            case "reloadplayer" -> handleReload(player);
            case "lang", "language" -> handleLanguage(player, args);
            case "toggle" -> handleToggle(player);
            default -> sendHelp(player);
        }

        return true;
    }

    // command handlers
    private boolean handleSet(Player player, String[] args) {
        if (args.length < 4) {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.usage");
            MessageUtil.sendTranslated(player, "command.wedisplay.set.example");
            return true;
        }

        String renderer = args[1].toLowerCase();
        String setting = args[2].toLowerCase();
        String value = args[3];

        if (!isValidRenderer(renderer)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.invalid_renderer");
            MessageUtil.sendTranslated(player, "command.wedisplay.set.available_renderers");
            return true;
        }

        String path = "renderer." + renderer + "." + setting;
        PlayerRenderSettings settings = plugin.getPlayerSettingsManager().getSettings(player);
        Object parsedValue = parseValue(setting, value);

        if (parsedValue == null) {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.invalid_value");
            return true;
        }

        if (settings.set(path, parsedValue)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.success", renderer, setting, value);
            MessageUtil.sendTranslated(player, "command.wedisplay.set.success_hint");
            plugin.getRenderManager().refreshPlayerRenderer(player);
        } else {
            MessageUtil.sendTranslated(player, "command.wedisplay.set.failed");
            MessageUtil.sendTranslated(player, "command.wedisplay.set.failed_hint");
        }

        return true;
    }

    private boolean handleReset(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.usage");
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.example1");
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.example2");
            return true;
        }

        String renderer = args[1].toLowerCase();

        if (!isValidRenderer(renderer)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.invalid_renderer");
            return true;
        }

        PlayerRenderSettings settings = plugin.getPlayerSettingsManager().getSettings(player);

        if (args.length >= 3) {
            // reset one setting
            String setting = args[2].toLowerCase();
            settings.reset("renderer." + renderer + "." + setting);
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.success_setting", renderer, setting);
        } else {
            // reset whole renderer
            settings.reset("renderer." + renderer);
            MessageUtil.sendTranslated(player, "command.wedisplay.reset.success_all", renderer);
        }

        plugin.getRenderManager().refreshPlayerRenderer(player);
        return true;
    }

    private boolean handleShow(Player player, String[] args) {
        PlayerRenderSettings settings = plugin.getPlayerSettingsManager().getSettings(player);

        if (args.length >= 2) {
            String renderer = args[1].toLowerCase();
            if (!isValidRenderer(renderer)) {
                MessageUtil.sendTranslated(player, "command.wedisplay.show.invalid_renderer");
                return true;
            }
            showRendererSettings(player, renderer, settings);
        } else {
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

    private boolean handleReload(Player player) {
        plugin.getPlayerSettingsManager().reloadSettings(player);
        MessageUtil.sendTranslated(player, "command.wedisplay.reload.success");
        plugin.getRenderManager().refreshPlayerRenderer(player);
        return true;
    }

    private boolean handleLanguage(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("language.allow_player_change", true)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.disabled");
            return true;
        }

        if (args.length < 2) {
            String lang = plugin.getLanguageManager().getPlayerLanguage(player);
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.current", lang);
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.usage");
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.example");
            return true;
        }

        String targetLang = args[1].toLowerCase();
        if (!plugin.getLanguageManager().isLanguageAvailable(targetLang)) {
            String available = String.join(", ", plugin.getLanguageManager().getAvailableLanguages());
            MessageUtil.sendTranslated(player, "command.wedisplay.lang.invalid", available);
            return true;
        }

        plugin.getLanguageManager().setPlayerLanguage(player.getUniqueId(), targetLang);
        MessageUtil.sendTranslated(player, "command.wedisplay.lang.success", targetLang);
        return true;
    }

    private boolean handleToggle(Player player) {
        PlayerData data = PlayerData.getPlayerData(player);
        boolean newState = !data.isRenderingEnabled();
        data.setRenderingEnabled(newState);

        if (newState) {
            MessageUtil.sendTranslated(player, "command.wedisplay.toggle.enabled");
        } else {
            MessageUtil.sendTranslated(player, "command.wedisplay.toggle.disabled");
            plugin.getRenderManager().clearRender(player.getUniqueId());
        }

        return true;
    }

    // helpers
    private void showRendererSettings(Player player, String renderer, PlayerRenderSettings settings) {
        MessageUtil.sendTranslated(player, "command.wedisplay.show.renderer_title", renderer.toUpperCase());

        switch (renderer) {
            case "cuboid" -> {
                MessageUtil.sendTranslated(player, "settings.edge_material", settings.getCuboidEdgeMaterial());
                MessageUtil.sendTranslated(player, "settings.point1_material", settings.getCuboidPoint1Material());
                MessageUtil.sendTranslated(player, "settings.point2_material", settings.getCuboidPoint2Material());
                MessageUtil.sendTranslated(player, "settings.grid_material", settings.getCuboidGridMaterial());
                MessageUtil.sendTranslated(player, "settings.edge_thickness", settings.getCuboidEdgeThickness());
                MessageUtil.sendTranslated(player, "settings.grid_thickness", settings.getCuboidGridThickness());
                MessageUtil.sendTranslated(player, "settings.vertex_marker_size", settings.getCuboidVertexMarkerSize());
                MessageUtil.sendTranslated(player, "settings.height_grid_division", settings.getCuboidHeightGridDivision());
            }
            case "cylinder" -> {
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
            }
            case "ellipsoid" -> {
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
            }
            case "polygon" -> {
                MessageUtil.sendTranslated(player, "settings.edge_material", settings.getPolygonEdgeMaterial());
                MessageUtil.sendTranslated(player, "settings.vertex_material", settings.getPolygonVertexMaterial());
                MessageUtil.sendTranslated(player, "settings.vertical_material", settings.getPolygonVerticalMaterial());
                MessageUtil.sendTranslated(player, "settings.edge_thickness", settings.getPolygonEdgeThickness());
                MessageUtil.sendTranslated(player, "settings.vertical_thickness", settings.getPolygonVerticalThickness());
                MessageUtil.sendTranslated(player, "settings.height_grid_division", settings.getPolygonHeightGridDivision());
            }
            case "polyhedron" -> {
                MessageUtil.sendTranslated(player, "settings.line_material", settings.getPolyhedronLineMaterial());
                MessageUtil.sendTranslated(player, "settings.vertex0_material", settings.getPolyhedronVertex0Material());
                MessageUtil.sendTranslated(player, "settings.vertex_material", settings.getPolyhedronVertexMaterial());
                MessageUtil.sendTranslated(player, "settings.line_thickness", settings.getPolyhedronLineThickness());
                MessageUtil.sendTranslated(player, "settings.vertex_size", settings.getPolyhedronVertexSize());
                MessageUtil.sendTranslated(player, "settings.vertex_thickness", settings.getPolyhedronVertexThickness());
            }
        }

        MessageUtil.sendTranslated(player, "command.wedisplay.show.modify_hint");
    }

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

    private boolean isValidRenderer(String renderer) {
        return renderer.equals("cuboid") || renderer.equals("cylinder") ||
                renderer.equals("ellipsoid") || renderer.equals("polygon") ||
                renderer.equals("polyhedron");
    }

    private Object parseValue(String setting, String value) {
        if (setting.contains("material")) {
            try { return Material.valueOf(value.toUpperCase()); }
            catch (IllegalArgumentException e) { return null; }
        } else if (setting.contains("thickness") || setting.contains("size") ||
                setting.contains("length") || setting.contains("factor")) {
            try { return Double.parseDouble(value); }
            catch (NumberFormatException e) { return null; }
        } else {
            try { return Integer.parseInt(value); }
            catch (NumberFormatException e) { return null; }
        }
    }

    // Tab Completion
    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "set", "reset", "show", "reloadplayer", "lang", "language", "toggle");

    // second argument options
    private static final List<String> RENDERERS = Arrays.asList(
            "cuboid", "cylinder", "ellipsoid", "polygon", "polyhedron");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return null;

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // first arg: subcommand
            completions = SUB_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("set") || subCommand.equals("reset") || subCommand.equals("show")) {
                // second arg: renderer type
                completions = RENDERERS.stream()
                        .filter(r -> r.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (subCommand.equals("lang") || subCommand.equals("language")) {
                // second arg: language code
                completions = plugin.getLanguageManager().getAvailableLanguages().stream()
                        .filter(lang -> lang.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String renderer = args[1].toLowerCase();

            if (subCommand.equals("set") || subCommand.equals("reset")) {
                // third arg: setting key
                completions = getSettingKeys(renderer).stream()
                        .filter(key -> key.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }

        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String setting = args[2].toLowerCase();

            if (subCommand.equals("set")) {
                if (setting.contains("material")) {
                    // fourth arg: material value
                    completions = getMaterialSuggestions().stream()
                            .filter(mat -> mat.startsWith(args[3].toUpperCase()))
                            .collect(Collectors.toList());
                } else {
                    // show examples
                    completions = Arrays.asList("<value>", "1", "0.05", "0.04", "0.03");
                }
            }
        }

        return completions;
    }

    // helper methods
    private List<String> getSettingKeys(String renderer) {
        return switch (renderer) {
            case "cuboid" -> Arrays.asList(
                    "edge_material", "point1_material", "point2_material", "grid_material",
                    "edge_thickness", "grid_thickness", "vertex_marker_size", "height_grid_division");
            case "cylinder" -> Arrays.asList(
                    "circle_material", "grid_material", "center_material", "center_line_material",
                    "circle_thickness", "grid_thickness", "center_line_thickness", "center_thickness",
                    "min_circle_segments", "max_circle_segments", "target_segment_length",
                    "height_grid_division", "radius_grid_division");
            case "ellipsoid" -> Arrays.asList(
                    "line_material", "center_line_material", "center_material",
                    "line_thickness", "center_line_thickness", "center_marker_size", "center_thickness",
                    "min_segments", "max_segments", "target_segment_length",
                    "radius_grid_division");
            case "polygon" -> Arrays.asList(
                    "edge_material", "vertex_material", "vertical_material",
                    "edge_thickness", "vertical_thickness", "height_grid_division");
            case "polyhedron" -> Arrays.asList(
                    "line_material", "vertex0_material", "vertex_material",
                    "line_thickness", "vertex_size", "vertex_thickness");
            default -> new ArrayList<>();
        };
    }

    private List<String> getMaterialSuggestions() {
        return Arrays.stream(Material.values())
                .filter(Material::isSolid)
                .map(Material::name)
                .sorted()
                .toList();
    }
}
