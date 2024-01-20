package gay.pancake.ambientled.updater;

import gay.pancake.ambientled.util.ColorUtil;

import java.io.Closeable;
import java.io.IOException;

/**
 * Led updater interface
 */
public interface LedUpdater extends Closeable {

    /**
     * Write color data to the led strip
     *
     * @param colors Colors array
     * @throws IOException If the data couldn't be written
     */
    void write(ColorUtil.Color[] colors) throws IOException;

    /**
     * Create a new ArduinoLed instance
     *
     * @param com Com port
     * @param max Max brightness
     * @param count Led count
     * @param r Red multiplier
     * @param g Green multiplier
     * @param b Blue multiplier
     * @param lerp Lerp value
     * @param ups Updates per second
     * @throws IOException If the com port couldn't be opened
     * @return ArduinoLed instance
     */
    static LedUpdater createArduinoLed(String com, int max, int count, float r, float g, float b, float lerp, int ups) throws IOException {
        return new ArduinoLedUpdater(com, max, count, r, g, b, lerp, ups);
    }

    /**
     * Create a new PiLed instance
     *
     * @param ip IP address
     * @param port Port
     * @param max Max brightness
     * @param count Led count
     * @param r Red multiplier
     * @param g Green multiplier
     * @param b Blue multiplier
     * @param lerp Lerp value
     * @param ups Updates per second
     * @return PiLed instance
     * @throws IOException If the connection couldn't be established
     */
    static LedUpdater createPiLed(String ip, int port, int max, int count, float r, float g, float b, float lerp, int ups) throws IOException {
        return new PiLedUpdater(ip, port, max, count, r, g, b, lerp, ups);
    }

}
