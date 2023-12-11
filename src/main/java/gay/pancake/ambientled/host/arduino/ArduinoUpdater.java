package gay.pancake.ambientled.host.arduino;

import gay.pancake.ambientled.host.AmbientLed;
import gay.pancake.ambientled.host.util.Color;
import gay.pancake.ambientled.host.util.ColorUtil;
import lombok.Getter;

import java.util.logging.Level;

/**
 * Arduino updater class
 * @author Pancake
 */
public class ArduinoUpdater implements Runnable {

    /** Max brightness of all leds divided by number of them */
    public static int MAX_BRIGHTNESS = 145;
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

        for (int i = 0; i < this.colors.length; i++) {
            this.colors[i] = new Color();
            this.final_colors[i] = new Color();
        }
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
                final_colors[i] = ColorUtil.lerp((int) (colors[i].getRed() * R_BRIGHTNESS), (int) (colors[i].getGreen() * G_BRIGHTNESS), (int) (colors[i].getBlue() * B_BRIGHTNESS), final_colors[i], .5);
                max += final_colors[i].getRed() + final_colors[i].getGreen() + final_colors[i].getBlue();
            }

            // reduce max brightness
            max = (int) (max / (double) final_colors.length);
            var reduction = Math.min(1, MAX_BRIGHTNESS / Math.max(1.0f, max));
            for (int i = 0; i < final_colors.length; i++) {
                var c = final_colors[i];
                this.arduino.write(i, (byte) (c.getRed() * reduction), (byte) (c.getGreen() * reduction), (byte) (c.getBlue() * reduction));
            }

            this.arduino.flush();
        } catch (Exception e) {
            AmbientLed.LOGGER.log(Level.WARNING, e.getMessage());
            this.reopen();
        }

    }

    /**
     * Reopen connection to arduino
     */
    private void reopen() {
        try {
            Thread.sleep(500);
            AmbientLed.LOGGER.fine("Reopening connection to Arduino");
            this.arduino = new ArduinoLed("Arduino");
        } catch (Exception e) {
            this.reopen(); // try again
        }
    }

}
