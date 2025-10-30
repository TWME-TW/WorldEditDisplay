package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when poly point event is received
 */
public class CUIEventPoint2D extends CUIEventPoint3D {
    public CUIEventPoint2D(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.POINT2D;
    }

    @Override
    public String raise() {
        int id = this.getInt(0);
        int x = this.getInt(1);
        int z = this.getInt(2);
        
        // Get the appropriate region (multi-selection or single selection)
        Region region = playerData.getSelection(this.multi);
        
        if (region != null) {
            try {
                region.setPolygonPoint(id, x, z);
            } catch (UnsupportedOperationException e) {
                // Region type does not support polygon points
            }
        }

        return null;
    }
}
