package gay.pancake.ambientled.capture;

import com.sun.jna.Memory;
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
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param memory Memory
     * @param attachment Attachment
     */
    record Capture(int x, int y, int width, int height, Memory memory, Object... attachment) {}

    /**
     * Setup capture record for screen capture
     *
     * @param screen Screen
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @return Capture record
     */
    Capture setupCapture(int screen, int x, int y, int width, int height) throws IOException;

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
     * @param orientation Orientation (true = horizontal, false = vertical)
     * @param inverse Is order of leds reversed
     * @param colors Colors
     * @param offset Offset
     * @param len Length
     * @param step Step size
     */
    default void averages(Capture memory, boolean orientation, boolean inverse, ColorUtil.Color[] colors, int offset, int len, int step) {
        var pixelsPerLed = (orientation ? memory.width() : memory.height()) / len;
        for (int i = 0; i < len; i++) {
            ColorUtil.average(
                    memory.memory, memory.width(),
                    orientation ? (pixelsPerLed * i) : 0, orientation ? 0 : (pixelsPerLed * i),
                    orientation ? pixelsPerLed : memory.width(), orientation ? memory.height() : pixelsPerLed,
                    step, colors[offset + (inverse ? len - i - 1 : i)]
            );
        }
    }

}
