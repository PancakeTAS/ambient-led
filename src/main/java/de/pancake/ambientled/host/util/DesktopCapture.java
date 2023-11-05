package de.pancake.ambientled.host.util;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;

import static de.pancake.ambientled.host.AmbientLed.LOGGER;

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
    private final WinDef.HDC DESKTOP = USER.GetDC(USER.GetDesktopWindow());
    /** Device context */
    private final WinDef.HDC DC = GDI.CreateCompatibleDC(DESKTOP);

    /** Capture record */
    public record Capture(WinDef.HBITMAP bitmap, WinGDI.BITMAPINFO bitmapInfo, int x, int y, int width, int height, Memory memory) {}

    /**
     * Setup capture record for screen capture
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @return Capture record
     */
    public Capture setupCapture(int x, int y, int width, int height) {
        LOGGER.fine("Setting up capture record for screen capture: " + x + ", " + y + ", " + width + ", " + height);

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
    public Memory screenshot(Capture capture) {
        LOGGER.finest("Taking screenshot of portion of screen: " + capture.x + ", " + capture.y + ", " + capture.width + ", " + capture.height);

        // copy desktop into bitmap
        GDI.SelectObject(DC, capture.bitmap);
        GDI.BitBlt(DC, 0, 0, capture.width, capture.height, DESKTOP, capture.x, capture.y, GDI32.SRCCOPY);
        GDI.GetDIBits(DESKTOP, capture.bitmap, 0, capture.height, capture.memory, capture.bitmapInfo, WinGDI.DIB_RGB_COLORS);
        return capture.memory;
    }

}
