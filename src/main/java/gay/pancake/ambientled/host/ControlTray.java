package gay.pancake.ambientled.host;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

import static gay.pancake.ambientled.host.AmbientLed.LOGGER;

public class ControlTray {

    /** Tray menu items */
    private MenuItem pause = null, freeze = null, efficiencymode = null;

    /**
     * Create a tray menu item
     * @throws Exception If something goes wrong
     */
    public ControlTray(AmbientLed led) throws Exception {
        LOGGER.info("Initializing tray icon");
        var icon = new TrayIcon(new ImageIcon(Objects.requireNonNull(AmbientLed.class.getResource("/tray.png"))).getImage());
        var tray = SystemTray.getSystemTray();
        var popup = new PopupMenu();
        icon.setPopupMenu(popup);
        tray.add(icon);

        // setup tray icons
        (this.pause = popup.add(this.trayEntry("Pause", i -> {
            if (led.isPaused()) {
                LOGGER.info("Resuming...");
                led.setPaused(false);
                this.pause.setLabel("Pause");
            } else {
                LOGGER.info("Pausing...");
                led.setPaused(true);
                this.pause.setLabel("Resume");
            }
        }))).setEnabled(true);

        // setup tray icons
        (this.freeze = popup.add(this.trayEntry("Freeze", i -> {
            if (led.isFrozen()) {
                LOGGER.info("Unfreezing...");
                led.setFrozen(false);
                this.freeze.setLabel("Freeze");
            } else {
                LOGGER.info("Freezing...");
                led.setFrozen(true);
                this.freeze.setLabel("Unfreeze");
            }
        }))).setEnabled(true);

        // setup efficiency toggle
        (this.efficiencymode = popup.add(this.trayEntry("Disable Efficiency Mode", i -> {
            if (led.isEfficiency()) {
                LOGGER.info("Disabling efficiency mode...");
                led.setEfficiency(false);
                this.efficiencymode.setLabel("Efficiency Mode");
            } else {
                LOGGER.info("Enabling efficiency mode...");
                led.setEfficiency(true);
                this.efficiencymode.setLabel("Disable Efficiency Mode");
            }
            led.startTimers();
        }))).setEnabled(true);

        popup.add(this.trayEntry("Exit Program", i -> {
            try {
                LOGGER.info("Exiting...");
                led.setPaused(true);
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.exit(0);
        }));
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

}
