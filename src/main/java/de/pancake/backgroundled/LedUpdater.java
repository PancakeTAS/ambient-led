package de.pancake.backgroundled;

import java.util.TimerTask;

import static de.pancake.backgroundled.Main.*;

/**
 * Led updater class
 * @author Pancake
 */
public class LedUpdater extends TimerTask {

    // Led strip instance
    private Led led;

    // Interpolated colors
    private final int[][] final_colors = new int[180][3];

    /**
     * Initializes the LedUpdater
     */
    public LedUpdater() {
        this.reopen();
    }

    /**
     * Updates the colors of the led strip
     */
    @Override
    public void run() {
        try {
            if (led != null && paused) {
                for (int i = 0; i < 180; i++)
                    led.write(i, 0, 0, 0);

                led.flush();
                led.close();
                led = null;
            }

            if (paused)
                return;

            // update the colors
            for (int i = 0; i < 180; i++) {
                // interpolate the final color
                final_colors[i][0] = (int) (final_colors[i][0] + (COLORS[i][0] - final_colors[i][0]) * .5);
                final_colors[i][1] = (int) (final_colors[i][1] + (COLORS[i][1] - final_colors[i][1]) * .5);
                final_colors[i][2] = (int) (final_colors[i][2] + (COLORS[i][2] - final_colors[i][2]) * .5);

                // write the color to the led
                this.led.write(i, final_colors[i][0], final_colors[i][1], final_colors[i][2]);
            }

            // flush the led
            this.led.flush();
        } catch (Exception e) {
            // something went wrong, try to reopen the connection
            LOGGER.severe(e.getMessage());
            this.reopen();
        }

    }

    /**
     * Reopens the connection to the Arduino
     */
    private void reopen() {
        try {
            Thread.sleep(500);
            LOGGER.info("Reopening connection to Arduino");
            this.led = new Led("Arduino");
        } catch (Exception e) {
            this.reopen(); // try again
        }
    }

}
