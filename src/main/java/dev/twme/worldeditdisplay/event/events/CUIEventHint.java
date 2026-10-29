package dev.twme.worldeditdisplay.event.events;

import java.util.Locale;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * WorldEditDisplay rendering hints for the current multi-region, sent by other plugins as
 * {@code +wed|<hint>...}. WorldEditCUI ignores unknown event keys, so the hints are safe to send
 * to every client. Unknown hints are ignored too.
 *
 * <ul>
 *   <li>{@code outline}: draw ellipsoids and cylinders as outlines only, for previews that move
 *   often (such as a brush preview following the crosshair)</li>
 * </ul>
 */
public class CUIEventHint extends CUIEvent {
    public CUIEventHint(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.HINT;
    }

    @Override
    public void prepare() {
        if (!this.multi) {
            throw new IllegalStateException("Hint event is not valid for non-multi selections");
        }

        super.prepare();
    }

    @Override
    public String raise() {
        Region region = playerData.getCurrentMultiRegion();
        if (region == null) return null;

        for (int i = 0; i < params.size(); i++) {
            if ("outline".equals(getString(i).toLowerCase(Locale.ROOT))) {
                region.setOutlineOnly(true);
            }
        }
        return null;
    }
}
