package gay.pancake.ambientled.host.updater;

import gay.pancake.ambientled.host.AmbientLed;
import gay.pancake.ambientled.host.util.ColorUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Led controller for Arduino based led strips
 *
 * @author Pancake
 */
class PiLedUpdater implements LedUpdater {

    /** Port of the Raspberry Pi */
    private final int port;
    /** Socket */
    private final Socket socket;
    /** Output stream */
    private final OutputStream stream;
    /** Buffer */
    private final byte[] buffer;

    /** Brightness modifiers of red, green and blue leds */
    private float r = 1.0f, g = 1.0f, b = 1.0f;

    /**
     * Initialize led strip controller
     *
     * @param ip IP of the Raspberry Pi
     * @param port Port of the Raspberry Pi
     * @param count Number of leds
     */
    public PiLedUpdater(String ip, int port, int count) throws Exception {
        AmbientLed.LOGGER.fine("Initializing raspberry pi led strip");
        this.port = port;
        this.socket = new Socket(ip, this.port);
        this.socket.setTcpNoDelay(true);
        this.stream = this.socket.getOutputStream();
        this.buffer = new byte[count * 3];

        if (!this.socket.isConnected())
            throw new Exception("Couldn't connect to raspberry pi");
    }

    @Override
    public void reduction(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void clear() throws IOException {
        for (int i = 0; i < this.buffer.length / 3; i++)
            this.stream.write(new byte[] { (byte) 0, (byte) 0, (byte) 0 });

        this.stream.flush();
    }

    @Override
    public void write(ColorUtil.Color[] colors, int offset, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            var color = colors[i + offset];
            this.buffer[i * 3] = (byte) (color.getRed() * this.r);
            this.buffer[i * 3 + 1] = (byte) (color.getGreen() * this.g);
            this.buffer[i * 3 + 2] = (byte) (color.getBlue() * this.b);
        }

        this.stream.write(this.buffer);
    }

    @Override
    public void close() throws IOException {
        AmbientLed.LOGGER.fine("Closing raspberry pi led strip on port " + this.port);
        this.clear();
        this.stream.close();
        this.socket.close();
    }

}
