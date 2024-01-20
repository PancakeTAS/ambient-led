package gay.pancake.ambientled;

import com.google.gson.Gson;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import static gay.pancake.ambientled.AmbientLed.LOGGER;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Configuration manager for the led strip
 *
 * @author Pancake
 */
public class ConfigurationManager implements Closeable {

    /**
     * Segment of leds on a strip
     *
     * @param offset The offset of the segment
     * @param length The length of the segment
     * @param display The display to use
     * @param x The x offset of the image
     * @param y The y offset of the image
     * @param width The width of the image
     * @param height The height of the image
     * @param steps The step size for averaging
     * @param orientation The orientation of the segment (true = horizontal, false = vertical)
     * @param invert If the segment is inverted
     */
    public record Segment(int offset, int length, int display, int x, int y, int width, int height, int steps, boolean orientation, boolean invert) {}

    /**
     * Strip of leds
     *
     * @param type The type of the strip
     * @param ip The ip of the pi
     * @param port The port of the pi
     * @param com The com port of the arduino
     * @param leds The number of leds
     * @param segments The segments of the strip
     * @param maxBrightness The max brightness of the strip (0 - 255*3)
     * @param reductionR The reduction of red (0 - 1)
     * @param reductionG The reduction of green (0 - 1)
     * @param reductionB The reduction of blue (0 - 1)
     */
    public record Strip(String type, String ip, int port, String com, int leds, List<Segment> segments, int maxBrightness, float reductionR, float reductionG, float reductionB) {}

    /**
     * Configuration for the led strip
     *
     * @param strips The strips
     * @param ups The updates per second
     * @param fps The frames per second
     * @param lerp The lerp value
     */
    public record Configuration(List<Strip> strips, int ups, int fps, float lerp) {}

    /** The gson instance */
    private static final Gson GSON = new Gson();

    /** The watch service for the configuration directory */
    private final WatchService watchService;
    /** The configuration directory */
    private final Path configDir = Path.of("config");
    /** The watch thread */
    private final Thread watchThread;

    /** The reload consumer */
    private final Consumer<Configuration> reload;


    /**
     * Creates a new configuration manager
     *
     * @param reload The reload consumer
     * @throws IOException If an I/O error occurs
     */
    public ConfigurationManager(Consumer<Configuration> reload) throws IOException {
        this.reload = reload;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.configDir.register(this.watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        this.watchThread = new Thread(this::watch);
        this.watchThread.setDaemon(true);
        this.watchThread.setName("Configuration Watcher");
        this.watchThread.start();
    }

    /**
     * Watches the configuration directory for changes
     */
    private void watch() {
        try {
            this.reloadConfiguration();

            while (true) {
                var key = this.watchService.take();

                for (var event : key.pollEvents())
                    if (event.kind() == ENTRY_CREATE || event.kind() == ENTRY_MODIFY)
                        this.reloadConfiguration();

                key.reset();
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to watch configuration directory!", e);
        }
    }

    /**
     * Reloads the configuration
     */
    private void reloadConfiguration() {
        var config = this.configDir.resolve("config.json");
        if (!Files.exists(config))
            return;

        try {
            this.reload.accept(GSON.fromJson(Files.newBufferedReader(config), Configuration.class));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to (re)load configuration!", e);
        }
    }

    @Override
    public void close() throws IOException {
        this.watchService.close();
        this.watchThread.interrupt();
    }

}
