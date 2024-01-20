package gay.pancake.ambientled.host.updater;

import com.fazecast.jSerialComm.SerialPort;
import gay.pancake.ambientled.host.AmbientLed;
import gay.pancake.ambientled.host.util.ColorUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Led controller for Arduino based led strips
 *
 * @author Pancake
 */
class ArduinoLedUpdater implements LedUpdater {

    /** Name of the com port */
    private final String name;
    /** Serial port */
    private final SerialPort device;
    /** Output stream */
    private final OutputStream stream;
    /** Buffer */
    private final byte[] buffer;

    /** Brightness modifiers of red, green and blue leds */
    private float r = 1.0f, g = 1.0f, b = 1.0f;

    /**
     * Initialize a new Led strip and open the com port
     *
     * @param name Name of the com port
     * @param count Number of leds
     * @throws IOException If the com port couldn't be opened
     */
    public ArduinoLedUpdater(String name, int count) throws IOException {
        AmbientLed.LOGGER.fine("Initializing arduino led strip");
        this.name = name;
        this.device = this.findComPort();
        this.device.openPort();
        this.device.setBaudRate(460800);
        this.stream = this.device.getOutputStream();

        this.buffer = new byte[count * 3];

        // send reset sequence
        var input = this.device.getInputStream();
        while (input.available() < 1) {
            this.stream.write("RESET!!!".getBytes(StandardCharsets.US_ASCII));
            this.stream.flush();

            try {
                Thread.sleep(100);
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    public void reduction(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void clear() throws IOException {
        Arrays.fill(this.buffer, (byte) 0);
        this.stream.write(this.buffer);
        this.stream.flush();
    }

    @Override
    public void write(ColorUtil.Color[] colors, int offset, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            var color = colors[i + offset];
            this.buffer[i * 3 ] = (byte) (color.getRed() * this.r);
            this.buffer[i * 3 + 1] = (byte) (color.getGreen() * this.g);
            this.buffer[i * 3 + 2] = (byte) (color.getBlue() * this.b);
        }
        this.stream.write(this.buffer);
        this.stream.flush();
    }

    @Override
    public void close() throws IOException {
        AmbientLed.LOGGER.fine("Closing arduino led strip");
        this.clear();
        this.stream.close();
        this.device.closePort();
    }

    /**
     * Tries to find a com port with a given name
     *
     * @return Serial Port
     */
    private SerialPort findComPort() {
        for (var porti : SerialPort.getCommPorts())
            if (porti.getDescriptivePortName().toLowerCase().contains(this.name.toLowerCase()))
                return porti;

        throw new RuntimeException("Couldn't find com port: " + this.name);
    }

}
