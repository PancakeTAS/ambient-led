package de.pancake.ambientled.host.rpi;

import de.pancake.ambientled.host.AmbientLed;
import de.pancake.ambientled.host.util.ColorUtil;
import lombok.Getter;

import java.awt.*;
import java.util.Arrays;
import java.util.logging.Level;

import static de.pancake.ambientled.host.AmbientLed.LOGGER;

/**
 * Raspberry Pi updater class
 * @author Pancake
 */
public class PiUpdater implements Runnable {

    /** Ambient led instance */
    private final AmbientLed led;
    /** Pi controller instance */
    private PiController piTop, piBottom;
    /** Colors */
    @Getter private final Color[] colors = new Color[288];
    /** Interpolated colors */
    private final Color[] final_colors_top = new Color[144];
    private final Color[] final_colors_bottom = new Color[144];

    /**
     * Initialize pi updater
     * @param led Ambient led instance
     */
    public PiUpdater(AmbientLed led) {
        this.led = led;

        Arrays.fill(this.colors, Color.BLACK);
        Arrays.fill(this.final_colors_top, Color.BLACK);
        Arrays.fill(this.final_colors_bottom, Color.BLACK);
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
                if (this.piTop != null)
                    this.piTop = this.piTop.close();

                if (this.piBottom != null)
                    this.piBottom = this.piBottom.close();

                return;
            }

            // lerp and update colors
            for (int i = 0; i < final_colors_top.length; i++)
                final_colors_top[i] = ColorUtil.lerp(colors[i], final_colors_top[i], .5);

            // lerp and update colors
            for (int i = 0; i < final_colors_bottom.length; i++)
                final_colors_bottom[i] = ColorUtil.lerp(colors[i+144], final_colors_bottom[i], .5);

            this.piTop.write(final_colors_top);
            this.piBottom.write(final_colors_bottom);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            this.reconnect();
        }

    }

    /**
     * Reopen connection to raspberry pi
     */
    private void reconnect() {
        try {
            LOGGER.fine("Reopening connection to Raspberry Pi");
            this.piTop = new PiController("192.168.178.54", 5163);
            this.piBottom = new PiController("192.168.178.54", 5164);
        } catch (Exception e) {
            this.reconnect(); // try again
        }
    }

}
