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

import org.bukkit.configuration.file.YamlConfiguration;

import dev.twme.worldeditdisplay.WorldEditDisplay;

/**
 * Manages selection-sharing relationships between players.
 *
 * Flow:
 *  1. Sharer calls sendRequest(sharer, target)  → target receives pending invite
 *  2. Target calls acceptShare(viewer, sharer)  → relationship becomes active
 *  3. Either side can dissolve:
 *       sharer calls removeShare(sharer, target)  → removes permission + active view
 *       viewer calls stopViewing(viewer, sharer)  → removes only the view
 *
 * Persistence: saved to <dataFolder>/share_data.yml
 */
public class ShareManager {

    private final WorldEditDisplay plugin;

    /** sharer → set of viewers who have a pending (not yet accepted) invite */
    private final Map<UUID, Set<UUID>> pendingRequests = new ConcurrentHashMap<>();

    /** sharer → set of viewers who are actively watching */
    private final Map<UUID, Set<UUID>> activeShares = new ConcurrentHashMap<>();

    private final File dataFile;

    public ShareManager(WorldEditDisplay plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "share_data.yml");
        load();
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Player {@code sharer} invites {@code target} to view their selection.
     * If an active share already exists, does nothing.
     *
     * @return {@code true} if a new pending request was created
     */
    public boolean sendRequest(UUID sharer, UUID target) {
        if (sharer.equals(target)) return false;
        if (isActiveShare(sharer, target)) return false;

        pendingRequests.computeIfAbsent(sharer, k -> ConcurrentHashMap.newKeySet()).add(target);
        return true;
    }

    /**
     * Player {@code viewer} accepts the pending invite from {@code sharer}.
     *
     * @return {@code true} if the invite existed and was accepted
     */
    public boolean acceptShare(UUID viewer, UUID sharer) {
        Set<UUID> pending = pendingRequests.get(sharer);
        if (pending == null || !pending.contains(viewer)) return false;

        pending.remove(viewer);
        if (pending.isEmpty()) pendingRequests.remove(sharer);

        activeShares.computeIfAbsent(sharer, k -> ConcurrentHashMap.newKeySet()).add(viewer);
        save();
        return true;
    }

    /**
     * Sharer revokes their share for {@code target}, removing both pending and active states.
     *
     * @return {@code true} if any state was removed
     */
    public boolean removeShare(UUID sharer, UUID target) {
        boolean changed = false;
        Set<UUID> pending = pendingRequests.get(sharer);
        if (pending != null && pending.remove(target)) {
            if (pending.isEmpty()) pendingRequests.remove(sharer);
            changed = true;
        }
        Set<UUID> active = activeShares.get(sharer);
        if (active != null && active.remove(target)) {
            if (active.isEmpty()) activeShares.remove(sharer);
            changed = true;
        }
        if (changed) save();
        return changed;
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
        save();
        return true;
    }

    /** Returns {@code true} if sharer has an active (accepted) share with viewer. */
    public boolean isActiveShare(UUID sharer, UUID viewer) {
        Set<UUID> active = activeShares.get(sharer);
        return active != null && active.contains(viewer);
    }

    /** Returns {@code true} if sharer sent a pending invite to target. */
    public boolean hasPendingRequest(UUID sharer, UUID target) {
        Set<UUID> pending = pendingRequests.get(sharer);
        return pending != null && pending.contains(target);
    }

    /**
     * Returns the set of sharer UUIDs that {@code viewer} is actively watching.
     * (i.e. those who have active shares with viewer as a viewer)
     */
    public Set<UUID> getActiveSharers(UUID viewer) {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> entry : activeShares.entrySet()) {
            if (entry.getValue().contains(viewer)) {
                result.add(entry.getKey());
            }
        }
        return result;
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
     * Returns pending invite UUIDs (sharers who invited {@code target} but haven't been accepted).
     */
    public Set<UUID> getPendingInvitesFor(UUID target) {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> entry : pendingRequests.entrySet()) {
            if (entry.getValue().contains(target)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Returns the set of players that {@code sharer} has pending invites sent to.
     */
    public Set<UUID> getPendingSentBy(UUID sharer) {
        Set<UUID> pending = pendingRequests.get(sharer);
        if (pending == null) return Collections.emptySet();
        return Collections.unmodifiableSet(pending);
    }

    /**
     * Called when a player quits: clears their pending incoming invites from other players
     * but keeps active shares in memory (they reload on next login).
     * Active shares are persisted so they survive restarts.
     */
    public void onPlayerQuit(UUID uuid) {
        // Remove pending invites sent TO this player (they'll re-appear if sharer re-invites)
        for (Set<UUID> pending : pendingRequests.values()) {
            pending.remove(uuid);
        }
        pendingRequests.entrySet().removeIf(e -> e.getValue().isEmpty());
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
                    try { viewers.add(UUID.fromString(vStr)); } catch (IllegalArgumentException ignored) {}
                }
                if (!viewers.isEmpty()) activeShares.put(sharer, viewers);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load share_data.yml", e);
        }
    }
}
