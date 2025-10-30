package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when ellipsoid event is received
 */
public class CUIEventEllipsoid extends CUIEvent {
    public CUIEventEllipsoid(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.ELLIPSOID;
    }

    @Override
    public String raise() {
        int id = this.getInt(0);
        
        // Get the appropriate region (multi-selection or single selection)
        Region region = playerData.getSelection(this.multi);
        
        if (region != null) {
            try {
                if (id == 0) {
                    int x = this.getInt(1);
                    int y = this.getInt(2);
                    int z = this.getInt(3);
                    
                    region.setEllipsoidCenter(x, y, z);
                } else if (id == 1) {
                    double x = this.getDouble(1);
                    double y = this.getDouble(2);
                    double z = this.getDouble(3);
                    
                    region.setEllipsoidRadii(x, y, z);
                }
            } catch (UnsupportedOperationException e) {
            }
        } else {
        }

        return null;
    }
}
