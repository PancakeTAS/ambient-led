package gay.pancake.ambientled;

import gay.pancake.ambientled.capture.DesktopCapture;
import gay.pancake.ambientled.updater.LedUpdater;
import gay.pancake.ambientled.util.ColorUtil;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;

import static gay.pancake.ambientled.AmbientLed.LOGGER;
import static gay.pancake.ambientled.capture.DesktopCapture.DC;

@RequiredArgsConstructor
public class LedInstance {

    /** Configuration for led strips */
    private ConfigurationManager.Configuration config;

    /** Executor for led strips */
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /** Updaters for led strips */
    private LedUpdater[] updaters;
    /** Captures for led segments */
    private DesktopCapture.Capture[][] captures;
    /** Colors for led segments */
    private ColorUtil.Color[][] colors;

    /** Has failed */
    private AtomicBoolean failed = new AtomicBoolean(false);

    /**
     * Open the instance
     *
     * @throws IOException If an I/O error occurs
     */
    public LedInstance(ConfigurationManager.Configuration config, Function<ConfigurationManager.Configuration, Boolean> reload) throws IOException {
        this.config = config;

        // setup all strips
        var strips = this.config.strips();
        this.updaters = new LedUpdater[strips.size()];
        this.captures = new DesktopCapture.Capture[strips.size()][];
        this.colors = new ColorUtil.Color[strips.size()][];
        for (int i = 0; i < strips.size(); i++) {
            var strip = strips.get(i);

            // prepare updater
            if (strip.type().equals("pi"))
                this.updaters[i] = LedUpdater.createPiLed(strip.ip(), strip.port(), strip.maxBrightness(), strip.leds(), strip.reductionR(), strip.reductionG(), strip.reductionB(), this.config.lerp(), this.config.ups());
            else if (strip.type().equals("arduino"))
                this.updaters[i] = LedUpdater.createArduinoLed(strip.com(), strip.maxBrightness(), strip.leds(), strip.reductionR(), strip.reductionG(), strip.reductionB(), this.config.lerp(), this.config.ups());
            else
                throw new IOException("Unknown led type: " + strip.type());

            // prepare colors
            this.colors[i] = new ColorUtil.Color[strip.leds()];
            for (int j = 0; j < strip.leds(); j++)
                this.colors[i][j] = new ColorUtil.Color();

            // prepare captures
            this.captures[i] = new DesktopCapture.Capture[strip.segments().size()];
            for (int j = 0; j < strip.segments().size(); j++) {
                var segment = strip.segments().get(j);
                this.captures[i][j] = DC.setupCapture(segment.display(), segment.x(), segment.y(), segment.width(), segment.height(), config.fps());
            }
        }

        // start rendering
        this.executor.scheduleAtFixedRate(() -> {
            try {
                if (this.failed.get())
                    return;
                this.render();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to render", e);
                new Thread(() -> {
                    LOGGER.log(Level.INFO, "Reloading configuration");
                    while (!reload.apply(this.config))
                        Thread.yield();
                }).start();
                this.failed.set(true);
            }
        }, 0, 1000000 / this.config.fps(), TimeUnit.MICROSECONDS);
    }

    /**
     * Render all strips
     *
     * @throws IOException If an I/O error occurs
     */
    public void render() throws IOException {
        for (int stripIndex = 0; stripIndex < this.updaters.length; stripIndex++) {
            var strip = this.config.strips().get(stripIndex);

            // screenshot segments
            for (int segmentIndex = 0; segmentIndex < strip.segments().size(); segmentIndex++) {
                var colors = this.colors[stripIndex];
                var segment = strip.segments().get(segmentIndex);
                var capture = this.captures[stripIndex][segmentIndex];

                DC.screenshot(capture);
                DC.averages(capture, segment.orientation(), segment.invert(), colors, segment.offset(), segment.length(), segment.steps());
            }

            // write colors
            this.updaters[stripIndex].write(this.colors[stripIndex]);
        }
    }

    /**
     * Close the instance
     *
     * @throws IOException If an I/O error occurs
     */
    public void close() throws IOException {
        this.executor.close();

        if (this.captures != null)
            for (var strip : this.captures)
                for (var capture : strip)
                    DC.free(capture);

        if (this.updaters != null)
            for (var updater : this.updaters)
                updater.close();
    }

}
