package gay.pancake.ambientled.updater;

import com.fazecast.jSerialComm.SerialPort;
import gay.pancake.ambientled.AmbientLed;
import gay.pancake.ambientled.util.ColorUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
        this.device.setBaudRate(490800*2);
        this.stream = this.device.getOutputStream();

        this.buffer = new byte[count * 3];

        // send header
        var backingArray = new byte[16];
        var buffer = ByteBuffer.wrap(backingArray);
        buffer.putInt(max);
        buffer.putInt(count);
        buffer.put((byte) (r * 255.0f - 128));
        buffer.put((byte) (g * 255.0f - 128));
        buffer.put((byte) (b * 255.0f - 128));
        buffer.put((byte) (lerp * 255.0f - 128));
        buffer.putInt(ups);
        buffer.flip();
        this.stream.write(backingArray);
        this.stream.flush();

        // wait for arduino to be ready
        var in = this.device.getInputStream();
        while (in.available() < 1)
            Thread.yield();
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
