package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when update event is received
 */
public class CUIEventUpdate extends CUIEvent {
    private boolean shouldTriggerRender;

    public CUIEventUpdate(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.UPDATE;
    }

    @Override
    public String raise() {
        Region region = playerData.getSelection(this.multi);
        if (region == null) {
            shouldTriggerRender = false;
            return null;
        }

        region.markDirty();
        shouldTriggerRender = true;
        return null;
    }

    @Override
    protected boolean shouldUpdateRender() {
        return shouldTriggerRender;
    }
}
