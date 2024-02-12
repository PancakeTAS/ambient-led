package gay.pancake.ambientled.util;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import gay.pancake.capture.NvFBC;
import gay.pancake.capture.structs.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static gay.pancake.capture.enums.NVFBCSTATUS.NVFBC_SUCCESS;
import static gay.pancake.capture.enums.NVFBC_BOOL.NVFBC_FALSE;
import static gay.pancake.capture.enums.NVFBC_BUFFER_FORMAT.NVFBC_BUFFER_FORMAT_RGB;
import static gay.pancake.capture.enums.NVFBC_CAPTURE_TYPE.NVFBC_CAPTURE_TO_SYS;
import static gay.pancake.capture.enums.NVFBC_TOSYS_GRAB_FLAGS.NVFBC_TOSYS_GRAB_FLAGS_NOWAIT;
import static gay.pancake.capture.enums.NVFBC_TRACKING_TYPE.NVFBC_TRACKING_OUTPUT;
import static gay.pancake.capture.enums.NVFBC_TRACKING_TYPE.NVFBC_TRACKING_SCREEN;

/**
 * A class to represent an instance of NvFBC
 */
public class NvFBCInstance extends Thread {

    /** The NvFBC instance */
    private static NVFBC_API_FUNCTION_LIST apiFunctionList = new NVFBC_API_FUNCTION_LIST();

    /**
     * Load the NvFBC library
     */
    private static void ensureLoaded() {
        if (apiFunctionList != null)
            return;

        // create instance
        apiFunctionList = new NVFBC_API_FUNCTION_LIST();
        var err = NvFBC.INSTANCE.NvFBCCreateInstance(apiFunctionList);
        if (err != NVFBC_SUCCESS)
            throw new RuntimeException("Failed to create NvFBC instance: Error " + err);
    }

    /** The display to capture */
    private final String display;
    /** The source capture box */
    private final NVFBC_BOX source;
    /** The destination size */
    private final NVFBC_SIZE destination;
    /** The framerate */
    private final int framerate;

    /** The session handle */
    private final LongByReference sessionHandle = new LongByReference(10L);
    /** The frame pointer */
    private final PointerByReference pFrame = new PointerByReference();

    /** The create handle parameters */
    private final NVFBC_CREATE_HANDLE_PARAMS createHandleParams = new NVFBC_CREATE_HANDLE_PARAMS();
    /** The get status parameters */
    private final NVFBC_GET_STATUS_PARAMS getStatusParams = new NVFBC_GET_STATUS_PARAMS();
    /** The create capture session parameters */
    private final NVFBC_CREATE_CAPTURE_SESSION_PARAMS createCaptureSessionParams = new NVFBC_CREATE_CAPTURE_SESSION_PARAMS();
    /** The capture setup params */
    private final NVFBC_TOSYS_SETUP_PARAMS toSysSetUpParams = new NVFBC_TOSYS_SETUP_PARAMS();

    /** The frame grab params */
    private final NVFBC_TOSYS_GRAB_FRAME_PARAMS toSysGrabFrameParams = new NVFBC_TOSYS_GRAB_FRAME_PARAMS();
    /** The frame info */
    private final NVFBC_FRAME_GRAB_INFO frameGrabInfo = new NVFBC_FRAME_GRAB_INFO();

    /** The initial future */
    public final CompletableFuture<Pointer> buffer = new CompletableFuture<>();
    /** The callback */
    private Consumer<Pointer> callback;

    /**
     * Create a new NvFBC instance
     *
     * @param display The display to capture
     * @param x The x position of the capture box
     * @param y The y position of the capture box
     * @param width The width of the capture box
     * @param height The height of the capture box
     * @param out_width The width of the output
     * @param out_height The height of the output
     * @param framerate The framerate of the capture
     */
    public NvFBCInstance(String display, int x, int y, int width, int height, int out_width, int out_height, int framerate) {
        ensureLoaded();

        this.display = display;
        this.source = new NVFBC_BOX();
        this.source.x = x;
        this.source.y = y;
        this.source.w = width;
        this.source.h = height;
        this.destination = new NVFBC_SIZE();
        this.destination.w = out_width;
        this.destination.h = out_height;
        this.framerate = framerate;
    }

    /**
     * Set a callback to be triggered when a frame is captured
     *
     * @param callback The callback
     */
    public void callback(Consumer<Pointer> callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        // create handle
        var err = NvFBC.INSTANCE.NvFBCCreateHandle(this.sessionHandle, this.createHandleParams);
        if (err != NVFBC_SUCCESS)
            throw new RuntimeException("Failed to create NvFBC handle: Error " + err);

        // get status
        err = NvFBC.INSTANCE.NvFBCGetStatus(this.sessionHandle.getValue(), this.getStatusParams);
        if (err != NVFBC_SUCCESS)
            throw new RuntimeException("Failed to get NvFBC status: Error " + err);

        // find the output
        var dwOutputId = -1;
        if (this.display != null) {
            for (var output : this.getStatusParams.outputs) {
                if (new String(output.name).startsWith(this.display)) {
                    dwOutputId = output.dwId;
                    break;
                }
            }
        }

        // create capture session
        this.createCaptureSessionParams.eCaptureType = NVFBC_CAPTURE_TO_SYS;
        this.createCaptureSessionParams.bWithCursor = NVFBC_FALSE;
        this.createCaptureSessionParams.captureBox = this.source;
        this.createCaptureSessionParams.frameSize = this.destination;
        this.createCaptureSessionParams.eTrackingType = this.display != null ? NVFBC_TRACKING_OUTPUT : NVFBC_TRACKING_SCREEN;
        this.createCaptureSessionParams.dwOutputId = dwOutputId;
        this.createCaptureSessionParams.dwSamplingRateMs = 1000 / this.framerate;

        err = NvFBC.INSTANCE.NvFBCCreateCaptureSession(this.sessionHandle.getValue(), this.createCaptureSessionParams);
        if (err != NVFBC_SUCCESS)
            throw new RuntimeException("Failed to create capture session: Error " + err);

        // setup capture session
        this.toSysSetUpParams.eBufferFormat = NVFBC_BUFFER_FORMAT_RGB;
        this.toSysSetUpParams.ppBuffer = pFrame;
        this.toSysSetUpParams.bWithDiffMap = NVFBC_FALSE;

        err = NvFBC.INSTANCE.NvFBCToSysSetUp(this.sessionHandle.getValue(), this.toSysSetUpParams);
        if (err != NVFBC_SUCCESS)
            throw new RuntimeException("Failed to setup capture session: Error " + err);

        // complete future
        this.buffer.complete(this.pFrame.getValue());

        // start capturing
        while (true) {

            // grab frame
            this.toSysGrabFrameParams.dwFlags = NVFBC_TOSYS_GRAB_FLAGS_NOWAIT;
            this.toSysGrabFrameParams.pFrameGrabInfo = this.frameGrabInfo;

            err = NvFBC.INSTANCE.NvFBCToSysGrabFrame(this.sessionHandle.getValue(), this.toSysGrabFrameParams);
            if (err != NVFBC_SUCCESS)
                throw new RuntimeException("Failed to grab frame: Error " + err);

            // trigger callback
            if (this.callback != null) this.callback.accept(this.pFrame.getValue());

        }
    }

}
