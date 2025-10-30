package dev.twme.worldeditdisplay.event.events;

import dev.twme.worldeditdisplay.event.CUIEvent;
import dev.twme.worldeditdisplay.event.CUIEventArgs;
import dev.twme.worldeditdisplay.event.CUIEventType;
import dev.twme.worldeditdisplay.region.Region;

/**
 * Called when cylinder event is received
 */
public class CUIEventCylinder extends CUIEvent {
    public CUIEventCylinder(CUIEventArgs args) {
        super(args);
    }

    @Override
    public CUIEventType getEventType() {
        return CUIEventType.CYLINDER;
    }

    @Override
    public String raise() {
        int x = this.getInt(0);
        int y = this.getInt(1);
        int z = this.getInt(2);
        double radX = this.getDouble(3);
        double radZ = this.getDouble(4);
        
        // Get the appropriate region (multi-selection or single selection)
        Region region = playerData.getSelection(this.multi);
        
        if (region != null) {
            try {
                region.setCylinderCenter(x, y, z);
                region.setCylinderRadius(radX, radZ);
            } catch (UnsupportedOperationException e) {
                // Region type does not support cylinder properties
            }
        }

        return null;
    }
}
