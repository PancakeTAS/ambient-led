package gay.pancake.ambientled.host.rpi;

import gay.pancake.ambientled.host.AmbientLed;
import gay.pancake.ambientled.host.util.Color;
import gay.pancake.ambientled.host.util.ColorUtil;
import lombok.Getter;

import java.util.logging.Level;

/**
 * Raspberry Pi updater class
 * @author Pancake
 */
public class PiUpdater implements Runnable {

    /** Max brightness of all leds divided by number of them */
    public static int MAX_BRIGHTNESS_1 = 400, MAX_BRIGHTNESS_2 = 300;
    /** Brightness modifiers of red, green and blue leds */
    public static float R_BRIGHTNESS = 1.0f, G_BRIGHTNESS = 1.0f, B_BRIGHTNESS = 1.0f;

    /** Ambient led instance */
    private final AmbientLed led;
    /** Pi controller instance */
    private PiController pi, pi2;
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

        for (int i = 0; i < this.colors.length; i++) {
            this.colors[i] = new Color();
            this.final_colors[i] = new Color();
            this.final_reduced_colors[i] = new Color();
        }
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

                if (this.pi2 != null)
                    this.pi2 = this.pi2.close();

                return;
            }

            // return on freeze
            if (this.led.isFrozen())
                return;

            // lerp and update colors
            int max = 0;
            for (int i = 0; i < final_colors.length; i++) {
                final_colors[i] = ColorUtil.lerp((int) (colors[i].getRed() * R_BRIGHTNESS), (int) (colors[i].getGreen() * G_BRIGHTNESS), (int) (colors[i].getBlue() * B_BRIGHTNESS), final_colors[i], .5);
                max += final_colors[i].getRed() + final_colors[i].getGreen() + final_colors[i].getBlue();
            }
            max = (int) (max / (double) final_colors.length);

            // reduce max brightness for first 144 leds
            var reduction = Math.min(1, MAX_BRIGHTNESS_1 / Math.max(1.0f, max));
            for (int i = 0; i < 144; i++) {
                var c = final_colors[i];
                this.final_reduced_colors[i].setRGB((int) (c.getRed() * reduction), (int) (c.getGreen() * reduction), (int) (c.getBlue() * reduction));
            }

            // reduce max brightness for last 144 leds
            reduction = Math.min(1, MAX_BRIGHTNESS_2 / Math.max(1.0f, max));
            for (int i = 144; i < 288; i++) {
                var c = final_colors[i];
                this.final_reduced_colors[i].setRGB((int) (c.getRed() * reduction), (int) (c.getGreen() * reduction), (int) (c.getBlue() * reduction));
            }

            this.pi.write(this.final_reduced_colors, 0, 144);
            this.pi2.write(this.final_reduced_colors, 144, 144);
        } catch (Exception e) {
            AmbientLed.LOGGER.log(Level.WARNING, e.getMessage());
            this.reconnect();
        }

    }

    /**
     * Reopen connection to raspberry pi
     */
    private void reconnect() {
        try {
            AmbientLed.LOGGER.fine("Reopening connection to Raspberry Pi");
            this.pi = new PiController("192.168.178.54", 5163);
            this.pi2 = new PiController("192.168.178.64", 5164);
        } catch (Exception e) {
            this.reconnect(); // try again
        }
    }

}
