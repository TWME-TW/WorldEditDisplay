package dev.twme.worldeditdisplay.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.player.PlayerData;
import dev.twme.worldeditdisplay.util.MessageUtil;

/**
 * Handles the "/wedisplay view" sub-command tree.
 *
 * Sub-commands:
 *   view                     – toggle viewall mode on/off          (use.view)
 *   view hide <player>       – exclude player from viewall          (use.view.hide)
 *   view hideall             – exclude all online players           (use.view.hide)
 *   view unhide <player>     – remove exclusion                     (use.view.hide)
 *   view label               – toggle name-label display            (use.view.label)
 *   view list [page]         – show viewall monitoring status       (use.view.list)
 */
public class ViewCommand {

    private final WorldEditDisplay plugin;

    public ViewCommand(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    /** Called by PlayerSettingsCommand when args[0] == "view". */
    public void handle(Player player, String[] args) {
        if (args.length < 2) {
            handleToggle(player);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "hide"   -> handleHide(player, args);
            case "hideall" -> handleHideAll(player);
            case "unhide" -> handleUnhide(player, args);
            case "label"  -> handleLabel(player);
            case "list"   -> {
                int page = 0;
                if (args.length >= 3) {
                    try { page = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
                handleList(player, page);
            }
            default -> MessageUtil.sendTranslated(player, "command.wedisplay.view.usage");
        }
    }

    // ─── /wedisplay view (toggle) ────────────────────────────────────────────

    private void handleToggle(Player player) {
        if (!player.hasPermission("worldeditdisplay.use.view")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        PlayerData data = PlayerData.getPlayerData(player);
        boolean newState = !data.isViewAllEnabled();
        data.setViewAllEnabled(newState);

        if (newState) {
            plugin.getViewAllPlayers().add(player.getUniqueId());
            MessageUtil.sendTranslated(player, "command.wedisplay.view.enabled");
        } else {
            plugin.getViewAllPlayers().remove(player.getUniqueId());
            MessageUtil.sendTranslated(player, "command.wedisplay.view.disabled");
            // Clear all viewall-sourced renders (re-render will only show active shares)
            plugin.getRenderManager().clearViewAllRenders(player.getUniqueId());
            plugin.getRenderManager().updateRender(player);
        }

        if (newState) {
            plugin.getRenderManager().updateRender(player);
        }
    }

    // ─── /wedisplay view hide <player> ──────────────────────────────────────

    private void handleHide(Player player, String[] args) {
        if (!player.hasPermission("worldeditdisplay.use.view.hide")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        if (args.length < 3) {
            MessageUtil.sendTranslated(player, "command.wedisplay.view.hide_usage");
            return;
        }

        String targetName = args[2];
        if (targetName.equalsIgnoreCase(player.getName())) {
            MessageUtil.sendTranslated(player, "command.wedisplay.view.hide_self");
            return;
        }

        UUID targetId = resolveUUID(targetName);
        if (targetId == null) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.player_not_found", targetName);
            return;
        }

        PlayerData data = PlayerData.getPlayerData(player);
        data.addViewAllHidden(targetId);
        MessageUtil.sendTranslated(player, "command.wedisplay.view.hide_success", targetName);

        // Remove any active viewall render for this target
        plugin.getRenderManager().clearViewAllRender(player.getUniqueId(), targetId);
    }

    // ─── /wedisplay view hideall ─────────────────────────────────────────────

    private void handleHideAll(Player player) {
        if (!player.hasPermission("worldeditdisplay.use.view.hide")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        PlayerData data = PlayerData.getPlayerData(player);
        data.hideAllOnline();
        MessageUtil.sendTranslated(player, "command.wedisplay.view.hideall_success");

        // Clear all viewall renders for this viewer
        plugin.getRenderManager().clearViewAllRenders(player.getUniqueId());
    }

    // ─── /wedisplay view unhide <player> ────────────────────────────────────

    private void handleUnhide(Player player, String[] args) {
        if (!player.hasPermission("worldeditdisplay.use.view.hide")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        if (args.length < 3) {
            MessageUtil.sendTranslated(player, "command.wedisplay.view.unhide_usage");
            return;
        }

        String targetName = args[2];
        UUID targetId = resolveUUID(targetName);
        if (targetId == null) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.player_not_found", targetName);
            return;
        }

        PlayerData data = PlayerData.getPlayerData(player);
        data.removeViewAllHidden(targetId);
        MessageUtil.sendTranslated(player, "command.wedisplay.view.unhide_success", targetName);

        // Re-render so the newly un-hidden player appears if viewall is active
        if (data.isViewAllEnabled()) {
            plugin.getRenderManager().updateRender(player);
        }
    }

    // ─── /wedisplay view label ───────────────────────────────────────────────

    private void handleLabel(Player player) {
        if (!player.hasPermission("worldeditdisplay.use.view.label")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        PlayerData data = PlayerData.getPlayerData(player);
        boolean newState = !data.isShowLabels();
        data.setShowLabels(newState);

        if (newState) {
            MessageUtil.sendTranslated(player, "command.wedisplay.view.label_enabled");
        } else {
            MessageUtil.sendTranslated(player, "command.wedisplay.view.label_disabled");
        }

        plugin.getRenderManager().updateRender(player);
    }

    // ─── /wedisplay view list [page] ─────────────────────────────────────────

    private void handleList(Player player, int page) {
        if (!player.hasPermission("worldeditdisplay.use.view.list")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        PlayerData data = PlayerData.getPlayerData(player);
        Set<UUID> hidden = data.getViewAllHidden();

        List<String> lines = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;
            UUID uid = p.getUniqueId();
            if (hidden.contains(uid)) {
                lines.add("<dark_gray>[Hidden]</dark_gray> <white>" + p.getName() + "</white>");
            } else {
                lines.add("<green>[Online]</green> <white>" + p.getName() + "</white>");
            }
        }

        int pageSize = plugin.getConfig().getInt("share.list_page_size", 8);
        int totalPages = Math.max(1, (lines.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, totalPages - 1));

        boolean viewAllOn = data.isViewAllEnabled();
        MessageUtil.sendTranslated(player, "command.wedisplay.view.list_title",
                viewAllOn ? "ON" : "OFF");
        MessageUtil.sendTranslated(player, "command.wedisplay.view.list_page_header", page + 1, totalPages);

        int start = page * pageSize;
        int end = Math.min(start + pageSize, lines.size());

        if (lines.isEmpty()) {
            MessageUtil.sendTranslated(player, "command.wedisplay.view.list_empty");
        } else {
            for (int i = start; i < end; i++) {
                MessageUtil.sendMessage(player, lines.get(i));
            }
        }

        String prevBtn = page > 0
                ? "<click:run_command:'/wedisplay view list " + (page - 1) + "'><gray>◀ Prev</gray></click>"
                : "<dark_gray>◀ Prev</dark_gray>";
        String nextBtn = page < totalPages - 1
                ? "<click:run_command:'/wedisplay view list " + (page + 1) + "'><gray>Next ▶</gray></click>"
                : "<dark_gray>Next ▶</dark_gray>";
        MessageUtil.sendMessage(player, "<gray>--- <white>Page " + (page + 1) + " / " + totalPages + "</white> ---");
        MessageUtil.sendMessage(player, "[" + prevBtn + "]  [" + nextBtn + "]");
    }

    // ─── Tab completion ──────────────────────────────────────────────────────

    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 2) {
            return filterPermittedOptions(player, List.of("hide", "hideall", "unhide", "label", "list"), args[1]);
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase();
            if (!canUseSubCommand(player, sub)) return List.of();
            if (sub.equals("hide") || sub.equals("unhide")) {
                List<String> result = new ArrayList<>();
                String prefix = args[2].toLowerCase();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.equals(player)) continue;
                    addIfStartsWith(result, onlinePlayer.getName(), prefix);
                }
                return result;
            }
        }
        return List.of();
    }

    private List<String> filterPermittedOptions(Player player, List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (!canUseSubCommand(player, option)) continue;
            addIfStartsWith(result, option, lowerPrefix);
        }
        return result;
    }

    private static void addIfStartsWith(List<String> result, String value, String lowerPrefix) {
        if (value.toLowerCase().startsWith(lowerPrefix)) {
            result.add(value);
        }
    }

    boolean canUseSubCommand(Player player, String subCommand) {
        return switch (subCommand) {
            case "hide", "hideall", "unhide" -> player.hasPermission("worldeditdisplay.use.view.hide");
            case "label" -> player.hasPermission("worldeditdisplay.use.view.label");
            case "list" -> player.hasPermission("worldeditdisplay.use.view.list");
            default -> false;
        };
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UUID resolveUUID(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) return offline.getUniqueId();
        return null;
    }
}
