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
            for (int i = 0; i < colors.length; i++)
                this.arduino.write(i, final_colors[i] = ColorUtil.lerp(colors[i], final_colors[i], .5));
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
            Thread.sleep(200);
            LOGGER.fine("Reopening connection to Arduino");
            this.arduino = new ArduinoLed("Arduino");
        } catch (Exception e) {
            this.reopen(); // try again
        }
    }

}
