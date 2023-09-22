package de.pancake.ambientled.rpi;

import com.diozero.ws281xj.rpiws281x.WS281x;

import java.io.IOException;

/**
 * Led controller for raspberry pi based led strips
 * @author Pancake
 */
public class PiLed {

    private final WS281x led;

    /**
     * Initialize the led strip
     * @param gpio GPIO pin
     */
    public PiLed(int gpio) {
        this.led = new WS281x(gpio, 255, 144);
    }

    /**
     * Clear the led strip
     */
    public void clear() {
        this.led.allOff();
    }

    /**
     * Write color data to the led strip
     * @param i Index
     * @param c Color
     * @throws IOException If the data couldn't be written
     */
    public void write(int i, byte r, byte g, byte b) throws IOException {
        this.led.setPixelColour(i, Byte.toUnsignedInt(r) << 16 | Byte.toUnsignedInt(g) << 8 | Byte.toUnsignedInt(b));
    }

    /**
     * Flush the stream to the led strip
     * @throws IOException If the stream couldn't be flushed
     */
    public void flush() throws IOException {
        this.led.render();
    }

    /**
     * Close the connection to the led strip
     * @throws IOException If the connection couldn't be closed
     * @return null
     */
    public PiLed close() throws IOException {
        this.clear();
        this.led.close();
        return null;
    }
}
