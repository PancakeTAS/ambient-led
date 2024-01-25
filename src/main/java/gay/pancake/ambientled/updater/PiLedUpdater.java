package gay.pancake.ambientled.updater;

import gay.pancake.ambientled.AmbientLed;
import gay.pancake.ambientled.util.ColorUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

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

    public PiLedUpdater(String ip, int port, int max, int count, float r, float g, float b, float lerp, int ups) throws IOException {
        AmbientLed.LOGGER.fine("Initializing raspberry pi led strip");
        this.port = port;
        this.socket = new Socket(ip, this.port);
        this.socket.setTcpNoDelay(true);
        this.stream = this.socket.getOutputStream();
        this.buffer = new byte[count * 3];

        if (!this.socket.isConnected())
            throw new IOException("Couldn't connect to raspberry pi");

        // send header
        var backingArray = new byte[4*7];
        var buffer = ByteBuffer.wrap(backingArray);
        buffer.putInt(max);
        buffer.putInt(count);
        buffer.putFloat(r);
        buffer.putFloat(g);
        buffer.putFloat(b);
        buffer.putFloat(lerp);
        buffer.putInt(ups);
        buffer.flip();
        this.stream.write(backingArray);
        this.stream.flush();
    }

    public void clear() throws IOException {
        for (int i = 0; i < this.buffer.length / 3; i++)
            this.stream.write(new byte[] { (byte) 0, (byte) 0, (byte) 0 });

        this.stream.flush();
    }

    @Override
    public void write(ColorUtil.Color[] colors) throws IOException {
        for (int i = 0; i < colors.length; i++) {
            var color = colors[i];
            this.buffer[i * 3] = (byte) color.getRed();
            this.buffer[i * 3 + 1] = (byte) color.getGreen();
            this.buffer[i * 3 + 2] = (byte) color.getBlue();
        }
        this.stream.write(this.buffer);
        this.stream.flush();
    }

    @Override
    public void close() throws IOException {
        AmbientLed.LOGGER.fine("Closing raspberry pi led strip on port " + this.port);
        this.socket.close();
        this.stream.close();
    }

}
