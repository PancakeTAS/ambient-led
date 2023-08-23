package de.pancake.backgroundled;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.TimerTask;

import static de.pancake.backgroundled.Main.COLORS;
import static de.pancake.backgroundled.Main.paused;

/**
 * Screen grabber class
 * @author Pancake
 */
public class ScreenGrabber extends TimerTask {

    // Scaled screen size
    private static final int WIDTH = (int) (3840 / 1.5);
    private static final int HEIGHT = (int) (2160 / 1.5);

    // Amount of LEDs on each side
    private static final int LEDS_SIDE = 55;
    private static final int LEDS_TOP = 75;

    // Size of each LED in pixels
    private static final int HEIGHT_PER_LED = HEIGHT / LEDS_SIDE;
    private static final int WIDTH_PER_LED = WIDTH / LEDS_TOP;

    // Robot instance
    private Robot robot;

    /**
     * Initialize screen grabber
     */
    public ScreenGrabber() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Grab screen and calculate average color for each led
     */
    @Override
    public void run() {
        if (paused)
            return;

        // capture left side of screen and calculate average color for each led
        var img = (BufferedImage) robot.createMultiResolutionScreenCapture(new Rectangle(1920+1920, 0, 200, HEIGHT)).getResolutionVariant(200, HEIGHT);
        for (int i = 0; i < LEDS_SIDE; i++)
            this.calculateAverageColor(i, img.getSubimage(0, HEIGHT_PER_LED * (LEDS_SIDE - i - 1), 200, HEIGHT_PER_LED - 1));

        // capture top of screen...
        img = (BufferedImage) robot.createMultiResolutionScreenCapture(new Rectangle(1920+1920, 0, WIDTH, 120)).getResolutionVariant(WIDTH, 120);
        for (int i = 0; i < LEDS_TOP; i++)
            this.calculateAverageColor(LEDS_SIDE + i, img.getSubimage(WIDTH_PER_LED * i, 0, WIDTH_PER_LED - 1, 120));

        // capture right side of screen...
        img = (BufferedImage) robot.createMultiResolutionScreenCapture(new Rectangle(1920+1920 + WIDTH - 200, 0, 200, HEIGHT)).getResolutionVariant(200, HEIGHT);
        for (int i = 0; i < LEDS_SIDE - 5; i++)
            this.calculateAverageColor(LEDS_SIDE + LEDS_TOP + i, img.getSubimage(0, HEIGHT_PER_LED * i, 200, HEIGHT_PER_LED - 1));
    }

    /**
     * Calculate average color of the buffered image
     * @param index Index of the led
     * @param image Image to calculate average color of
     */
    private void calculateAverageColor(int index, BufferedImage image) {
        int totalRed = 0, totalGreen = 0, totalBlue = 0;
        int totalPixels = image.getHeight() * image.getWidth();

        // iterate through each pixel of the image
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                var pixelColor = new Color(image.getRGB(x, y));
                totalRed += pixelColor.getRed();
                totalGreen += pixelColor.getGreen();
                totalBlue += pixelColor.getBlue();
            }
        }

        // calculate average color of the image
        int avgRed = totalRed / totalPixels;
        int avgGreen = totalGreen / totalPixels;
        int avgBlue = totalBlue / totalPixels;

        // reduce brightness of the green (because green wall)
        avgGreen = (int) (avgGreen * 0.75);

        // max brightness is 255, so if the average is higher than 355, reduce it. Arduino can't handle more than 355
        if (avgRed + avgGreen + avgBlue > 355) {
            avgRed /= 4;
            avgGreen /= 4;
            avgBlue /= 4;
        }

        // set color of the led
        COLORS[index][0] = avgRed;
        COLORS[index][1] = avgGreen;
        COLORS[index][2] = avgBlue;
    }

}
