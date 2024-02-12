package gay.pancake.ambientled.rpi;

import com.diozero.ws281xj.StripType;
import com.diozero.ws281xj.rpiws281x.WS281x;
import gay.pancake.ambientled.util.ColorUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static gay.pancake.ambientled.AmbientLed.LOGGER;

/**
 * Main class for Raspberry Pi
 * @author Pancake
 */
public class AmbientLedRpi implements Closeable {

    /** Led instance */
    private final WS281x led;
    /** Initial color array */
    private final ColorUtil.Color[] colors;
    /** Final color array */
    private final ColorUtil.Color[] final_colors;
    /** Executor service */
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Create new ambient led
     *
     * @param gpio GPIO pin
     * @param count Number of leds
     */
    public AmbientLedRpi(int gpio, int count) {
        this.led = new WS281x(800_000, 5, gpio, 255, count, StripType.WS2812, gpio == 13 ? 1 : 0);
        this.led.allOff();
        this.led.render();

        this.colors = new ColorUtil.Color[count];
        for (int i = 0; i < count; i++)
            this.colors[i] = new ColorUtil.Color();

        this.final_colors = new ColorUtil.Color[count];
        for (int i = 0; i < count; i++)
            this.final_colors[i] = new ColorUtil.Color();
    }

    /**
     * Setup lerping thread
     *
     * @param max Max brightness
     * @param r Red
     * @param g Green
     * @param b Blue
     * @param lerp Lerp
     * @param ups Updates per second
     */
    private void setup(float lerp, int ups, int max, float r, float g, float b) {
        this.executor.scheduleAtFixedRate(() -> {
            try {
                // lerp and update colors
                var avg = 0;
                for (int i = 0; i < this.colors.length; i++) {
                    ColorUtil.lerp((int) (this.colors[i].getRed() * r), (int) (this.colors[i].getGreen() * g), (int) (this.colors[i].getBlue() * b), this.final_colors[i], lerp);
                    avg += this.final_colors[i].getRed() + this.final_colors[i].getGreen() + this.final_colors[i].getBlue();
                }
                avg = (int) (avg / (double) this.colors.length);

                // reduce max brightness
                var reduction = Math.min(1, max / Math.max(1.0f, avg));

                // write to led
                for (int i = 0; i < this.colors.length; i++) {
                    this.led.setPixelColourRGB(
                            i,
                            Byte.toUnsignedInt((byte) ((int) (this.final_colors[i].getRed() * reduction) & 0xFF)),
                            Byte.toUnsignedInt((byte) ((int) (this.final_colors[i].getGreen() * reduction) & 0xFF)),
                            Byte.toUnsignedInt( (byte) ((int) (this.final_colors[i].getBlue() * reduction) & 0xFF))
                    );
                }
                this.led.render();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while updating leds", e);
                e.printStackTrace();
            }
        }, 0, 1000 / ups, TimeUnit.MILLISECONDS);
    }

    /**
     * Start ambient led
     *
     * @param in Input stream
     * @throws IOException If an I/O error occurs
     */
    public void start(InputStream in) throws IOException {
        var buffer = new byte[3*this.colors.length];
        while ((in.read(buffer) != -1)) {
            for (int i = 0; i < this.colors.length; i++)
                this.colors[i].setRGB(buffer[i*3], buffer[i*3+1], buffer[i*3+2]);
            Thread.yield();
        }
    }

    @Override
    public void close() {
        this.executor.shutdownNow();
        this.led.allOff();
        this.led.render();
        this.led.close();
    }

    public static void main(String[] args) throws Exception {
        var port = Integer.parseInt(args[0]);
        var gpio = Integer.parseInt(args[1]);

        var serverSocket = new ServerSocket(port);
        while (serverSocket.isBound()) {
            // accept new client
            try (var socket = serverSocket.accept();
                 var input = socket.getInputStream()) {
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(2000);
                LOGGER.info("Connection established from " + socket.getInetAddress());

                var backingArray = new byte[4*7];
                if (input.read(backingArray) == -1)
                    continue;

                // read header
                var buffer = ByteBuffer.wrap(backingArray);
                var max = buffer.getInt();
                var count = buffer.getInt();
                var r = buffer.getFloat();
                var g = buffer.getFloat();
                var b = buffer.getFloat();
                var lerp = buffer.getFloat();
                var ups = buffer.getInt();
                buffer.clear();
                LOGGER.info("Requesting " + count + " at " + ups + " updates per second with a " + lerp + " lerp");
                LOGGER.info("Limits: " + max + " " + r + " " + g + " " + b);

                // prepare ambient led
                try (var led = new AmbientLedRpi(gpio, count)) {
                    LOGGER.info("Starting ambient led");
                    led.setup(lerp, ups, max, r, g, b);
                    led.start(input);
                    LOGGER.info("Stopping ambient led");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while running ambient led", e);
                    e.printStackTrace();
                }
            }
        }

        serverSocket.close();
    }

}
