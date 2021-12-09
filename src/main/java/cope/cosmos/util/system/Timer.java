package cope.cosmos.util.system;

import cope.cosmos.client.Cosmos;
import cope.cosmos.event.annotation.Subscription;
import cope.cosmos.util.Wrapper;
import net.minecraftforge.event.entity.living.LivingEvent;

@SuppressWarnings("unused")
public class Timer implements Wrapper {

    // time
    private long milliseconds;
    private long ticks;

    public Timer() {
        // initialize the values
        milliseconds = -1;
        ticks = -1;

        // register to event bus
        Cosmos.EVENT_BUS.subscribe(this);
    }

    @Subscription
    public void onUpdate(LivingEvent.LivingUpdateEvent event) {
        // update ticks
        if (nullCheck()) {
            ticks++;
        }

        else {
            // reset time
            milliseconds = -1;
            ticks = -1;
        }
    }

    /**
     * Checks if the timer has passed a specified time
     * @param time The specified time
     * @param format The timing format to use
     * @return Whether the timer has passed the specified time
     */
    public boolean passedTime(long time, Format format) {
        switch (format) {
            case SYSTEM:
            default:
                return (System.currentTimeMillis() - milliseconds) >= time;
            case TICKS:
                return ticks >= time;
        }
    }

    /**
     * Gets the elapsed time in milliseconds
     * @return The elapsed time in milliseconds
     */
    public long getMilliseconds() {
        if (milliseconds <= 0) {
            return 0;
        }

        return System.currentTimeMillis() - milliseconds;
    }

    /**
     * Sets the timer time
     * @param in The new time
     * @param format The timing format to use
     */
    public void setTime(long in, Format format) {
        switch (format) {
            case SYSTEM:
            default:
                milliseconds = System.currentTimeMillis() - in;
                break;
            case TICKS:
                ticks = in;
                break;
        }
    }

    /**
     * Resets the timer
     */
    public void resetTime() {
        // reset our values
        milliseconds = System.currentTimeMillis();
        ticks = 0;
    }

    public enum Format {
        /**
         * Time in milliseconds
         */
        SYSTEM,

        /**
         * Time in ticks
         */
        TICKS
    }
}
