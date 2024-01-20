package gay.pancake.ambientled.host.updater;

import gay.pancake.ambientled.host.util.ColorUtil;

import java.io.Closeable;
import java.io.IOException;

/**
 * Led updater interface
 */
public interface LedUpdater extends Closeable {

    /**
     * Set the brightness of the led strip
     *
     * @param r Red
     * @param g Green
     * @param b Blue
     */
    void reduction(float r, float g, float b);

    /**
     * Clear the led strip
     *
     * @throws Exception If the data couldn't be written
     */
    void clear() throws Exception;

    /**
     * Write color data to the led strip
     *
     * @param colors Colors array
     * @param offset Offset
     * @param length Length
     * @throws Exception If the data couldn't be written
     */
    void write(ColorUtil.Color[] colors, int offset, int length) throws Exception;

    /**
     * Create a new ArduinoLed instance
     *
     * @param com Com port
     * @param count Led count
     * @throws IOException If the com port couldn't be opened
     * @throws InterruptedException If the reset sequence was interrupted
     * @return ArduinoLed instance
     */
    static LedUpdater createArduinoLed(String com, int count) throws IOException, InterruptedException {
        return new ArduinoLedUpdater(com, count);
    }

    /**
     * Create a new PiLed instance
     *
     * @param ip IP address
     * @param port Port
     * @param count Led count
     * @return ArduinoLed instance
     * @throws Exception If the connection couldn't be established
     */
    static LedUpdater createPiLed(String ip, int port, int count) throws Exception {
        return new PiLedUpdater(ip, port, count);
    }

}
