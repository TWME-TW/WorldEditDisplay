package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when resize event is received
 */
public class CUIEventBounds extends CUIEvent {
    public CUIEventBounds(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.MINMAX;
    }

    @Override
    public String raise() {
        int min = this.getInt(0);
        int max = this.getInt(1);
        
        // Get the appropriate region (multi-selection or single selection)
        Region region = playerData.getSelection(this.multi);
        
        if (region != null) {
            try {
                region.setMinMax(min, max);
            } catch (UnsupportedOperationException e) {
                // Region type does not support min/max bounds
            }
        }

        return null;
    }
}
