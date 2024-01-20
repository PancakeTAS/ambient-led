package gay.pancake.ambientled.updater;

import com.fazecast.jSerialComm.SerialPort;
import gay.pancake.ambientled.AmbientLed;
import gay.pancake.ambientled.util.ColorUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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

    public ArduinoLedUpdater(String name, int max, int count, float r, float g, float b, float lerp, int ups) throws IOException {
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

        // send header FIXME: new pattern!!
        var backingArray = new byte[4*7];
        var buffer = ByteBuffer.wrap(backingArray);
        buffer.putInt(ups);
        buffer.putFloat(lerp);
        buffer.putFloat(b);
        buffer.putFloat(g);
        buffer.putFloat(r);
        buffer.putInt(count);
        buffer.putInt(max);
        buffer.flip();
        this.stream.write(backingArray);
        this.stream.flush();
    }

    public void clear() throws IOException {
        Arrays.fill(this.buffer, (byte) 0);
        this.stream.write(this.buffer);
        this.stream.flush();
    }

    @Override
    public void write(ColorUtil.Color[] colors) throws IOException {
        for (int i = 0; i < colors.length; i++) {
            var color = colors[i];
            this.buffer[i * 3 ] = (byte) color.getRed();
            this.buffer[i * 3 + 1] = (byte) color.getGreen();
            this.buffer[i * 3 + 2] = (byte) color.getBlue();
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
