package de.pancake.ambientled.host.rpi;

import de.pancake.ambientled.host.AmbientLed;
import de.pancake.ambientled.host.util.ColorUtil;
import de.pancake.ambientled.host.util.DesktopCapture;
import lombok.RequiredArgsConstructor;

import static de.pancake.ambientled.host.AmbientLed.LOGGER;

/**
 * Raspberry Pi screen grabber class
 * @author Pancake
 */
@RequiredArgsConstructor
public class PiGrabber implements Runnable {

    // Scaled screen size
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    // Amount of LEDs
    private static final int LEDS = 144;
    // Size of each LED in pixels
    private static final int WIDTH_PER_LED = WIDTH / LEDS;

    /** Ambient led instance */
    private final AmbientLed led;
    /** Desktop capture instance */
    private final DesktopCapture dc = new DesktopCapture();
    /** Captures */
    private final DesktopCapture.Capture
            TOP = this.dc.setupCapture(0, 0, WIDTH, 180),
            BOTTOM = this.dc.setupCapture(0, HEIGHT - 180, WIDTH, 180);

    /**
     * Grab screen and calculate average color for each led
     */
    @Override
    public void run() {
        if (this.led.isPaused())
            return;

        var ms = System.currentTimeMillis();

        // capture screen
        var top = this.dc.screenshot(TOP);
        var bottom = this.dc.screenshot(BOTTOM);

        // calculate average color for each led
        for (int i = 0; i < LEDS; i++) {
            var c = ColorUtil.average(
                    top,
                    WIDTH_PER_LED * i, 0,
                    WIDTH_PER_LED - 1, 180,
                    6, false, false, true
            );

            this.led.getPiUpdater().getColors()[i] = c;
        }

        for (int i = 0; i < LEDS; i++) {
            var c = ColorUtil.average(
                    bottom,
                    WIDTH_PER_LED * i, 0,
                    WIDTH_PER_LED - 1, 180,
                    6, false, false, true
            );

            this.led.getPiUpdater().getColors()[i+LEDS] = c;
        }

        LOGGER.finer("Grabbed screen for raspberry pi in " + (System.currentTimeMillis() - ms) + "ms");
    }

}
