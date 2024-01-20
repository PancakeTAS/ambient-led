package gay.pancake.ambientled.host.arduino;

import gay.pancake.ambientled.host.AmbientLed;
import gay.pancake.ambientled.host.capture.DesktopCapture;
import lombok.RequiredArgsConstructor;

import static gay.pancake.ambientled.host.capture.DesktopCapture.DC;

/**
 * Arduino screen grabber class
 * @author Pancake
 */
@RequiredArgsConstructor
public class ArduinoGrabber implements Runnable {

    /** Width and height of the screen */
    private static final int WIDTH = 1920, HEIGHT = 1080;
    private static final int LEDS_RIGHT = 47, LEDS_TOP = 76, LEDS_LEFT = 57;

    /** Ambient led instance */
    private final AmbientLed led;
    /** Captures */
    private final DesktopCapture.Capture
            LEFT = DC.setupCapture(0, 0, 0, 150, HEIGHT),
            TOP = DC.setupCapture(0, 0, 0, WIDTH, 90),
            RIGHT = DC.setupCapture(0, WIDTH - 150, 0, 150, HEIGHT);

    /**
     * Grab screen and calculate average color for each led
     */
    @Override
    public void run() {
        try {
            if (this.led.isPaused() || this.led.isFrozen())
                return;

            var ms = System.currentTimeMillis();

            // capture screen
            DC.screenshot(RIGHT);
            DC.screenshot(TOP);
            DC.screenshot(LEFT);

            DC.averages(RIGHT, false, true, this.led.getArduinoUpdater().getColors(), 0, LEDS_RIGHT);
            DC.averages(TOP, true, true, this.led.getArduinoUpdater().getColors(), LEDS_RIGHT, LEDS_TOP);
            DC.averages(LEFT, false, false, this.led.getArduinoUpdater().getColors(), LEDS_RIGHT + LEDS_TOP, LEDS_LEFT);

            AmbientLed.LOGGER.finer("Grabbed screen for arduino in " + (System.currentTimeMillis() - ms) + "ms");
        } catch (Exception e) {
            AmbientLed.LOGGER.severe("Grabbing screen for arduino failed!");
            e.printStackTrace(System.err);
        }
    }

}
