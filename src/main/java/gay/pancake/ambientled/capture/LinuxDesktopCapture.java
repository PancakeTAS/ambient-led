package gay.pancake.ambientled.capture;

import gay.pancake.ambientled.AmbientLed;
import gay.pancake.ambientled.util.NvFBCInstance;

/**
 * Utility class for capturing the desktop
 *
 * @author Pancake
 */
class LinuxDesktopCapture implements DesktopCapture {

    @Override
    public Capture setupCapture(String screen, int x, int y, int width, int height, int framerate) {
        AmbientLed.LOGGER.fine("Setting up capture record for screen capture: " + x + ", " + y + ", " + width + ", " + height);

        var nvfbc = new NvFBCInstance(screen, x, y, width, height, width, height, framerate);
        nvfbc.start();

        // create capture record
        return new Capture(
                x, y, width, height,
                nvfbc.buffer.join(),
                nvfbc
        );
    }

    @Override
    public void screenshot(Capture capture) {
        AmbientLed.LOGGER.finest("Taking screenshot of portion of screen: " + capture.x() + ", " + capture.y() + ", " + capture.width() + ", " + capture.height());
    }

    @Override
    public void free(Capture capture) {
        AmbientLed.LOGGER.warning("Freeing NvFBC instance is not supported yet");
        System.exit(1);
    }
}
