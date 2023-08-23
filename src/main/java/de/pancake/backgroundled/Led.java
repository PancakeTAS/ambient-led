package de.pancake.backgroundled;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Led strip
 * @author Pancake
 */
public class Led {

    private final String name;
    private final SerialPort device;
    private final OutputStream stream;

    /**
     * Initialize a new Led strip and open the com port
     * @param name Name of the com port
     * @throws Exception If the com port couldn't be opened
     */
    public Led(String name) throws Exception {
        this.name = name;
        this.device = this.findComPort();
        this.device.openPort();
        this.device.setBaudRate(38400);
        this.stream = this.device.getOutputStream();

        for (int i = 0; i < 180; i++)
            this.write(i, 0, 0, 0);
    }

    /**
     * Write color data to the led strip
     * @param i Index
     * @param r Red
     * @param g Green
     * @param b Blue
     * @throws IOException If the data couldn't be written
     */
    public void write(int i, int r, int g, int b) throws IOException {
        this.stream.write(new byte[] { (byte) (i << 8), (byte) i, (byte) r, (byte) g, (byte) b });
    }

    /**
     * Flush the stream to the led strip
     * @throws IOException If the stream couldn't be flushed
     */
    public void flush() throws IOException {
        this.stream.flush();
    }

    /**
     * Tries to find a com port with a given name
     * @return Serial Port
     */
    private SerialPort findComPort() {
        for (var porti : SerialPort.getCommPorts())
            if (porti.getDescriptivePortName().toLowerCase().contains(this.name.toLowerCase()))
                return porti;

        throw new RuntimeException("Couldn't find com port: " + this.name);
    }

    /**
     * Close the connection to the Arduino
     * @throws IOException If the connection couldn't be closed
     */
    public void close() throws IOException {
        this.stream.close();
        this.device.closePort();
    }
}
