package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;

/**
 * Called when update event is received
 */
public class CUIEventUpdate extends CUIEvent {
    public CUIEventUpdate(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.UPDATE;
    }

    @Override
    public String raise() {
        // TODO: Implement update logic

        return null;
    }
}
