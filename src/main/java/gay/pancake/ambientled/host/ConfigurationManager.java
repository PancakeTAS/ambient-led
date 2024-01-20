package gay.pancake.ambientled.host;

import com.google.gson.Gson;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
     * @param fps The frames per second
     * @param orientation The orientation of the segment (true = horizontal, false = vertical)
     * @param invert If the segment is inverted
     */
    public record Segment(int offset, int length, int display, int x, int y, int width, int height, int steps, int fps, boolean orientation, boolean invert) {}

    /**
     * Strip of leds
     *
     * @param type The type of the strip
     * @param ip The ip of the pi
     * @param port The port of the pi
     * @param com The com port of the arduino
     * @param leds The number of leds
     * @param ups The updates per second
     * @param segments The segments of the strip
     * @param maxBrightness The max brightness of the strip (0 - 255*3)
     * @param reductionR The reduction of red (0 - 1)
     * @param reductionG The reduction of green (0 - 1)
     * @param reductionB The reduction of blue (0 - 1)
     */
    public record Configuration(String type, String ip, int port, String com, int leds, int ups, List<Segment> segments, int maxBrightness, float reductionR, float reductionG, float reductionB) {}

    /** The gson instance */
    private static final Gson GSON = new Gson();

    /** The watch service for the configuration directory */
    private final WatchService watchService;
    /** The configuration directory */
    private final Path configDir = Path.of("config");
    /** The watch thread */
    private final Thread watchThread;

    /** The add consumer */
    private final BiConsumer<String, LedInstance> add;
    /** The remove consumer */
    private final Consumer<String> remove;


    /**
     * Creates a new configuration manager
     *
     * @param add The add consumer
     * @param remove The remove consumer
     * @throws IOException If an I/O error occurs
     */
    public ConfigurationManager(BiConsumer<String, LedInstance> add, Consumer<String> remove) throws IOException {
        this.add = add;
        this.remove = remove;
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
            try (var paths = Files.list(this.configDir)) {
                for (var path : paths.toArray(Path[]::new))
                    this.createConfiguration(this.configDir.relativize(path));
            }

            while (true) {
                var key = this.watchService.take();

                for (var event : key.pollEvents()) {
                    var path = (Path) event.context();
                    if (event.kind() == ENTRY_DELETE)
                        this.removeConfiguration(path);
                    else if (event.kind() == ENTRY_CREATE || event.kind() == ENTRY_MODIFY)
                        this.createConfiguration(path);
                }

                key.reset();
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a configuration from the given path
     *
     * @param path The path
     */
    private void createConfiguration(Path path) throws IOException {
        var name = path.getFileName().toString();
        var file = this.configDir.resolve(path);
        if (!name.toLowerCase(Locale.ROOT).endsWith(".json"))
            return;

        var config = GSON.fromJson(Files.newBufferedReader(file), Configuration[].class);
        this.add.accept(name, new LedInstance(config));
    }

    /**
     * Removes a configuration from the given path
     *
     * @param path The path
     */
    private void removeConfiguration(Path path) {
        var name = path.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".json"))
            return;

        this.remove.accept(name);
    }

    @Override
    public void close() throws IOException {
        this.watchService.close();
        this.watchThread.interrupt();
    }

}
