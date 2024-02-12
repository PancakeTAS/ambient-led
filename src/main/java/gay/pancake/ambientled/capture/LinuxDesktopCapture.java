package gay.pancake.ambientled.capture;

import gay.pancake.ambientled.AmbientLed;
import gay.pancake.ambientled.ConfigurationManager;
import gay.pancake.ambientled.util.ColorUtil;
import gay.pancake.ambientled.util.NvFBCInstance;

/**
 * Utility class for capturing the desktop
 *
 * @author Pancake
 */
class LinuxDesktopCapture implements DesktopCapture {

    @Override
    public Capture setupCapture(ConfigurationManager.Segment strip, int framerate) {
        AmbientLed.LOGGER.fine("Setting up capture record for screen capture: " + strip.x() + ", " + strip.y() + ", " + strip.width() + ", " + strip.height());

        // create nvfbc instance
        var nvfbc = new NvFBCInstance(strip.display(), strip.x(), strip.y(), strip.width(), strip.height(), strip.orientation() ? strip.length() : 1, strip.orientation() ? 1 : strip.length(), framerate);
        nvfbc.start();

        // create capture record
        return new Capture(strip, nvfbc.buffer.join(), nvfbc);
    }

    @Override
    public void screenshot(Capture capture) {
        AmbientLed.LOGGER.finest("Taking screenshot of portion of screen: " + capture.strip().x() + ", " + capture.strip().y() + ", " + capture.strip().width() + ", " + capture.strip().height());
    }

    @Override
    public void free(Capture capture) {
        AmbientLed.LOGGER.warning("Freeing NvFBC instance is not supported yet");
        System.exit(1);
    }

    @Override
    public void averages(Capture memory, ColorUtil.Color[] colors) {
        AmbientLed.LOGGER.finest("Calculating average color for each led");
        for (int i = 0; i < memory.strip().length(); i++)
            colors[memory.strip().offset() + (memory.strip().invert() ? memory.strip().length() - i - 1 : i)].setRGB(
                    (Byte.toUnsignedInt(memory.memory().getByte(i * 3L) )),
                    (Byte.toUnsignedInt(memory.memory().getByte(i * 3L + 1))),
                    (Byte.toUnsignedInt(memory.memory().getByte(i * 3L + 2) ))
            );
    }

}
