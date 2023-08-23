package de.pancake.backgroundled;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinGDI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.util.TimerTask;

import static de.pancake.backgroundled.Main.COLORS;
import static de.pancake.backgroundled.Main.paused;

/**
 * Screen grabber class
 * @author Pancake
 */
public class ScreenGrabber extends TimerTask {

    // Scaled screen size
    private static final int WIDTH = 3840;
    private static final int HEIGHT = 2160;

    // Amount of LEDs on each side
    private static final int LEDS_SIDE = 55;
    private static final int LEDS_TOP = 75;

    // Size of each LED in pixels
    private static final int HEIGHT_PER_LED = HEIGHT / LEDS_SIDE;
    private static final int WIDTH_PER_LED = WIDTH / LEDS_TOP;

    /**
     * Grab screen and calculate average color for each led
     */
    @Override
    public void run() {
        if (paused)
            return;

        // capture left side of screen and calculate average color for each led
        var img = this.screenshot(3840, 0, 300, HEIGHT);
        for (int i = 0; i < LEDS_SIDE; i++)
            this.calculateAverageColor(i, img.getSubimage(0, HEIGHT_PER_LED * (LEDS_SIDE - i - 1), 300, HEIGHT_PER_LED - 1));

        // capture top of screen...
        img = this.screenshot(1920+1920, 0, WIDTH, 180);
        for (int i = 0; i < LEDS_TOP; i++)
            this.calculateAverageColor(LEDS_SIDE + i, img.getSubimage(WIDTH_PER_LED * i, 0, WIDTH_PER_LED - 1, 180));

        // capture right side of screen...
        img = this.screenshot(1920+1920 + WIDTH - 300, 0, 300, HEIGHT);
        for (int i = 0; i < LEDS_SIDE - 5; i++)
            this.calculateAverageColor(LEDS_SIDE + LEDS_TOP + i, img.getSubimage(0, HEIGHT_PER_LED * i, 300, HEIGHT_PER_LED - 1));
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

    /**
     * Takes a screenshot of a portion of the screen
     * @param x x coordinate
     * @param y y coordinate
     * @param width Width
     * @param height Height
     * @return Image
     */
    private BufferedImage screenshot(int x, int y, int width, int height) {
        var user = User32.INSTANCE;
        var gdi = GDI32.INSTANCE;

        // create bitmap stuff
        var desktop = user.GetDC(user.GetDesktopWindow()); // get desktop window
        var dc = gdi.CreateCompatibleDC(desktop); // get device context
        var bitmap = gdi.CreateCompatibleBitmap(desktop, width, height); // create bitmap
        var prevObject = gdi.SelectObject(dc, bitmap); // select object (and store previous one for restoring later)

        // copy desktop into bitmap
        gdi.BitBlt(dc, 0, 0, width, height, desktop, x, y, GDI32.SRCCOPY);

        // create bitmap info
        var bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height; // wtf
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        // load bitmap into buffer
        var buffer = new Memory((long) width * height * 4);
        gdi.GetDIBits(desktop, bitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

        // load buffer into image
        int bufferSize = width * height;
        var dataBuffer = new DataBufferInt(buffer.getIntArray(0, bufferSize), bufferSize);
        var colorModel = new DirectColorModel(24, 0x00FF0000, 0xFF00, 0xFF);
        var raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask() }, null);
        var image = new BufferedImage(colorModel, raster, false, null);

        // free resources
        gdi.SelectObject(dc, prevObject); // restore previous object
        gdi.DeleteObject(bitmap);
        gdi.DeleteDC(dc);
        user.ReleaseDC(user.GetDesktopWindow(), desktop);

        return image;
    }

}
