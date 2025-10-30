package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when point event is received
 */
public class CUIEventPoint3D extends CUIEvent {
    public CUIEventPoint3D(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.POINT;
    }

    @Override
    public String raise() {
        // Get the point data
        int id = this.getInt(0);
        double x = this.getDouble(1);
        double y = this.getDouble(2);
        double z = this.getDouble(3);
        
        // The 5th parameter (index 4) represents the volume/area of the selection
        // This is sent by WorldEdit server but is optional
        Long volume = null;
        if (this.params.size() >= 5) {
            try {
                volume = Long.parseLong(this.params.get(4));
            } catch (NumberFormatException e) {
                // Ignore invalid volume parameter
            }
        }
        
        // Get the appropriate region (multi-selection or single selection)
        Region region = playerData.getSelection(this.multi);
        
        if (region == null) {
            return null;
        }
        
        // Set the point on the region
        try {
            region.setCuboidPoint(id, x, y, z);
        } catch (UnsupportedOperationException e) {
            // Region type does not support cuboid points
        }

        return null;
    }
    
    @Override
    protected boolean shouldUpdateRender() {
        // Trigger render after setting a point
        return true;
    }
}
