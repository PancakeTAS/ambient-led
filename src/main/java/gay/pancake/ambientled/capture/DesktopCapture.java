package gay.pancake.ambientled.capture;

import com.sun.jna.Pointer;
import gay.pancake.ambientled.ConfigurationManager;
import gay.pancake.ambientled.util.ColorUtil;

import java.io.IOException;

/**
 * Abstract class for capturing the desktop
 *
 * @author Pancake
 */
public interface DesktopCapture {

    /** Instance of desktop capture */
    DesktopCapture DC = System.getProperty("os.name").toLowerCase().contains("windows") ? new WindowsDesktopCapture() : new LinuxDesktopCapture();

    /**
     * Capture record
     *
     * @param strip Strip
     * @param memory Memory
     * @param attachment Attachment
     */
    record Capture(ConfigurationManager.Segment strip, Pointer memory, Object... attachment) {}

    /**
     * Setup capture record for screen capture
     *
     * @param strip Strip
     * @param framerate Framerate
     * @return Capture record
     */
    Capture setupCapture(ConfigurationManager.Segment strip, int framerate) throws IOException;

    /**
     * Take screenshot of portion of screen
     *
     * @param capture Capture record
     */
    void screenshot(Capture capture) throws IOException;

    /**
     * Free memory of capture record
     *
     * @param capture Capture record
     */
    void free(Capture capture) throws IOException;

    /**
     * Calculate average color for each led
     *
     * @param memory Memory
     * @param colors Colors
     */
    void averages(Capture memory, ColorUtil.Color[] colors);

}
