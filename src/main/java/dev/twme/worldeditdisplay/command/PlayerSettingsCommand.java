package dev.twme.worldeditdisplay.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.config.PlayerRenderSettings;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.util.ColorUtil;
import dev.twme.worldeditdisplay.util.MessageUtil;

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
            case "debug" -> handleDebug(player);
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
            plugin.getRenderManager().refreshPlayerRenderer(player);
            MessageUtil.sendMessage(player, "");
            showRendererSettings(player, renderer, settings, setting);
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
            showRendererSettings(player, renderer, settings, null);
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

    private boolean handleDebug(Player player) {
        PlayerData data = PlayerData.getPlayerData(player);
        boolean newState = !data.isDebugEnabled();
        data.setDebugEnabled(newState);

        if (newState) {
            MessageUtil.sendTranslated(player, "command.wedisplay.debug.enabled");
        } else {
            MessageUtil.sendTranslated(player, "command.wedisplay.debug.disabled");
        }

        return true;
    }

    // helpers
    private void showRendererSettings(Player player, String renderer, PlayerRenderSettings settings, @Nullable String highlightedSetting) {
        MessageUtil.sendTranslated(player, "command.wedisplay.show.renderer_title", renderer.toUpperCase());

        switch (renderer) {
            case "cuboid" -> {
                sendRendererSetting(player, renderer, "see_through", settings.isCuboidSeeThrough(), highlightedSetting);
                sendRendererSetting(player, renderer, "edge_color", ColorUtil.toHexString(settings.getCuboidEdgeColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "point1_color", ColorUtil.toHexString(settings.getCuboidPoint1Color()), highlightedSetting);
                sendRendererSetting(player, renderer, "point2_color", ColorUtil.toHexString(settings.getCuboidPoint2Color()), highlightedSetting);
                sendRendererSetting(player, renderer, "grid_color", ColorUtil.toHexString(settings.getCuboidGridColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "edge_thickness", settings.getCuboidEdgeThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "grid_thickness", settings.getCuboidGridThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "vertex_marker_size", settings.getCuboidVertexMarkerSize(), highlightedSetting);
                sendRendererSetting(player, renderer, "height_grid_division", settings.getCuboidHeightGridDivision(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_enabled", settings.isCuboidFillEnabled(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_color", ColorUtil.toHexString(settings.getCuboidFillColor()), highlightedSetting);
            }
            case "cylinder" -> {
                sendRendererSetting(player, renderer, "see_through", settings.isCylinderSeeThrough(), highlightedSetting);
                sendRendererSetting(player, renderer, "circle_color", ColorUtil.toHexString(settings.getCylinderCircleColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "grid_color", ColorUtil.toHexString(settings.getCylinderGridColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "center_color", ColorUtil.toHexString(settings.getCylinderCenterColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "center_line_color", ColorUtil.toHexString(settings.getCylinderCenterLineColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "circle_thickness", settings.getCylinderCircleThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "grid_thickness", settings.getCylinderGridThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "center_line_thickness", settings.getCylinderCenterLineThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "center_thickness", settings.getCylinderCenterThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "min_circle_segments", settings.getCylinderMinCircleSegments(), highlightedSetting);
                sendRendererSetting(player, renderer, "max_circle_segments", settings.getCylinderMaxCircleSegments(), highlightedSetting);
                sendRendererSetting(player, renderer, "target_segment_length", settings.getCylinderTargetSegmentLength(), highlightedSetting);
                sendRendererSetting(player, renderer, "height_grid_division", settings.getCylinderHeightGridDivision(), highlightedSetting);
                sendRendererSetting(player, renderer, "radius_grid_division", settings.getCylinderRadiusGridDivision(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_enabled", settings.isCylinderFillEnabled(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_color", ColorUtil.toHexString(settings.getCylinderFillColor()), highlightedSetting);
            }
            case "ellipsoid" -> {
                sendRendererSetting(player, renderer, "see_through", settings.isEllipsoidSeeThrough(), highlightedSetting);
                sendRendererSetting(player, renderer, "line_color", ColorUtil.toHexString(settings.getEllipsoidLineColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "center_line_color", ColorUtil.toHexString(settings.getEllipsoidCenterLineColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "center_color", ColorUtil.toHexString(settings.getEllipsoidCenterColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "line_thickness", settings.getEllipsoidLineThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "center_line_thickness", settings.getEllipsoidCenterLineThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "center_marker_size", settings.getEllipsoidCenterMarkerSize(), highlightedSetting);
                sendRendererSetting(player, renderer, "center_thickness", settings.getEllipsoidCenterThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "min_segments", settings.getEllipsoidMinSegments(), highlightedSetting);
                sendRendererSetting(player, renderer, "max_segments", settings.getEllipsoidMaxSegments(), highlightedSetting);
                sendRendererSetting(player, renderer, "target_segment_length", settings.getEllipsoidTargetSegmentLength(), highlightedSetting);
                sendRendererSetting(player, renderer, "radius_grid_division", settings.getEllipsoidRadiusGridDivision(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_enabled", settings.isEllipsoidFillEnabled(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_color", ColorUtil.toHexString(settings.getEllipsoidFillColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_generators", settings.getEllipsoidFillGenerators(), highlightedSetting);
            }
            case "polygon" -> {
                sendRendererSetting(player, renderer, "see_through", settings.isPolygonSeeThrough(), highlightedSetting);
                sendRendererSetting(player, renderer, "edge_color", ColorUtil.toHexString(settings.getPolygonEdgeColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "vertex_color", ColorUtil.toHexString(settings.getPolygonVertexColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "vertical_color", ColorUtil.toHexString(settings.getPolygonVerticalColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "edge_thickness", settings.getPolygonEdgeThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "vertical_thickness", settings.getPolygonVerticalThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "height_grid_division", settings.getPolygonHeightGridDivision(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_enabled", settings.isPolygonFillEnabled(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_color", ColorUtil.toHexString(settings.getPolygonFillColor()), highlightedSetting);
            }
            case "polyhedron" -> {
                sendRendererSetting(player, renderer, "see_through", settings.isPolyhedronSeeThrough(), highlightedSetting);
                sendRendererSetting(player, renderer, "line_color", ColorUtil.toHexString(settings.getPolyhedronLineColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "vertex0_color", ColorUtil.toHexString(settings.getPolyhedronVertex0Color()), highlightedSetting);
                sendRendererSetting(player, renderer, "vertex_color", ColorUtil.toHexString(settings.getPolyhedronVertexColor()), highlightedSetting);
                sendRendererSetting(player, renderer, "line_thickness", settings.getPolyhedronLineThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "vertex_size", settings.getPolyhedronVertexSize(), highlightedSetting);
                sendRendererSetting(player, renderer, "vertex_thickness", settings.getPolyhedronVertexThickness(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_enabled", settings.isPolyhedronFillEnabled(), highlightedSetting);
                sendRendererSetting(player, renderer, "fill_color", ColorUtil.toHexString(settings.getPolyhedronFillColor()), highlightedSetting);
            }
        }

        MessageUtil.sendTranslated(player, "command.wedisplay.show.modify_hint");
    }

    private void sendRendererSetting(Player player, String renderer, String setting, Object value, @Nullable String highlightedSetting) {
        if (highlightedSetting != null && setting.equalsIgnoreCase(highlightedSetting)) {
            String line = MessageUtil.getTranslated(player, "settings." + renderer + "." + setting, value);
            MessageUtil.sendMessage(player, "<yellow>> </yellow>" + line);
            return;
        }

        MessageUtil.sendTranslated(player, "settings." + renderer + "." + setting, value);
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
        MessageUtil.sendTranslated(player, "command.wedisplay.help.debug");
        MessageUtil.sendTranslated(player, "command.wedisplay.help.debug_desc");
    }

    private boolean isValidRenderer(String renderer) {
        return renderer.equals("cuboid") || renderer.equals("cylinder") ||
                renderer.equals("ellipsoid") || renderer.equals("polygon") ||
                renderer.equals("polyhedron");
    }

    private Object parseValue(String setting, String value) {
        if (setting.contains("color")) {
            if (!value.startsWith("#")) {
                value = "#" + value;
            }
            if (ColorUtil.isValidHexColor(value)) {
                return value;
            }
            return null;
        } else if (setting.contains("enabled") || setting.contains("see_through")) {
            return Boolean.parseBoolean(value);
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
            "set", "reset", "show", "reloadplayer", "lang", "language", "toggle", "debug");

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
                if (setting.contains("color")) {
                    completions = Arrays.asList("#RRGGBBAA", "#FF0000FF", "#00FF00FF", "#0000FFFF", "#FFFFFF80");
                } else if (setting.contains("enabled") || setting.contains("see_through")) {
                    completions = Arrays.asList("true", "false");
                } else {
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
                    "edge_color", "point1_color", "point2_color", "grid_color",
                    "edge_thickness", "grid_thickness", "vertex_marker_size", "height_grid_division",
                    "see_through", "fill_enabled", "fill_color");
            case "cylinder" -> Arrays.asList(
                    "circle_color", "grid_color", "center_color", "center_line_color",
                    "circle_thickness", "grid_thickness", "center_line_thickness", "center_thickness",
                    "min_circle_segments", "max_circle_segments", "target_segment_length",
                    "height_grid_division", "radius_grid_division",
                    "see_through", "fill_enabled", "fill_color");
            case "ellipsoid" -> Arrays.asList(
                    "line_color", "center_line_color", "center_color",
                    "line_thickness", "center_line_thickness", "center_marker_size", "center_thickness",
                    "min_segments", "max_segments", "target_segment_length",
                    "radius_grid_division",
                    "see_through", "fill_enabled", "fill_color", "fill_generators");
            case "polygon" -> Arrays.asList(
                    "edge_color", "vertex_color", "vertical_color",
                    "edge_thickness", "vertical_thickness", "height_grid_division",
                    "see_through", "fill_enabled", "fill_color");
            case "polyhedron" -> Arrays.asList(
                    "line_color", "vertex0_color", "vertex_color",
                    "line_thickness", "vertex_size", "vertex_thickness",
                    "see_through", "fill_enabled", "fill_color");
            default -> new ArrayList<>();
        };
    }
}
