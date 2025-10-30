package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when polygon event is received
 */
public class CUIEventPolygon extends CUIEvent {
    public CUIEventPolygon(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.POLYGON;
    }

    @Override
    public String raise() {
        final int[] vertexIds = new int[this.params.size()];
        for (int i = 0; i < this.params.size(); ++i) {
            vertexIds[i] = this.getInt(i);
        }
        
        // Get the appropriate region (multi-selection or single selection)
        Region region = playerData.getSelection(this.multi);
        
        if (region != null) {
            try {
                region.addPolygon(vertexIds);
            } catch (UnsupportedOperationException e) {
                // Region type does not support polygon faces
            }
        }

        return null;
    }
}
