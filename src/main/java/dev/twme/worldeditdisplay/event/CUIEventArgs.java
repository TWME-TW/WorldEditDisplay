package dev.twme.worldeditdisplay.event;

import java.util.List;

import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * CUI communication event arguments
 * Called when a CUI event is sent from the server.
 */
public final class CUIEventArgs {
    private final PlayerData playerData;
    private final boolean multi;
    private final String type;
    private final List<String> params;

    public CUIEventArgs(PlayerData playerData, boolean multi, String type, List<String> params) {
        this.playerData = playerData;
        this.multi = multi;
        this.type = type;

        if (params.size() == 1 && params.get(0).isEmpty()) {
            params = List.of();
        }

        this.params = params;
    }

    public PlayerData getPlayerData() {
        return this.playerData;
    }

    public List<String> getParams() {
        return this.params;
    }

    public String getType() {
        return this.type;
    }

    public boolean isMulti() {
        return this.multi;
    }
}
