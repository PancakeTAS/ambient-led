package gay.pancake.ambientled.host.rpi;

import gay.pancake.ambientled.host.util.Color;
import gay.pancake.ambientled.host.AmbientLed;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Led controller for Arduino based led strips
 * @author Pancake
 */
public class PiController {

    /** Port of the Raspberry Pi */
    private final int port;
    /** Socket */
    private final Socket socket;
    /** Output stream */
    private final OutputStream stream;
    /** Buffer */
    private final byte[] buf = new byte[144*3];

    /**
     * Initialize led strip controller
     * @param ip IP of the Raspberry Pi
     * @param port Port of the Raspberry Pi
     * @throws Exception If the connection couldn't be established
     */
    public PiController(String ip, int port) throws Exception {
        AmbientLed.LOGGER.fine("Initializing raspberry pi led strip");
        this.port = port;
        this.socket = new Socket(ip, this.port);
        this.socket.setTcpNoDelay(true);
        this.stream = this.socket.getOutputStream();

        if (!this.socket.isConnected())
            throw new Exception("Couldn't connect to raspberry pi");
    }

    /**
     * Clear the led strip
     * @throws IOException If the data couldn't be written
     */
    public void clear() throws IOException {
        for (int i = 0; i < 144; i++)
            this.stream.write(new byte[] { (byte) 0, (byte) 0, (byte) 0 });

        this.stream.flush();
    }

    /**
     * Write color data to the led strip
     * @param colors Colors with a length of 144
     * @throws IOException If the data couldn't be written
     */
    public void write(Color[] colors, int index, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            this.buf[i * 3] = (byte) (colors[i + index].getRed());
            this.buf[i * 3 + 1] = (byte) (colors[i + index].getGreen());
            this.buf[i * 3 + 2] = (byte) (colors[i + index].getBlue());
        }

        this.stream.write(this.buf);
    }

    /**
     * Close the connection to the raspberry pi
     * @throws IOException If the connection couldn't be closed
     * @return null
     */
    public PiController close() throws IOException {
        AmbientLed.LOGGER.fine("Closing raspberry pi led strip on port " + this.port);
        this.clear();
        this.stream.close();
        this.socket.close();
        return null;
    }

}
