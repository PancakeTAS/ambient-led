package de.pancake.ambientled.host.arduino;

import de.pancake.ambientled.host.AmbientLed;
import de.pancake.ambientled.host.util.ColorUtil;
import de.pancake.ambientled.host.util.DesktopCapture;
import lombok.RequiredArgsConstructor;

import static de.pancake.ambientled.host.AmbientLed.LOGGER;

/**
 * Arduino screen grabber class
 * @author Pancake
 */
@RequiredArgsConstructor
public class ArduinoGrabber implements Runnable {

    /** Width and height of the screen */
    private static final int WIDTH = 3840, HEIGHT = 2160;
    private static final int LEDS_RIGHT = 47, LEDS_TOP = 76, LEDS_LEFT = 57;
    /** Width and height per led */
    private static final int
            R_HEIGHT_PER_LED = HEIGHT / LEDS_RIGHT,
            WIDTH_PER_LED = WIDTH / LEDS_TOP,
            L_HEIGHT_PER_LED = HEIGHT / LEDS_LEFT;

    /** Ambient led instance */
    private final AmbientLed led;
    /** Desktop capture instance */
    private final DesktopCapture dc = new DesktopCapture();
    /** Captures */
    private final DesktopCapture.Capture
            LEFT = this.dc.setupCapture(3840, 0, 300, HEIGHT),
            TOP = this.dc.setupCapture(3840, 0, WIDTH, 180),
            RIGHT = this.dc.setupCapture(3840 + WIDTH - 300, 0, 300, HEIGHT);

    /**
     * Grab screen and calculate average color for each led
     */
    @Override
    public void run() {
        try {
            if (this.led.isPaused())
                return;

            var ms = System.currentTimeMillis();

            // capture screen
            var left = this.dc.screenshot(LEFT);
            var top = this.dc.screenshot(TOP);
            var right = this.dc.screenshot(RIGHT);

            // calculate average color for each led
            for (int i = 0; i < LEDS_RIGHT; i++) {
                var c = ColorUtil.average(
                        right,
                        0, R_HEIGHT_PER_LED * (LEDS_RIGHT - i - 1),
                        300, R_HEIGHT_PER_LED - 1,
                        5
                );

                this.led.getArduinoUpdater().getColors()[i] = c;
            }

            for (int i = 0; i < LEDS_TOP; i++) {
                var c = ColorUtil.average(
                        top,
                        WIDTH_PER_LED * (LEDS_TOP - i - 1), 0,
                        WIDTH_PER_LED - 1, 180,
                        5
                );

                this.led.getArduinoUpdater().getColors()[i + LEDS_RIGHT] = c;
            }

            for (int i = 0; i < LEDS_LEFT; i++) {
                var c = ColorUtil.average(
                        left,
                        0, L_HEIGHT_PER_LED * i,
                        300, L_HEIGHT_PER_LED - 1,
                        5
                );

                this.led.getArduinoUpdater().getColors()[i + LEDS_RIGHT + LEDS_TOP] = c;
            }


            LOGGER.finer("Grabbed screen for arduino in " + (System.currentTimeMillis() - ms) + "ms");
        } catch (Exception e) {
            LOGGER.severe("Grabbing screen for arduino failed!");
            e.printStackTrace(System.err);
        }
    }

}
