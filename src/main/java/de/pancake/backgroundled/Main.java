package de.pancake.backgroundled;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Main class
 * @author Pancake
 */
public class Main {

    public static final Logger LOGGER = Logger.getLogger("Background Led");
    public static final int[][] COLORS = new int[180][3];
    public static final Timer timer = new Timer();
    public static volatile boolean paused = false;

    private static MenuItem pause;
    private static MenuItem resume;

    /**
     * Main method
     * @param args Command line arguments
     * @throws Exception If something goes wrong
     */
    public static void main(String[] args) throws Exception {
        // create tray icon
        var icon = new TrayIcon(new ImageIcon(Main.class.getResource("/tray.png")).getImage());
        var tray = SystemTray.getSystemTray();
        var popup = new PopupMenu();
        icon.setPopupMenu(popup);
        tray.add(icon);

        // setup tray icons
        (pause = popup.add(trayEntry("Pause", i -> {
            paused = true;
            pause.setEnabled(false);
            resume.setEnabled(true);
        }))).setEnabled(true);
        (resume = popup.add(trayEntry("Resume", i -> {
            paused = false;
            pause.setEnabled(true);
            resume.setEnabled(false);
        }))).setEnabled(false);

        popup.add(trayEntry("Exit Program", i -> System.exit(0)));

        // start timers
        timer.scheduleAtFixedRate(new LedUpdater(), 0, 1000/60);
        timer.scheduleAtFixedRate(new ScreenGrabber(), 0, 1000/10);
    }

    /**
     * Creates a tray entry
     * @param title Title of the entry
     * @param run Action to run
     * @return The created entry
     */
    private static MenuItem trayEntry(String title, Consumer<MenuItem> run) {
        var item = new MenuItem(title);
        item.addActionListener(e -> run.accept(item));
        return item;
    }

}
