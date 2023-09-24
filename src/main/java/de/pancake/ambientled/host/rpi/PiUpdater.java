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

    /** Max brightness of all leds divided by number of them */
    public static int MAX_BRIGHTNESS = 200;
    /** Brightness modifiers of red, green and blue leds */
    public static float R_BRIGHTNESS = 1.0f, G_BRIGHTNESS = 1.0f, B_BRIGHTNESS = 1.0f;

    /** Ambient led instance */
    private final AmbientLed led;
    /** Pi controller instance */
    private PiController piTop, piBottom;
    /** Colors */
    @Getter private final Color[] colors = new Color[288];
    /** Interpolated colors */
    private final Color[] final_colors = new Color[288];
    /** Interpolated reduced colors */
    private final Color[] final_reduced_colors = new Color[288];


    /**
     * Initialize pi updater
     * @param led Ambient led instance
     */
    public PiUpdater(AmbientLed led) {
        this.led = led;

        Arrays.fill(this.colors, Color.BLACK);
        Arrays.fill(this.final_colors, Color.BLACK);
        Arrays.fill(this.final_reduced_colors, Color.BLACK);
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
            int max = 0;
            for (int i = 0; i < final_colors.length; i++) {
                final_colors[i] = ColorUtil.lerp(new Color((int) (colors[i].getRed() * R_BRIGHTNESS), (int) (colors[i].getGreen() * G_BRIGHTNESS), (int) (colors[i].getBlue() * B_BRIGHTNESS)), final_colors[i], .5);
                max += final_colors[i].getRed() + final_colors[i].getGreen() + final_colors[i].getBlue();
            }

            // reduce max brightness
            max = (int) (max / (double) final_colors.length);
            var reduction = Math.min(1, MAX_BRIGHTNESS / Math.max(1.0f, max));
            for (int i = 0; i < final_colors.length; i++) {
                var c = final_colors[i];
                this.final_reduced_colors[i] = new Color((int) (c.getRed() * reduction), (int) (c.getGreen() * reduction), (int) (c.getBlue() * reduction));
            }

            this.piTop.write(this.final_reduced_colors, 0, 144);
            this.piBottom.write(this.final_reduced_colors, 144, 144);
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
