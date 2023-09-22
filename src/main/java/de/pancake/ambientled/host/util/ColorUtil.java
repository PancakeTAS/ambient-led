package de.pancake.ambientled.host.util;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility class for math with colors
 * @author Pancake
 */
public class ColorUtil {

    /**
     * Calculate average color of buffered image
     * @param image Buffered image
     * @param startX Start x
     * @param startY Start y
     * @param width Width
     * @param height Height
     * @param step Step size
     * @param reduceGreen Reduce brightness of the green
     * @param reduceBlue Reduce brightness of the blue
     * @param reduceBrightness Adjust max brightness to 355
     * @return Average color
     */
    public static Color average(BufferedImage image, int startX, int startY, int width, int height, int step, boolean reduceGreen, boolean reduceBlue, boolean reduceBrightness) {
        int total = (width * height) / (step * step);

        int red_total = 0, green_total = 0, blue_total = 0;

        // iterate through each pixel of the image with the given step size
        for (int x = startX; x < startX + width; x += step) {
            for (int y = startY; y < startY + height; y += step) {
                var c = new Color(image.getRGB(x, y));
                red_total += c.getRed();
                green_total += c.getGreen();
                blue_total += c.getBlue();
            }
        }

        // calculate average color of the image
        var red = red_total / total;
        var green = green_total / total;
        var blue = blue_total / total;

        // reduce brightness of the green (because green wall)
        if (reduceGreen)
            green = (int) (green * 0.75);

        // reduce brightness of the blue (because stupid led strip)
        if (reduceBlue)
            blue = (int) (blue * 0.75);

        // reduce overall brightness (because arduino)
        if (red + green + blue > 355 && reduceBrightness) {
            red /= 4;
            green /= 4;
            blue /= 4;
        }

        return new Color(red, green, blue);
    }

    /**
     * Calculate average color of buffered image
     * @param a Color a
     * @param b Color b
     * @param t Interpolation value
     * @return Interpolated color
     */
    public static Color lerp(Color a, Color b, double t) {
        return new Color(
                (int) (b.getRed() + (a.getRed() - b.getRed()) * t),
                (int) (b.getGreen() + (a.getGreen() - b.getGreen()) * t),
                (int) (b.getBlue() + (a.getBlue() - b.getBlue()) * t)
        );
    }

}
