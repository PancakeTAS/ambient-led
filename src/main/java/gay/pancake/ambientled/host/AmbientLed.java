package gay.pancake.ambientled.host;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

/**
 * Main class
 * @author Pancake
 */
public class AmbientLed {

    /** Logger */
    public static final Logger LOGGER;
    public static final Level LOG_LEVEL = Level.FINE;

    static {
        LOGGER = Logger.getLogger("Ambient Led");
        LOGGER.setLevel(LOG_LEVEL);
        LOGGER.setUseParentHandlers(false);

        // create console handler
        var handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() { @Override public String format(LogRecord record) { return formatMessage(record) + "\n"; } });
        handler.setLevel(LOG_LEVEL);

        // replace logger handlers
        Arrays.stream(LOGGER.getHandlers()).forEach(LOGGER::removeHandler);
        LOGGER.addHandler(handler);
    }

    /** Is ambient led paused */
    @Getter @Setter private volatile boolean paused = false, frozen = false, efficiency = true;

    private ControlTray tray;
    private final ConfigurationManager config;
    private final Map<String, LedInstance> instances = new HashMap<>();

    /**
     * Initialize host application
     *
     * @throws IOException If the system tray is not supported
     * @throws InterruptedException If the main thread is interrupted
     */
    private AmbientLed() throws IOException, InterruptedException {
        try {
            this.tray = new ControlTray(this);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to initialize system tray", e);
        }

        this.config = new ConfigurationManager(this::onAdd, this::onRemove);
        LOGGER.info("Initialization complete");
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * Add an instance
     *
     * @param name Instance name
     * @param led Instance
     */
    @SneakyThrows
    private void onAdd(String name, LedInstance led) {
        LOGGER.info("Adding instance " + name);
        var instance = this.instances.get(name);
        if (instance != null)
            instance.close();
        this.instances.put(name, led);
        led.open();
    }

    /**
     * Remove an instance
     *
     * @param name Instance name
     */
    @SneakyThrows
    private void onRemove(String name) {
        LOGGER.info("Removing instance " + name);
        var instance = this.instances.remove(name);
        if (instance != null)
            instance.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException { new AmbientLed(); }

}
