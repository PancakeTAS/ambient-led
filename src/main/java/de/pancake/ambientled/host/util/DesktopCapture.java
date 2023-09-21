package de.pancake.ambientled.host.util;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;

/**
 * Utility class for capturing the desktop
 * @author Pancake
 */
public class DesktopCapture {

    /** User32 instance */
    private static final User32 USER = User32.INSTANCE;
    /** GDI32 instance */
    private static final GDI32 GDI = GDI32.INSTANCE;
    /** Desktop device context */
    private static final WinDef.HDC DESKTOP = USER.GetDC(USER.GetDesktopWindow());
    /** Device context */
    private static final WinDef.HDC DC = GDI.CreateCompatibleDC(DESKTOP);
    /** Color model */
    private static final DirectColorModel COLOR_MODEL = new DirectColorModel(24, 0x00FF0000, 0xFF00, 0xFF);
    /** Color model mask */
    private static final int[] COLOR_MODEL_MASK = new int[] { COLOR_MODEL.getRedMask(), COLOR_MODEL.getGreenMask(), COLOR_MODEL.getBlueMask() };

    /** Capture record */
    public static record Capture(WinDef.HBITMAP bitmap, WinGDI.BITMAPINFO bitmapInfo, int x, int y, int width, int height, Memory memory) {}

    /**
     * Setup capture record for screen capture
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @return Capture record
     */
    public static Capture setupCapture(int x, int y, int width, int height) {
        // create bitmap info
        var bitmapInfo = new WinGDI.BITMAPINFO();
        bitmapInfo.bmiHeader.biWidth = width;
        bitmapInfo.bmiHeader.biHeight = -height;
        bitmapInfo.bmiHeader.biPlanes = 1;
        bitmapInfo.bmiHeader.biBitCount = 32;
        bitmapInfo.bmiHeader.biCompression = WinGDI.BI_RGB;

        // create capture record
        return new Capture(
                GDI.CreateCompatibleBitmap(DESKTOP, width, height),
                bitmapInfo,
                x, y, width, height,
                new Memory((long) width * height * 4)
        );
    }

    /**
     * Take screenshot of portion of screen
     * @param capture Capture record
     * @return Screenshot
     */
    public static BufferedImage screenshot(Capture capture) {
        // copy desktop into bitmap
        GDI.SelectObject(DC, capture.bitmap);
        GDI.BitBlt(DC, 0, 0, capture.width, capture.height, DESKTOP, capture.x, capture.y, GDI32.SRCCOPY);
        GDI.GetDIBits(DESKTOP, capture.bitmap, 0, capture.height, capture.memory, capture.bitmapInfo, WinGDI.DIB_RGB_COLORS);

        // load buffer into image
        return new BufferedImage(
                COLOR_MODEL,
                Raster.createPackedRaster(
                        new DataBufferInt(
                                capture.memory.getIntArray(0, capture.width * capture.height),
                                capture.width * capture.height
                        ), capture.width, capture.height, capture.width, COLOR_MODEL_MASK, null
                ), false, null
        );
    }

    /**
     * Cleanup allocated resources from capture
     * @param capture Capture record
     */
    public static void cleanupCapture(Capture capture) {
        capture.memory.close();
        capture.bitmapInfo.clear();
        GDI.DeleteObject(capture.bitmap);
    }

}
