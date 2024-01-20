package gay.pancake.ambientled.host.arduino;

import gay.pancake.ambientled.host.AmbientLed;
import gay.pancake.ambientled.host.updater.LedUpdater;
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

    /** Ambient led instance */
    private final AmbientLed led;
    /** Arduino led instance */
    private LedUpdater arduino;
    /** Colors */
    @Getter private final ColorUtil.Color[] colors = new ColorUtil.Color[180];
    /** Interpolated colors */
    private final ColorUtil.Color[] final_colors = new ColorUtil.Color[180];

    /**
     * Initialize arduino updater
     * @param led Ambient led instance
     */
    public ArduinoUpdater(AmbientLed led) {
        this.led = led;

        for (int i = 0; i < this.colors.length; i++) {
            this.colors[i] = new ColorUtil.Color();
            this.final_colors[i] = new ColorUtil.Color();
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
                if (this.arduino != null) {
                    this.arduino.close();
                    this.arduino = null;
                }

                return;
            }

            // return on freeze
            if (this.led.isFrozen())
                return;

            // lerp and update colors
            int max = 0;
            for (int i = 0; i < colors.length; i++) {
                final_colors[i] = ColorUtil.lerp(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), final_colors[i], .5);
                max += final_colors[i].getRed() + final_colors[i].getGreen() + final_colors[i].getBlue();
            }

            // reduce max brightness
            max = (int) (max / (double) final_colors.length);
            var reduction = Math.min(1, MAX_BRIGHTNESS / Math.max(1.0f, max));
            this.arduino.reduction(reduction, reduction * 0.7f, reduction);
            this.arduino.write(final_colors, 0, final_colors.length);
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
            this.arduino = LedUpdater.createArduinoLed("Arduino", 180);
        } catch (Exception e) {
            this.reopen(); // try again
        }
    }

}
