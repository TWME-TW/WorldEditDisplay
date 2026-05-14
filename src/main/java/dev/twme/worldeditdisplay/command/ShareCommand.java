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
import dev.twme.worldeditdisplay.util.MessageUtil;

/**
 * Handles the "/wedisplay share" sub-command tree.
 *
 * Sub-commands:
 *   share <player>           – invite a player to view your selection
 *   share accept <player>    – accept a pending share invitation
 *   share remove <player>    – stop sharing with a player
 *   share stop <player>      – stop viewing a player's selection
 *   share list               – show current sharing status
 */
public class ShareCommand {

    private final WorldEditDisplay plugin;

    public ShareCommand(WorldEditDisplay plugin) {
        this.plugin = plugin;
    }

    /** Called by PlayerSettingsCommand when args[0] == "share". */
    public void handle(Player player, String[] args) {
        if (args.length < 2) {
            handleList(player);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "accept"  -> handleAccept(player, args);
            case "remove"  -> handleRemove(player, args);
            case "stop"    -> handleStop(player, args);
            case "list"    -> handleList(player);
            default        -> handleInvite(player, args); // args[1] treated as player name
        }
    }

    // ─── /wedisplay share <player> ───────────────────────────────────────────

    private void handleInvite(Player player, String[] args) {
        // args[1] is the target player name
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
        if (sm.isActiveShare(player.getUniqueId(), target.getUniqueId())) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.already_shared", target.getName());
            return;
        }
        if (sm.hasPendingRequest(player.getUniqueId(), target.getUniqueId())) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.already_pending", target.getName());
            return;
        }

        sm.sendRequest(player.getUniqueId(), target.getUniqueId());
        MessageUtil.sendTranslated(player, "command.wedisplay.share.invite_sent", target.getName());

        // Notify the target with a clickable accept button
        MessageUtil.sendTranslated(target, "command.wedisplay.share.invite_received",
                player.getName(), player.getName());
    }

    // ─── /wedisplay share accept <player> ───────────────────────────────────

    private void handleAccept(Player player, String[] args) {
        if (args.length < 3) {
            // Show pending invites
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

        sm.acceptShare(player.getUniqueId(), sharerId);
        MessageUtil.sendTranslated(player, "command.wedisplay.share.accept_success", getPlayerName(sharerId));

        // Notify sharer
        if (sharer != null && sharer.isOnline()) {
            MessageUtil.sendTranslated(sharer, "command.wedisplay.share.viewer_accepted", player.getName());
        }

        // Immediately render the sharer's selection for this viewer
        if (sharer != null && sharer.isOnline()) {
            plugin.getRenderManager().clearSharedRenders(player.getUniqueId());
            // Trigger a re-render for the viewer so updateSharedSelections picks up the new sharer
            plugin.getRenderManager().updateRender(player);
        }
    }

    // ─── /wedisplay share remove <player> ───────────────────────────────────

    private void handleRemove(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.remove_usage");
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
        if (!sm.removeShare(player.getUniqueId(), targetId)) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.not_sharing_with", targetName);
            return;
        }

        MessageUtil.sendTranslated(player, "command.wedisplay.share.removed", getPlayerName(targetId));

        // Clear the now-invalid shared render from the former viewer
        plugin.getRenderManager().clearSharedRender(targetId, player.getUniqueId());

        // Notify the former viewer if online
        if (target != null && target.isOnline()) {
            MessageUtil.sendTranslated(target, "command.wedisplay.share.share_revoked", player.getName());
        }
    }

    // ─── /wedisplay share stop <player> ─────────────────────────────────────

    private void handleStop(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.stop_usage");
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

        MessageUtil.sendTranslated(player, "command.wedisplay.share.stopped_viewing", getPlayerName(sharerId));
        // Clear the shared render from this viewer
        plugin.getRenderManager().clearSharedRender(player.getUniqueId(), sharerId);

        // Notify the sharer if online
        if (sharer != null && sharer.isOnline()) {
            MessageUtil.sendTranslated(sharer, "command.wedisplay.share.viewer_stopped", player.getName());
        }
    }

    // ─── /wedisplay share list ───────────────────────────────────────────────

    private void handleList(Player player) {
        ShareManager sm = plugin.getShareManager();
        UUID playerId = player.getUniqueId();

        MessageUtil.sendTranslated(player, "command.wedisplay.share.list_title");

        // Pending invites SENT by this player
        Set<UUID> pendingSent = sm.getPendingSentBy(playerId);
        MessageUtil.sendTranslated(player, "command.wedisplay.share.list_pending_sent",
                pendingSent.isEmpty() ? "-" : pendingSent.stream().map(this::getPlayerName).collect(Collectors.joining(", ")));

        // Players actively viewing this player's selection
        Set<UUID> viewers = sm.getActiveViewers(playerId);
        MessageUtil.sendTranslated(player, "command.wedisplay.share.list_viewers",
                viewers.isEmpty() ? "-" : viewers.stream().map(this::getPlayerName).collect(Collectors.joining(", ")));

        // Players whose selections this player is viewing
        Set<UUID> sharers = sm.getActiveSharers(playerId);
        MessageUtil.sendTranslated(player, "command.wedisplay.share.list_viewing",
                sharers.isEmpty() ? "-" : sharers.stream().map(this::getPlayerName).collect(Collectors.joining(", ")));

        // Pending invites received (waiting for accept)
        Set<UUID> pendingReceived = sm.getPendingInvitesFor(playerId);
        if (!pendingReceived.isEmpty()) {
            MessageUtil.sendTranslated(player, "command.wedisplay.share.list_pending_received",
                    pendingReceived.stream().map(this::getPlayerName).collect(Collectors.joining(", ")));
        }
    }

    // ─── Tab completion ──────────────────────────────────────────────────────

    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 2) {
            List<String> options = new ArrayList<>(List.of("accept", "remove", "stop", "list"));
            // Also suggest online player names for the invite case
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
                case "remove" -> {
                    Set<UUID> viewers = plugin.getShareManager().getActiveViewers(player.getUniqueId());
                    Set<UUID> pending = plugin.getShareManager().getPendingSentBy(player.getUniqueId());
                    Set<UUID> combined = new HashSet<>(viewers);
                    combined.addAll(pending);
                    yield combined.stream()
                            .map(this::getPlayerName)
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "stop" -> {
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

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
        // getOfflinePlayer(name) returns a non-null object; check if they've played before
        if (offline.hasPlayedBefore() || offline.isOnline()) return offline.getUniqueId();
        return null;
    }
}
