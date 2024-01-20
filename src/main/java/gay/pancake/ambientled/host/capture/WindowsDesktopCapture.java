package gay.pancake.ambientled.host.capture;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import gay.pancake.ambientled.host.AmbientLed;

/**
 * Utility class for capturing the desktop
 *
 * @author Pancake
 */
class WindowsDesktopCapture implements DesktopCapture {

    /** User32 instance */
    private static final User32 USER = User32.INSTANCE;
    /** GDI32 instance */
    private static final GDI32 GDI = GDI32.INSTANCE;

    /** Desktop device context */
    private final WinDef.HDC DESKTOP = USER.GetDC(USER.GetDesktopWindow());
    /** Device context */
    private final WinDef.HDC DC = GDI.CreateCompatibleDC(DESKTOP);

    @Override
    public Capture setupCapture(int screen, int x, int y, int width, int height) {
        AmbientLed.LOGGER.fine("Setting up capture record for screen capture: " + x + ", " + y + ", " + width + ", " + height);

        // create bitmap info
        var bitmapInfo = new WinGDI.BITMAPINFO();
        bitmapInfo.bmiHeader.biWidth = width;
        bitmapInfo.bmiHeader.biHeight = -height;
        bitmapInfo.bmiHeader.biPlanes = 1;
        bitmapInfo.bmiHeader.biBitCount = 32;
        bitmapInfo.bmiHeader.biCompression = WinGDI.BI_RGB;

        // create capture record
        return new Capture(
                x, y, width, height,
                new Memory((long) width * height * 4),
                GDI.CreateCompatibleBitmap(DESKTOP, width, height),
                bitmapInfo
        );
    }

    @Override
    public void screenshot(Capture capture) {
        AmbientLed.LOGGER.finest("Taking screenshot of portion of screen: " + capture.x() + ", " + capture.y() + ", " + capture.width() + ", " + capture.height());

        // copy desktop into bitmap
        var bitmap = ((WinDef.HBITMAP) capture.attachment()[0]);
        GDI.SelectObject(DC, bitmap);
        GDI.BitBlt(DC, 0, 0, capture.width(), capture.height(), DESKTOP, capture.x(), capture.y(), GDI32.SRCCOPY);
        GDI.GetDIBits(DESKTOP, bitmap, 0, capture.height(), capture.memory(), (WinGDI.BITMAPINFO) capture.attachment()[1], WinGDI.DIB_RGB_COLORS);
    }

    @Override
    public void free(Capture capture) {
        capture.memory().close();
        ((WinGDI.BITMAPINFO) capture.attachment()[1]).clear();
        GDI.DeleteObject((WinDef.HBITMAP) capture.attachment()[0]);
    }

}
