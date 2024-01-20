package gay.pancake.ambientled.host.rpi;

import gay.pancake.ambientled.host.AmbientLed;
import gay.pancake.ambientled.host.capture.DesktopCapture;
import lombok.RequiredArgsConstructor;

import static gay.pancake.ambientled.host.AmbientLed.LOGGER;
import static gay.pancake.ambientled.host.capture.DesktopCapture.DC;

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

    /** Ambient led instance */
    private final AmbientLed led;
    /** Captures */
    private final DesktopCapture.Capture
            TOP = DC.setupCapture(0, 0, 0, WIDTH, 90),
            BOTTOM = DC.setupCapture(0, 0, HEIGHT - 90, WIDTH, 90);

    /**
     * Grab screen and calculate average color for each led
     */
    @Override
    public void run() {
        if (this.led.isPaused() || this.led.isFrozen())
            return;

        var ms = System.currentTimeMillis();

        // capture screen
        DC.screenshot(TOP);
        DC.screenshot(BOTTOM);

        // calculate average color for each led
        DC.averages(TOP, true, false, this.led.getPiUpdater().getColors(), 0, LEDS);
        DC.averages(BOTTOM, true, false, this.led.getPiUpdater().getColors(), LEDS, LEDS);

        LOGGER.finer("Grabbed screen for raspberry pi in " + (System.currentTimeMillis() - ms) + "ms");
    }

}
