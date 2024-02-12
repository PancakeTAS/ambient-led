package gay.pancake.ambientled.capture;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import gay.pancake.ambientled.AmbientLed;
import gay.pancake.ambientled.ConfigurationManager;
import gay.pancake.ambientled.util.ColorUtil;

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
    public Capture setupCapture(ConfigurationManager.Segment strip, int framerate) {
        AmbientLed.LOGGER.fine("Setting up capture record for screen capture: " + strip.x() + ", " + strip.y() + ", " + strip.width() + ", " + strip.height());

        // create bitmap info
        var bitmapInfo = new WinGDI.BITMAPINFO();
        bitmapInfo.bmiHeader.biWidth = strip.width();
        bitmapInfo.bmiHeader.biHeight = -strip.height();
        bitmapInfo.bmiHeader.biPlanes = 1;
        bitmapInfo.bmiHeader.biBitCount = 32;
        bitmapInfo.bmiHeader.biCompression = WinGDI.BI_RGB;

        // create capture record
        return new Capture(
                strip,
                new Memory((long) strip.width() * strip.height() * 4),
                GDI.CreateCompatibleBitmap(DESKTOP, strip.width(), strip.height()),
                bitmapInfo
        );
    }

    @Override
    public void screenshot(Capture capture) {
        AmbientLed.LOGGER.finest("Taking screenshot of portion of screen: " + capture.strip().x() + ", " + capture.strip().y() + ", " + capture.strip().width() + ", " + capture.strip().height());

        // copy desktop into bitmap
        var bitmap = ((WinDef.HBITMAP) capture.attachment()[0]);
        GDI.SelectObject(DC, bitmap);
        GDI.BitBlt(DC, 0, 0, capture.strip().width(), capture.strip().height(), DESKTOP, capture.strip().x(), capture.strip().y(), GDI32.SRCCOPY);
        GDI.GetDIBits(DESKTOP, bitmap, 0, capture.strip().height(), capture.memory(), (WinGDI.BITMAPINFO) capture.attachment()[1], WinGDI.DIB_RGB_COLORS);
    }

    @Override
    public void free(Capture capture) {
        ((Memory) capture.memory()).close();
        ((WinGDI.BITMAPINFO) capture.attachment()[1]).clear();
        GDI.DeleteObject((WinDef.HBITMAP) capture.attachment()[0]);
    }

    @Override
    public void averages(Capture memory, ColorUtil.Color[] colors) {
        var pixelsPerLed = (memory.strip().orientation() ? memory.strip().width() : memory.strip().height()) / memory.strip().length();
        for (int i = 0; i < memory.strip().length(); i++) {
            ColorUtil.average(
                    memory.memory(), memory.strip().width(),
                    memory.strip().orientation() ? (pixelsPerLed * i) : 0, memory.strip().orientation() ? 0 : (pixelsPerLed * i),
                    memory.strip().orientation() ? pixelsPerLed : memory.strip().width(), memory.strip().orientation() ? memory.strip().height() : pixelsPerLed,
                    memory.strip().steps(), colors[memory.strip().offset() + (memory.strip().invert() ? memory.strip().length() - i - 1 : i)]
            );
        }
    }


}
