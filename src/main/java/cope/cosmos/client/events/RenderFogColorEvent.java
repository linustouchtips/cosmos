package cope.cosmos.client.events;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.awt.*;

/**
 * Called when the color of the fog is calculated
 * @author linustouchtips
 * @since 12/28/2021
 */
@Cancelable
public class RenderFogColorEvent extends Event {

    // fog color
    private final Color color;

    public RenderFogColorEvent(Color color) {
        this.color = color;
    }

    /**
     * Gets the fog color
     * @return The fog color
     */
    public Color getColor() {
        return color;
    }
}
