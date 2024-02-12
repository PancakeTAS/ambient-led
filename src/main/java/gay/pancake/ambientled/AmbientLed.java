package gay.pancake.ambientled;

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

    private LedInstance instance;

    /**
     * Initialize host application
     *
     * @throws IOException If the system tray is not supported
     * @throws InterruptedException If the thread is interrupted
     */
    private AmbientLed() throws IOException, InterruptedException {
        try (var ignored = new ConfigurationManager(this::reload)) {
            LOGGER.info("Initialization complete");
            Thread.sleep(Long.MAX_VALUE);
        }
    }

    /**
     * Reload configuration
     *
     * @param configuration Configuration
     * @return If the configuration was reloaded
     */
    private boolean reload(ConfigurationManager.Configuration configuration) {
        try {
            if (this.instance != null)
                this.instance.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to close led instance", e);
            e.printStackTrace();
            return false;
        }

        try {
            LOGGER.info("Initializing new led instance");
            this.instance = new LedInstance(configuration, this::reload);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize led instance", e);
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException { new AmbientLed(); }

}
