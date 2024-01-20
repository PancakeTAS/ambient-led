package gay.pancake.ambientled.host.capture;

import com.sun.jna.Memory;
import gay.pancake.ambientled.host.AmbientLed;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility class for capturing the desktop
 *
 * @author Pancake
 */
class LinuxDesktopCapture implements DesktopCapture {

    @Override
    public Capture setupCapture(int screen, int x, int y, int width, int height) throws IOException {
        AmbientLed.LOGGER.fine("Setting up capture record for screen capture: " + x + ", " + y + ", " + width + ", " + height);

        // create frame grabber
        var grabber = new FFmpegFrameGrabber(":0." + screen + "+" + x + "," + y);
        grabber.setFormat("x11grab");
        grabber.setImageWidth(width);
        grabber.setImageHeight(height);
        grabber.setPixelFormat(avutil.AV_PIX_FMT_BGRA);
        grabber.start();

        // create capture record
        return new Capture(
                x, y, width, height,
                new Memory((long) width * height * 4),
                grabber,
                new byte[width * height * 4]
        );
    }

    @Override
    public void screenshot(Capture capture) throws IOException {
        AmbientLed.LOGGER.finest("Taking screenshot of portion of screen: " + capture.x() + ", " + capture.y() + ", " + capture.width() + ", " + capture.height());

        // take screenshot
        var frame = ((FFmpegFrameGrabber) capture.attachment()[0]).grabImage();
        var buffer = ((ByteBuffer) frame.image[0]);
        var backingBuffer = (byte[]) capture.attachment()[1];
        buffer.position(0);
        buffer.get(backingBuffer);
        frame.close();

        // copy to memory
        capture.memory().write(0, backingBuffer, 0, backingBuffer.length);
    }

    @Override
    public void free(Capture capture) throws IOException {
        capture.memory().close();
        ((FFmpegFrameGrabber) capture.attachment()[0]).close();
    }
}
