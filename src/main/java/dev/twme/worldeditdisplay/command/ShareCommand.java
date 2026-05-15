package dev.twme.worldeditdisplay.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.share.ShareManager;
import dev.twme.worldeditdisplay.share.ShareManager.RequestResult;
import dev.twme.worldeditdisplay.util.MessageUtil;

/**
 * Handles the "/wedisplay share" sub-command tree.
 *
 * Sub-commands:
 *   share <player>             - invite a player to view your selection   (use.share.invite)
 *   share accept <player>      - accept a pending share invitation         (use.share.accept)
 *   share revoke <player>      - revoke a share you granted                (use.share.revoke)
 *   share unwatch <player>     - stop watching a player's selection        (use.share.unwatch)
 *   share list [page]          - show current sharing status               (use.share.list)
 */
public class ShareCommand {

    private final WorldEditDisplay plugin;

    public ShareCommand(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    /** Called by PlayerSettingsCommand when args[0] == "share". */
    public void handle(Player player, String[] args) {
        if (args.length < 2) {
            handleList(player, 0);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "accept"  -> handleAccept(player, args);
            case "revoke"  -> handleRevoke(player, args);
            case "unwatch" -> handleUnwatch(player, args);
            case "list"    -> {
                int page = 0;
                if (args.length >= 3) {
                    try { page = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
                handleList(player, page);
            }
            default -> handleInvite(player, args);
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (!player.hasPermission("worldeditdisplay.use.share.invite")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.player_not_found", targetName);
            return;
        }
        if (target.equals(player)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.cannot_share_self");
            return;
        }

        ShareManager sm = plugin.getShareManager();
        RequestResult result = sm.sendRequest(player.getUniqueId(), target.getUniqueId());

        switch (result) {
            case SELF -> MessageUtil.sendTranslated(player, "command.wedisplay.share.cannot_share_self");
            case ALREADY_ACTIVE -> MessageUtil.sendTranslated(player, "command.wedisplay.share.already_shared", target.getName());
            case ALREADY_PENDING -> {
                long remaining = sm.getPendingRemainingSeconds(player.getUniqueId(), target.getUniqueId());
                MessageUtil.sendTranslated(player, "command.wedisplay.share.already_pending", target.getName(), remaining);
            }
            case SENT -> {
                MessageUtil.sendTranslated(player, "command.wedisplay.share.invite_sent", target.getName());
                MessageUtil.sendTranslated(target, "command.wedisplay.share.invite_received",
                        player.getName(), player.getName());
            }
        }
    }

    private void handleAccept(Player player, String[] args) {
        if (!player.hasPermission("worldeditdisplay.use.share.accept")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        if (args.length < 3) {
            Set<UUID> pending = plugin.getShareManager().getPendingInvitesFor(player.getUniqueId());
            if (pending.isEmpty()) {
                MessageUtil.sendTranslated(player, "command.wedisplay.share.no_pending");
                return;
            }
            MessageUtil.sendTranslated(player, "command.wedisplay.share.pending_list_title");
            for (UUID sharerId : pending) {
                String name = getPlayerName(sharerId);
                MessageUtil.sendTranslated(player, "command.wedisplay.share.pending_entry", name, name);
            }
            return;
        }

        String sharerName = args[2];
        Player sharer = Bukkit.getPlayerExact(sharerName);
        UUID sharerId = sharer != null ? sharer.getUniqueId() : findOfflineUUID(sharerName);

        if (sharerId == null) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.player_not_found", sharerName);
            return;
        }

        ShareManager sm = plugin.getShareManager();
        if (!sm.hasPendingRequest(sharerId, player.getUniqueId())) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.no_invite_from", sharerName);
            return;
        }

        if (!sm.acceptShare(player.getUniqueId(), sharerId)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.invite_expired_target",
                    getPlayerName(sharerId));
            return;
        }

        MessageUtil.sendTranslated(player, "command.wedisplay.share.accept_success", getPlayerName(sharerId));

        if (sharer != null && sharer.isOnline()) {
            MessageUtil.sendTranslated(sharer, "command.wedisplay.share.viewer_accepted", player.getName());
        }

        if (sharer != null && sharer.isOnline()) {
            plugin.getRenderManager().clearSharedRenders(player.getUniqueId());
            plugin.getRenderManager().updateRender(player);
        }
    }

    private void handleRevoke(Player player, String[] args) {
        if (!player.hasPermission("worldeditdisplay.use.share.revoke")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        if (args.length < 3) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.revoke_usage");
            return;
        }

        String targetName = args[2];
        Player target = Bukkit.getPlayerExact(targetName);
        UUID targetId = target != null ? target.getUniqueId() : findOfflineUUID(targetName);

        if (targetId == null) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.player_not_found", targetName);
            return;
        }

        ShareManager sm = plugin.getShareManager();
        if (!sm.revokeShare(player.getUniqueId(), targetId)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.not_sharing_with", targetName);
            return;
        }

        MessageUtil.sendTranslated(player, "command.wedisplay.share.revoked", getPlayerName(targetId));
        plugin.getRenderManager().clearSharedRender(targetId, player.getUniqueId());

        if (target != null && target.isOnline()) {
            MessageUtil.sendTranslated(target, "command.wedisplay.share.share_revoked_target", player.getName());
        }
    }

    private void handleUnwatch(Player player, String[] args) {
        if (!player.hasPermission("worldeditdisplay.use.share.unwatch")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        if (args.length < 3) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.unwatch_usage");
            return;
        }

        String sharerName = args[2];
        Player sharer = Bukkit.getPlayerExact(sharerName);
        UUID sharerId = sharer != null ? sharer.getUniqueId() : findOfflineUUID(sharerName);

        if (sharerId == null) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.player_not_found", sharerName);
            return;
        }

        ShareManager sm = plugin.getShareManager();
        if (!sm.stopViewing(player.getUniqueId(), sharerId)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.not_viewing", sharerName);
            return;
        }

        MessageUtil.sendTranslated(player, "command.wedisplay.share.unwatch_success", getPlayerName(sharerId));
        plugin.getRenderManager().clearSharedRender(player.getUniqueId(), sharerId);
    }

    private void handleList(Player player, int page) {
        if (!player.hasPermission("worldeditdisplay.use.share.list")) {
            MessageUtil.sendTranslated(player, "general.no_permission");
            return;
        }

        ShareManager sm = plugin.getShareManager();
        UUID playerId = player.getUniqueId();

        List<String> lines = new ArrayList<>();

        Set<UUID> viewers = sm.getActiveViewers(playerId);
        for (UUID vid : viewers) {
            Player vp = Bukkit.getPlayer(vid);
            boolean online = vp != null && vp.isOnline();
            String name = getPlayerName(vid);
            lines.add(online
                    ? "<green>[Online]</green> <white>" + name + "</white>"
                    : "<gray>[Offline]</gray> <white>" + name + "</white>");
        }

        Set<UUID> sharers = sm.getActiveSharers(playerId);
        for (UUID sid : sharers) {
            Player sp = Bukkit.getPlayer(sid);
            boolean online = sp != null && sp.isOnline();
            String name = getPlayerName(sid);
            lines.add((online
                    ? "<aqua>[Watching]</aqua> "
                    : "<dark_aqua>[Watching-Offline]</dark_aqua> ")
                    + "<white>" + name + "</white>");
        }

        int pageSize = plugin.getConfig().getInt("share.list_page_size", 8);
        int totalPages = Math.max(1, (lines.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, totalPages - 1));

        MessageUtil.sendTranslated(player, "command.wedisplay.share.list_title");
        MessageUtil.sendTranslated(player, "command.wedisplay.share.list_page_header", page + 1, totalPages);

        int start = page * pageSize;
        int end = Math.min(start + pageSize, lines.size());

        if (lines.isEmpty()) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.list_empty");
        } else {
            for (int i = start; i < end; i++) {
                MessageUtil.sendMessage(player, lines.get(i));
            }
        }

        String prevBtn = page > 0
                ? "<click:run_command:'/wedisplay share list " + (page - 1) + "'><gray>◀ Prev</gray></click>"
                : "<dark_gray>◀ Prev</dark_gray>";
        String nextBtn = page < totalPages - 1
                ? "<click:run_command:'/wedisplay share list " + (page + 1) + "'><gray>Next ▶</gray></click>"
                : "<dark_gray>Next ▶</dark_gray>";
        MessageUtil.sendMessage(player, "<gray>--- <white>Page " + (page + 1) + " / " + totalPages + "</white> ---");
        MessageUtil.sendMessage(player, "[" + prevBtn + "]  [" + nextBtn + "]");
    }

    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 2) {
            List<String> options = new ArrayList<>(List.of("accept", "revoke", "unwatch", "list"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) options.add(p.getName());
            }
            return options.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase();
            return switch (sub) {
                case "accept" -> {
                    Set<UUID> pending = plugin.getShareManager().getPendingInvitesFor(player.getUniqueId());
                    yield pending.stream()
                            .map(this::getPlayerName)
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "revoke" -> {
                    Set<UUID> viewers = plugin.getShareManager().getActiveViewers(player.getUniqueId());
                    Set<UUID> pending = plugin.getShareManager().getPendingSentBy(player.getUniqueId());
                    Set<UUID> combined = new HashSet<>(viewers);
                    combined.addAll(pending);
                    yield combined.stream()
                            .map(this::getPlayerName)
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "unwatch" -> {
                    Set<UUID> sharers = plugin.getShareManager().getActiveSharers(player.getUniqueId());
                    yield sharers.stream()
                            .map(this::getPlayerName)
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    private String getPlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    @SuppressWarnings("deprecation")
    private UUID findOfflineUUID(String name) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) return offline.getUniqueId();
        return null;
    }
}
