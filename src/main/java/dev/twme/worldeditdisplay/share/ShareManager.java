package dev.twme.worldeditdisplay.share;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import dev.twme.worldeditdisplay.WorldEditDisplay;
import dev.twme.worldeditdisplay.util.MessageUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;

/**
 * Manages selection-sharing relationships between players.
 *
 * Flow:
 *  1. Sharer calls sendRequest(sharer, target)  → target receives pending invite
 *  2. Target calls acceptShare(viewer, sharer)  → relationship becomes active
 *  3. Either side can dissolve:
 *       sharer calls revokeShare(sharer, target)  → removes permission + active view
 *       viewer calls stopViewing(viewer, sharer)  → removes only the view
 *
 * Persistence: saved to <dataFolder>/share_data.yml (activeShares only)
 * pendingRequests are memory-only and cleared on restart.
 */
public class ShareManager {

    private final WorldEditDisplay plugin;
    private volatile int requestTimeoutSec;

    /**
     * sharer → (viewer → invite-timestamp-ms)
     * Memory-only; not persisted across restarts.
     */
    private final Map<UUID, Map<UUID, Long>> pendingRequests = new ConcurrentHashMap<>();

    /** sharer → set of viewers who are actively watching */
    private final Map<UUID, Set<UUID>> activeShares = new ConcurrentHashMap<>();
    /** viewer → set of sharers the viewer is actively watching (reverse index of activeShares) */
    private final Map<UUID, Set<UUID>> viewerToSharers = new ConcurrentHashMap<>();

    private final File dataFile;

    public ShareManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "share_data.yml");
        this.requestTimeoutSec = plugin.getConfig().getInt("share.request_timeout", 30);
        load();
    }

    /** Refresh cached config values. Call after a /reload. */
    public void reloadConfig() {
        this.requestTimeoutSec = plugin.getConfig().getInt("share.request_timeout", 30);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Player {@code sharer} invites {@code target} to view their selection.
     * Returns a {@link RequestResult} describing the outcome.
     */
    public RequestResult sendRequest(UUID sharer, UUID target) {
        if (sharer.equals(target)) return RequestResult.SELF;
        if (isActiveShare(sharer, target)) return RequestResult.ALREADY_ACTIVE;

        purgeExpiredRequests(sharer);
        if (hasPendingRequest(sharer, target)) return RequestResult.ALREADY_PENDING;

        pendingRequests.computeIfAbsent(sharer, k -> new ConcurrentHashMap<>())
                .put(target, System.currentTimeMillis());
        return RequestResult.SENT;
    }

    /**
     * Player {@code viewer} accepts the pending invite from {@code sharer}.
     *
     * @return {@code true} if the invite existed, was not expired, and was accepted
     */
    public boolean acceptShare(UUID viewer, UUID sharer) {
        Map<UUID, Long> pending = pendingRequests.get(sharer);
        if (pending == null || !pending.containsKey(viewer)) return false;

        long ts = pending.get(viewer);
        if (System.currentTimeMillis() - ts > requestTimeoutSec * 1000L) {
            pending.remove(viewer);
            if (pending.isEmpty()) pendingRequests.remove(sharer);
            return false;
        }

        pending.remove(viewer);
        if (pending.isEmpty()) pendingRequests.remove(sharer);

        activeShares.computeIfAbsent(sharer, k -> ConcurrentHashMap.newKeySet()).add(viewer);
        viewerToSharers.computeIfAbsent(viewer, k -> ConcurrentHashMap.newKeySet()).add(sharer);
        save();
        return true;
    }

    /**
     * Sharer revokes their share for {@code target}, removing both pending and active states.
     *
     * @return {@code true} if any state was removed
     */
    public boolean revokeShare(UUID sharer, UUID target) {
        boolean changed = false;
        Map<UUID, Long> pending = pendingRequests.get(sharer);
        if (pending != null && pending.remove(target) != null) {
            if (pending.isEmpty()) pendingRequests.remove(sharer);
            changed = true;
        }
        Set<UUID> active = activeShares.get(sharer);
        if (active != null && active.remove(target)) {
            if (active.isEmpty()) activeShares.remove(sharer);
            Set<UUID> targetSharers = viewerToSharers.get(target);
            if (targetSharers != null) {
                targetSharers.remove(sharer);
                if (targetSharers.isEmpty()) viewerToSharers.remove(target);
            }
            changed = true;
        }
        if (changed) save();
        return changed;
    }

    /** @deprecated Use {@link #revokeShare(UUID, UUID)} instead. */
    @Deprecated
    public boolean removeShare(UUID sharer, UUID target) {
        return revokeShare(sharer, target);
    }

    /**
     * Viewer stops watching {@code sharer}'s selection voluntarily.
     *
     * @return {@code true} if the active view was removed
     */
    public boolean stopViewing(UUID viewer, UUID sharer) {
        Set<UUID> active = activeShares.get(sharer);
        if (active == null || !active.contains(viewer)) return false;

        active.remove(viewer);
        if (active.isEmpty()) activeShares.remove(sharer);
        Set<UUID> viewerSharers = viewerToSharers.get(viewer);
        if (viewerSharers != null) {
            viewerSharers.remove(sharer);
            if (viewerSharers.isEmpty()) viewerToSharers.remove(viewer);
        }
        save();
        return true;
    }

    /** Returns {@code true} if sharer has an active (accepted) share with viewer. */
    public boolean isActiveShare(UUID sharer, UUID viewer) {
        Set<UUID> active = activeShares.get(sharer);
        return active != null && active.contains(viewer);
    }

    /** Returns {@code true} if sharer sent a non-expired pending invite to target. */
    public boolean hasPendingRequest(UUID sharer, UUID target) {
        Map<UUID, Long> pending = pendingRequests.get(sharer);
        if (pending == null || !pending.containsKey(target)) return false;
        long ts = pending.get(target);
        if (System.currentTimeMillis() - ts > requestTimeoutSec * 1000L) {
            pending.remove(target);
            if (pending.isEmpty()) pendingRequests.remove(sharer);
            return false;
        }
        return true;
    }

    /**
     * Returns how many seconds remain before the pending invite from {@code sharer} to
     * {@code target} expires, or -1 if no such invite exists / already expired.
     */
    public long getPendingRemainingSeconds(UUID sharer, UUID target) {
        Map<UUID, Long> pending = pendingRequests.get(sharer);
        if (pending == null || !pending.containsKey(target)) return -1;
        long ts = pending.get(target);
        long elapsed = (System.currentTimeMillis() - ts) / 1000;
        long remaining = requestTimeoutSec - elapsed;
        return remaining > 0 ? remaining : -1;
    }

    /**
     * Returns the set of sharer UUIDs that {@code viewer} is actively watching.
     */
    public Set<UUID> getActiveSharers(UUID viewer) {
        Set<UUID> sharers = viewerToSharers.get(viewer);
        if (sharers == null || sharers.isEmpty()) return new HashSet<>();
        return new HashSet<>(sharers);
    }

    /**
     * Returns the set of viewer UUIDs that are actively watching {@code sharer}.
     */
    public Set<UUID> getActiveViewers(UUID sharer) {
        Set<UUID> active = activeShares.get(sharer);
        if (active == null) return Collections.emptySet();
        return Collections.unmodifiableSet(active);
    }

    /**
     * Returns sharer UUIDs who sent a pending (non-expired) invite to {@code target}.
     */
    public Set<UUID> getPendingInvitesFor(UUID target) {
        Set<UUID> result = new HashSet<>();
        long timeoutMs = requestTimeoutSec * 1000L;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<UUID, Long>> entry : pendingRequests.entrySet()) {
            Long ts = entry.getValue().get(target);
            if (ts != null && now - ts <= timeoutMs) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Returns the set of players that {@code sharer} has non-expired pending invites sent to.
     */
    public Set<UUID> getPendingSentBy(UUID sharer) {
        purgeExpiredRequests(sharer);
        Map<UUID, Long> pending = pendingRequests.get(sharer);
        if (pending == null) return Collections.emptySet();
        return Collections.unmodifiableSet(pending.keySet());
    }

    /**
     * Called when a player quits: clears their pending invites, active shares,
     * and viewer-to-sharer mappings to prevent memory leaks.
     */
    public void onPlayerQuit(UUID uuid) {
        // Remove pending invites sent TO this player
        for (Map<UUID, Long> pending : pendingRequests.values()) {
            pending.remove(uuid);
        }
        pendingRequests.entrySet().removeIf(e -> e.getValue().isEmpty());

        // Remove this player's own outgoing pending invites
        pendingRequests.remove(uuid);

        // Remove this player as a sharer — revoke all active views on them
        Set<UUID> viewers = activeShares.remove(uuid);
        if (viewers != null) {
            for (UUID viewerId : viewers) {
                Set<UUID> sharers = viewerToSharers.get(viewerId);
                if (sharers != null) {
                    sharers.remove(uuid);
                    if (sharers.isEmpty()) viewerToSharers.remove(viewerId);
                }
            }
        }

        // Remove this player as a viewer — stop watching all sharers
        Set<UUID> sharers = viewerToSharers.remove(uuid);
        if (sharers != null) {
            for (UUID sharerId : sharers) {
                Set<UUID> activeViewers = activeShares.get(sharerId);
                if (activeViewers != null) {
                    activeViewers.remove(uuid);
                    if (activeViewers.isEmpty()) activeShares.remove(sharerId);
                }
            }
        }
    }

    /**
     * Purge expired requests for a specific sharer.
     * Expired entries are removed first (data-only), then notifications are dispatched via
     * per-entity schedulers so they always run on the correct Folia/Paper thread.
     */
    public void purgeExpiredRequests(UUID sharer) {
        Map<UUID, Long> pending = pendingRequests.get(sharer);
        if (pending == null) return;
        long timeoutMs = requestTimeoutSec * 1000L;
        long now = System.currentTimeMillis();

        List<UUID> expiredTargets = new ArrayList<>();
        pending.entrySet().removeIf(e -> {
            if (now - e.getValue() > timeoutMs) {
                expiredTargets.add(e.getKey());
                return true;
            }
            return false;
        });
        if (pending.isEmpty()) pendingRequests.remove(sharer);

        if (expiredTargets.isEmpty()) return;

        String sharerName = getPlayerName(sharer);
        for (UUID targetId : expiredTargets) {
            String targetName = getPlayerName(targetId);
            Player sharerPlayer = Bukkit.getPlayer(sharer);
            if (sharerPlayer != null) {
                FoliaScheduler.getEntityScheduler().run(sharerPlayer, plugin,
                        ignored -> MessageUtil.sendTranslated(sharerPlayer,
                                "command.wedisplay.share.invite_expired_sharer", targetName), null);
            }
            Player targetPlayer = Bukkit.getPlayer(targetId);
            if (targetPlayer != null) {
                FoliaScheduler.getEntityScheduler().run(targetPlayer, plugin,
                        ignored -> MessageUtil.sendTranslated(targetPlayer,
                                "command.wedisplay.share.invite_expired_target", sharerName), null);
            }
        }
    }

    /**
     * Purge all expired pending requests across all sharers.
     */
    public void purgeAllExpiredRequests() {
        for (UUID sharer : new HashSet<>(pendingRequests.keySet())) {
            purgeExpiredRequests(sharer);
        }
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> entry : activeShares.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            List<String> viewers = new ArrayList<>();
            for (UUID v : entry.getValue()) viewers.add(v.toString());
            yaml.set("shares." + entry.getKey().toString(), viewers);
        }
        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save share_data.yml", e);
        }
    }

    private void load() {
        activeShares.clear();
        viewerToSharers.clear();
        if (!dataFile.exists()) return;
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
            if (!yaml.isConfigurationSection("shares")) return;
            for (String sharerStr : yaml.getConfigurationSection("shares").getKeys(false)) {
                UUID sharer;
                try { sharer = UUID.fromString(sharerStr); } catch (IllegalArgumentException e) { continue; }
                List<String> viewerList = yaml.getStringList("shares." + sharerStr);
                Set<UUID> viewers = ConcurrentHashMap.newKeySet();
                for (String vStr : viewerList) {
                    try {
                        UUID viewerUUID = UUID.fromString(vStr);
                        viewers.add(viewerUUID);
                        viewerToSharers.computeIfAbsent(viewerUUID, k -> ConcurrentHashMap.newKeySet()).add(sharer);
                    } catch (IllegalArgumentException ignored) {}
                }
                if (!viewers.isEmpty()) activeShares.put(sharer, viewers);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load share_data.yml", e);
        }
    }

    private String getPlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    // ─── Result enum ─────────────────────────────────────────────────────────

    public enum RequestResult {
        SENT, SELF, ALREADY_ACTIVE, ALREADY_PENDING
    }
}
