package gay.pancake.ambientled.host;

import gay.pancake.ambientled.host.arduino.ArduinoGrabber;
import gay.pancake.ambientled.host.arduino.ArduinoUpdater;
import gay.pancake.ambientled.host.rpi.PiGrabber;
import gay.pancake.ambientled.host.rpi.PiUpdater;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    /** Executor service */
    private ScheduledExecutorService executor;

    /** Arduino updater instance */
    @Getter private final ArduinoUpdater arduinoUpdater = new ArduinoUpdater(this);
    /** Arduino grabber instance */
    @Getter private final ArduinoGrabber arduinoGrabber = new ArduinoGrabber(this);
    /** Pi updater instance */
    @Getter private final PiUpdater piUpdater = new PiUpdater(this);
    /** Pi grabber instance */
    @Getter private final PiGrabber piGrabber = new PiGrabber(this);
    /** Is ambient led paused */
    @Getter @Setter private volatile boolean paused = false, frozen = false, efficiency = true;

    /**
     * Initialize host application
     */
    private AmbientLed() {
        try {
            new ControlTray(this);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to initialize system tray", e);
        }

        LOGGER.info("Initialization complete");
        this.startTimers();
    }

    public void startTimers() {
        LOGGER.info("Launching arduino and raspberry pi services");
        if (this.executor != null)
            this.executor.close();

        this.executor = Executors.newScheduledThreadPool(4);
        this.efficiency = false;
        this.executor.scheduleAtFixedRate(this.arduinoUpdater, 0, 1000000 / (this.efficiency ? 4 : 60), TimeUnit.MICROSECONDS);
        this.executor.scheduleAtFixedRate(this.arduinoGrabber, 0, 1000000 / (this.efficiency ? 2 : 30), TimeUnit.MICROSECONDS);
        this.executor.scheduleAtFixedRate(this.piUpdater, 0, 1000000 / (this.efficiency ? 4 : 60), TimeUnit.MICROSECONDS);
        this.executor.scheduleAtFixedRate(this.piGrabber, 0, 1000000 / (this.efficiency ? 2 : 30), TimeUnit.MICROSECONDS);
    }

    public static void main(String[] args) { new AmbientLed(); }

}
