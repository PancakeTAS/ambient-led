package de.pancake.ambientled.host.rpi;

import de.pancake.ambientled.host.AmbientLed;
import de.pancake.ambientled.host.util.ColorUtil;
import lombok.Getter;

import java.awt.*;
import java.util.Arrays;

import static de.pancake.ambientled.host.AmbientLed.LOGGER;

/**
 * Raspberry Pi updater class
 * @author Pancake
 */
public class PiUpdater implements Runnable {

    /** Ambient led instance */
    private final AmbientLed led;
    /** Pi controller instance */
    private PiController pi;
    /** Colors */
    @Getter private final Color[] colors = new Color[144];
    /** Interpolated colors */
    private final Color[] final_colors = new Color[144];

    /**
     * Initialize pi updater
     * @param led Ambient led instance
     */
    public PiUpdater(AmbientLed led) {
        this.led = led;

        Arrays.fill(this.colors, Color.BLACK);
        Arrays.fill(this.final_colors, Color.BLACK);
        this.reconnect();
    }

    /**
     * Update colors of pi
     */
    @Override
    public void run() {
        try {
            // disconnect pi on pause
            if (this.led.isPaused()) {
                if (this.pi != null)
                    this.pi = this.pi.close();

                return;
            }

            // lerp and update colors
            for (int i = 0; i < colors.length; i++)
                final_colors[i] = ColorUtil.lerp(colors[i], final_colors[i], .25);

            this.pi.write(final_colors);
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            this.reconnect();
        }

    }

    /**
     * Reopen connection to raspberry pi
     */
    private void reconnect() {
        try {
            LOGGER.info("Reopening connection to Raspberry Pi");
            this.pi = new PiController("192.168.178.54", 5163);
        } catch (Exception e) {
            this.reconnect(); // try again
        }
    }

}
