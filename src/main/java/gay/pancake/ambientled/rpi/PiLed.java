package gay.pancake.ambientled.rpi;

import com.diozero.ws281xj.StripType;
import com.diozero.ws281xj.rpiws281x.WS281x;

import java.io.Closeable;

/**
 * Led controller for raspberry pi based led strips
 *
 * @author Pancake
 */
public class PiLed implements Closeable {

    private final WS281x led;

    /**
     * Initialize the led strip
     *
     * @param gpio GPIO pin
     */
    public PiLed(int gpio) {
        this.led = new WS281x(800_000, 5, gpio, 255, 144, StripType.WS2812, gpio == 13 ? 1 : 0);
    }

    /**
     * Clear the led strip
     */
    public void clear() {
        this.led.allOff();
    }

    /**
     * Write color data to the led strip
     *
     * @param i Index
     * @param r Red
     * @param g Green
     * @param b Blue
     */
    public void write(int i, byte r, byte g, byte b) {
        this.led.setPixelColour(i, Byte.toUnsignedInt(r) << 16 | Byte.toUnsignedInt(g) << 8 | Byte.toUnsignedInt(b));
    }

    /**
     * Flush the stream to the led strip
     */
    public void flush() {
        this.led.render();
    }

    /**
     * Close the connection to the led strip
     */
    @Override
    public void close() {
        this.clear();
        this.led.close();
    }
}
