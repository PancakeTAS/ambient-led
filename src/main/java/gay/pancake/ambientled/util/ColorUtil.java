package gay.pancake.ambientled.util;

import com.sun.jna.Memory;

/**
 * Utility class for math with colors
 * @author Pancake
 */
public class ColorUtil {

    /**
     * Color class
     */
    public static class Color {

        /** Color value */
        public int value = 0x00000000;

        public int getRed() { return (value >> 16) & 0xFF; }
        public int getGreen() { return (value >> 8) & 0xFF; }
        public int getBlue() { return value & 0xFF; }

        public void setRGB(int r, int g, int b) {
            this.value = (0xFF << 24) |
                    ((r & 0xFF) << 16) |
                    ((g & 0xFF) << 8)  |
                    (b & 0xFF);
        }

    }

    /**
     * Calculate average color of buffered image
     * @param image Buffered image
     * @param startX Start x
     * @param startY Start y
     * @param width Width
     * @param height Height
     * @param step Step size
     */
    public static void average(Memory image, int imageWidth, int startX, int startY, int width, int height, int step, Color cc) {
        int total = 0, red_total = 0, green_total = 0, blue_total = 0;

        // iterate through each pixel of the image with the given step size
        for (int x = startX; x < startX + width; x += step) {
            for (int y = startY; y < startY + height; y += step) {
                var c = image.getInt(((long) y*imageWidth+x)*4);
                red_total += (c >> 16) & 0xFF;
                green_total += (c >> 8) & 0xFF;
                blue_total += c & 0xFF;
                total++;
            }
        }

        cc.setRGB(red_total / total, green_total / total, blue_total / total);
    }

    /**
     * Linearly interpolate a color
     * @param r Red
     * @param g Green
     * @param b Blue
     * @param c Color b
     * @param t Interpolation value
     */
    public static void lerp(int r, int g, int b, Color c, double t) {
        c.setRGB(r + (int) ((c.getRed() - r) * t),
                        g + (int) ((c.getGreen() - g) * t),
                        b + (int) ((c.getBlue() - b) * t));
    }

}
