package de.pancake.ambientled.host.arduino;

import de.pancake.ambientled.host.AmbientLed;
import de.pancake.ambientled.host.util.ColorUtil;
import lombok.Getter;

import java.awt.*;
import java.util.Arrays;
import java.util.logging.Level;

import static de.pancake.ambientled.host.AmbientLed.LOGGER;

/**
 * Arduino updater class
 * @author Pancake
 */
public class ArduinoUpdater implements Runnable {

    /** Max brightness of all leds divided by number of them */
    public static int MAX_BRIGHTNESS = 140;
    /** Brightness modifiers of red, green and blue leds */
    public static float R_BRIGHTNESS = 1.0f, G_BRIGHTNESS = 0.7f, B_BRIGHTNESS = 1.0f;

    /** Ambient led instance */
    private final AmbientLed led;
    /** Arduino led instance */
    private ArduinoLed arduino;
    /** Colors */
    @Getter private final Color[] colors = new Color[180];
    /** Interpolated colors */
    private final Color[] final_colors = new Color[180];

    /**
     * Initialize arduino updater
     * @param led Ambient led instance
     */
    public ArduinoUpdater(AmbientLed led) {
        this.led = led;

        Arrays.fill(this.colors, Color.BLACK);
        Arrays.fill(this.final_colors, Color.BLACK);
        this.reopen();
    }

    /**
     * Update colors of arduino
     */
    @Override
    public void run() {
        try {
            // disconnect arduino on pause
            if (this.led.isPaused()) {
                if (this.arduino != null)
                    this.arduino = this.arduino.close();

                return;
            }

            // lerp and update colors
            int max = 0;
            for (int i = 0; i < colors.length; i++) {
                final_colors[i] = ColorUtil.lerp(new Color((int) (colors[i].getRed() * R_BRIGHTNESS), (int) (colors[i].getGreen() * G_BRIGHTNESS), (int) (colors[i].getBlue() * B_BRIGHTNESS)), final_colors[i], 0.5f);
                max += final_colors[i].getRed() + final_colors[i].getGreen() + final_colors[i].getBlue();
            }

            // reduce max brightness
            max = (int) (max / (double) final_colors.length);
            var reduction = Math.min(1, MAX_BRIGHTNESS / Math.max(1.0f, max));
            for (int i = 0; i < final_colors.length; i++) {
                var c = final_colors[i];
                this.arduino.write(i, new Color((int) (c.getRed() * reduction), (int) (c.getGreen() * reduction), (int) (c.getBlue() * reduction)));
            }

            this.arduino.flush();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            this.reopen();
        }

    }

    /**
     * Reopen connection to arduino
     */
    private void reopen() {
        try {
            Thread.sleep(500);
            LOGGER.fine("Reopening connection to Arduino");
            this.arduino = new ArduinoLed("Arduino");
        } catch (Exception e) {
            this.reopen(); // try again
        }
    }

}
