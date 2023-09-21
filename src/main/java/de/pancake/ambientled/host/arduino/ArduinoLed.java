package de.pancake.ambientled.host.arduino;

import com.fazecast.jSerialComm.SerialPort;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Led controller for Arduino based led strips
 * @author Pancake
 */
public class ArduinoLed {

    private final String name;
    private final SerialPort device;
    private final OutputStream stream;

    /**
     * Initialize a new Led strip and open the com port
     * @param name Name of the com port
     * @throws Exception If the com port couldn't be opened
     */
    public ArduinoLed(String name) throws Exception {
        this.name = name;
        this.device = this.findComPort();
        this.device.openPort();
        this.device.setBaudRate(38400);
        this.stream = this.device.getOutputStream();
    }

    /**
     * Clear the led strip
     * @throws IOException If the data couldn't be written
     */
    public void clear() throws IOException {
        for (int i = 0; i < 180; i++)
            this.write(i, Color.BLACK);
    }

    /**
     * Write color data to the led strip
     * @param i Index
     * @param c Color
     * @throws IOException If the data couldn't be written
     */
    public void write(int i, Color c) throws IOException {
        this.stream.write(new byte[] { (byte) (i << 8), (byte) i, (byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue() });
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
     * @return null
     */
    public ArduinoLed close() throws IOException {
        this.clear();
        this.flush();
        this.stream.close();
        this.device.closePort();
        return null;
    }
}
