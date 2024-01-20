package gay.pancake.ambientled.host;

import gay.pancake.ambientled.host.capture.DesktopCapture;
import gay.pancake.ambientled.host.updater.LedUpdater;
import gay.pancake.ambientled.host.util.ColorUtil;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static gay.pancake.ambientled.host.AmbientLed.LOGGER;
import static gay.pancake.ambientled.host.capture.DesktopCapture.DC;

@RequiredArgsConstructor
public class LedInstance {

    /** Configuration for led strips */
    private final ConfigurationManager.Configuration[] strips;

    /** Executor for led strips */
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    /** Updaters for led strips */
    private LedUpdater[] updaters;
    /** Captures for led segments */
    private DesktopCapture.Capture[][] captures;
    /** Colors for led segments */
    private ColorUtil.Color[][] colors;
    /** Final colors for led segments */
    private ColorUtil.Color[][] final_colors;

    /**
     * Open the instance
     *
     * @throws IOException If an I/O error occurs
     */
    public void open() throws IOException {
        this.updaters = new LedUpdater[this.strips.length];
        this.captures = new DesktopCapture.Capture[this.strips.length][];
        this.colors = new ColorUtil.Color[this.strips.length][];
        this.final_colors = new ColorUtil.Color[this.strips.length][];
        for (int i = 0; i < this.strips.length; i++) {
            var strip = this.strips[i];

            if (strip.type().equals("pi"))
                this.updaters[i] = LedUpdater.createPiLed(strip.ip(), strip.port(), strip.leds());
            else if (strip.type().equals("arduino"))
                this.updaters[i] = LedUpdater.createArduinoLed(strip.com(), strip.leds());
            else
                throw new IOException("Unknown led type: " + strip.type());

            this.colors[i] = new ColorUtil.Color[strip.leds()];
            this.final_colors[i] = new ColorUtil.Color[strip.leds()];
            for (int j = 0; j < strip.leds(); j++) {
                this.colors[i][j] = new ColorUtil.Color();
                this.final_colors[i][j] = new ColorUtil.Color();
            }

            var index = i;
            this.executor.scheduleAtFixedRate(() -> this.update(index), 1000000, 1000000 / strip.ups(), TimeUnit.MICROSECONDS);

            this.captures[i] = new DesktopCapture.Capture[strip.segments().size()];
            for (int j = 0; j < strip.segments().size(); j++) {
                var segment = strip.segments().get(j);

                this.captures[i][j] = DC.setupCapture(segment.display(), segment.x(), segment.y(), segment.width(), segment.height());

                var index1 = j;
                this.executor.scheduleAtFixedRate(() -> this.render(index, index1), 1000000, 1000000 / segment.fps(), TimeUnit.MICROSECONDS);
            }
        }
    }

    /**
     * Render a segment
     *
     * @param stripIndex Strip index
     * @param segmentIndex Segment index
     */
    public void render(int stripIndex, int segmentIndex) {
        var colors = this.colors[stripIndex];
        var strip = this.strips[stripIndex];
        var capture = this.captures[stripIndex][segmentIndex];
        var segment = strip.segments().get(segmentIndex);

        try {
            DC.screenshot(capture);
            DC.averages(capture, segment.orientation(), segment.invert(), colors, segment.offset(), segment.length(), segment.steps());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to render segment " + segmentIndex + " of strip " + stripIndex, e);
        }
    }


    /**
     * Update a strip
     *
     * @param stripIndex Strip index
     */
    public void update(int stripIndex) {
        var colors = this.colors[stripIndex];
        var final_colors = this.final_colors[stripIndex];
        var updater = this.updaters[stripIndex];
        var strip = this.strips[stripIndex];

        // lerp and update colors
        var max = 0;
        for (int i = 0; i < colors.length; i++) {
            ColorUtil.lerp(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), final_colors[i], .5);
            max += final_colors[i].getRed() + final_colors[i].getGreen() + final_colors[i].getBlue();
        }

        // reduce max brightness
        max = (int) (max / (double) final_colors.length);
        var reduction = Math.min(1, strip.maxBrightness() / Math.max(1.0f, max));

        try {
            updater.reduction(reduction * strip.reductionR(), reduction * strip.reductionG(), reduction * strip.reductionB());
            updater.write(final_colors, 0, final_colors.length);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update strip " + stripIndex, e);
        }
    }

    /**
     * Close the instance
     *
     * @throws IOException If an I/O error occurs
     */
    public void close() throws IOException {
        this.executor.close();

        if (this.updaters != null)
            for (var updater : this.updaters)
                updater.close();

        if (this.captures != null)
            for (var strip : this.captures)
                for (var capture : strip)
                    DC.free(capture);
    }

}
