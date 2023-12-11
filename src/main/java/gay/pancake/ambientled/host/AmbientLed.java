package gay.pancake.ambientled.host;

import gay.pancake.ambientled.host.arduino.ArduinoGrabber;
import gay.pancake.ambientled.host.arduino.ArduinoUpdater;
import gay.pancake.ambientled.host.rpi.PiGrabber;
import gay.pancake.ambientled.host.rpi.PiUpdater;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    /** Tray menu items */
    private MenuItem pause = null, resume = null;

    /** Arduino updater instance */
    @Getter private final ArduinoUpdater arduinoUpdater = new ArduinoUpdater(this);
    /** Arduino grabber instance */
    @Getter private final ArduinoGrabber arduinoGrabber = new ArduinoGrabber(this);
    /** Pi updater instance */
    @Getter private final PiUpdater piUpdater = new PiUpdater(this);
    /** Pi grabber instance */
    @Getter private final PiGrabber piGrabber = new PiGrabber(this);
    /** Is ambient led paused */
    @Getter @Setter private volatile boolean paused = false;

    /**
     * Initialize host application
     * @throws Exception If something goes wrong
     */
    private AmbientLed() throws Exception {
        // create tray icon
        LOGGER.info("Initializing tray icon");
        var icon = new TrayIcon(new ImageIcon(AmbientLed.class.getResource("/tray.png")).getImage());
        var tray = SystemTray.getSystemTray();
        var popup = new PopupMenu();
        icon.setPopupMenu(popup);
        tray.add(icon);

        // setup tray icons
        (this.pause = popup.add(this.trayEntry("Pause", i -> {
            LOGGER.info("Pausing...");
            this.paused = true;
            this.pause.setEnabled(false);
            this.resume.setEnabled(true);
        }))).setEnabled(true);
        (this.resume = popup.add(this.trayEntry("Resume", i -> {
            LOGGER.info("Resuming...");
            this.paused = false;
            this.pause.setEnabled(true);
            this.resume.setEnabled(false);
        }))).setEnabled(false);

        popup.add(this.trayEntry("Exit Program", i -> {
            try {
                LOGGER.info("Exiting...");
                this.setPaused(true);
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.exit(0);
        }));

        // start timers
        LOGGER.info("Launching arduino and raspberry pi services");
        this.executor.scheduleAtFixedRate(this.arduinoUpdater, 0, 1000000 / 60, TimeUnit.MICROSECONDS);
        this.executor.scheduleAtFixedRate(this.arduinoGrabber, 0, 1000000 / 30, TimeUnit.MICROSECONDS);
        this.executor.scheduleAtFixedRate(this.piUpdater, 0, 1000000 / 60, TimeUnit.MICROSECONDS);
        this.executor.scheduleAtFixedRate(this.piGrabber, 0, 1000000 / 30, TimeUnit.MICROSECONDS);

        LOGGER.info("Initialization complete");
    }

    /**
     * Create tray entry
     * @param title Title of entry
     * @param run Action to run
     * @return Created entry
     */
    private MenuItem trayEntry(String title, Consumer<MenuItem> run) {
        var item = new MenuItem(title);
        item.addActionListener(e -> run.accept(item));
        return item;
    }

    public static void main(String[] args) throws Exception { new AmbientLed(); }

}
