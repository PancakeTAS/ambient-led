package gay.pancake.ambientled;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.Arrays;
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

    private LedInstance instance;

    /**
     * Initialize host application
     *
     * @throws IOException If the system tray is not supported
     * @throws InterruptedException If the thread is interrupted
     */
    private AmbientLed() throws IOException, InterruptedException {
        try {
            new ControlTray(this);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to initialize system tray", e);
        }

        try (var ignored = new ConfigurationManager(this::reload)) {
            LOGGER.info("Initialization complete");
            Thread.sleep(Long.MAX_VALUE);
        }
    }

    /**
     * Reload configuration
     *
     * @param configuration Configuration
     */
    private void reload(ConfigurationManager.Configuration configuration) {
        try {
            if (this.instance != null) {
                this.instance.close();
                Thread.sleep(4000); // gotta be safe
            }

            this.instance = new LedInstance(configuration);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize led instance", e);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException { new AmbientLed(); }

}
