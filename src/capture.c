#include "capture.h"
#include "log.h"

#include <NvFBC.h>
#include <string.h>

static NVFBC_API_FUNCTION_LIST fbc = { .dwVersion = 0 }; //!< NvFBC API function list

/**
 * Ensure NvFBC is loaded.
 *
 * \return
 *   0 on success, 1 on failure
 */
static int ensure_loaded() {
    if (fbc.dwVersion)
        return 0;

    // load NvFBC
    fbc.dwVersion = NVFBC_VERSION;
    NVFBCSTATUS status = NvFBCCreateInstance(&fbc);
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "NvFBCCreateInstance() failed: %d", status);

        return 1;
    }

    return 0;
}

int capture_create_session(capture_session* session) {
    // ensure NvFBC is loaded
    if (ensure_loaded())
        return 1;

    // create session handle
    NVFBCSTATUS status = fbc.nvFBCCreateHandle(&session->nvfbc_handle, &(NVFBC_CREATE_HANDLE_PARAMS) { .dwVersion = NVFBC_CREATE_HANDLE_PARAMS_VER });
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "nvFBCCreateHandle() failed: %d", status);

        return 1;
    }

    // get session status
    NVFBC_GET_STATUS_PARAMS params = {
        .dwVersion = NVFBC_GET_STATUS_PARAMS_VER
    };
    status = fbc.nvFBCGetStatus(session->nvfbc_handle, &params);
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "nvFBCGetStatus() failed: %s (%d)", fbc.nvFBCGetLastErrorStr(session->nvfbc_handle), status);

        fbc.nvFBCDestroyHandle(session->nvfbc_handle, &(NVFBC_DESTROY_HANDLE_PARAMS) { .dwVersion = NVFBC_DESTROY_HANDLE_PARAMS_VER });
        return 1;
    }

    // find display id
    uint32_t display_id = 0;
    for (uint32_t i = 0; i < params.dwOutputNum; i++) {
        if (strstr(params.outputs[i].name, session->display)) {
            display_id = params.outputs[i].dwId;
            break;
        }
    }

    // create capture session
    status = fbc.nvFBCCreateCaptureSession(session->nvfbc_handle, &(NVFBC_CREATE_CAPTURE_SESSION_PARAMS) {
        .dwVersion = NVFBC_CREATE_CAPTURE_SESSION_PARAMS_VER,
        .eCaptureType = NVFBC_CAPTURE_TO_SYS,
        .captureBox = (NVFBC_BOX) {
            .x = session->area.x,
            .y = session->area.y,
            .w = session->area.width,
            .h = session->area.height
        },
        .frameSize = (NVFBC_SIZE) {
            .w = session->size.width,
            .h = session->size.height
        },
        .eTrackingType = NVFBC_TRACKING_OUTPUT,
        .dwOutputId = display_id,
        .dwSamplingRateMs = 1000 / session->framerate
    });
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "nvFBCCreateCaptureSession() failed: %s (%d)", fbc.nvFBCGetLastErrorStr(session->nvfbc_handle), status);

        fbc.nvFBCDestroyHandle(session->nvfbc_handle, &(NVFBC_DESTROY_HANDLE_PARAMS) { .dwVersion = NVFBC_DESTROY_HANDLE_PARAMS_VER });
        return 1;
    }

    // setup system memory buffer
    NVFBC_TOSYS_SETUP_PARAMS setup_params = {
        .dwVersion = NVFBC_TOSYS_SETUP_PARAMS_VER,
        .eBufferFormat = NVFBC_BUFFER_FORMAT_RGB,
        .ppBuffer = (void**) &session->buffer,
        .bWithDiffMap = NVFBC_FALSE
    };
    status = fbc.nvFBCToSysSetUp(session->nvfbc_handle, &setup_params);
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "nvFBCToSysSetup() failed: %s (%d)", fbc.nvFBCGetLastErrorStr(session->nvfbc_handle), status);

        fbc.nvFBCDestroyCaptureSession(session->nvfbc_handle, &(NVFBC_DESTROY_CAPTURE_SESSION_PARAMS) { .dwVersion = NVFBC_DESTROY_CAPTURE_SESSION_PARAMS_VER });
        fbc.nvFBCDestroyHandle(session->nvfbc_handle, &(NVFBC_DESTROY_HANDLE_PARAMS) { .dwVersion = NVFBC_DESTROY_HANDLE_PARAMS_VER });
        return 1;
    }

    // unbind context
    status = fbc.nvFBCReleaseContext(session->nvfbc_handle, &(NVFBC_RELEASE_CONTEXT_PARAMS) { .dwVersion = NVFBC_RELEASE_CONTEXT_PARAMS_VER });
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "nvFBCReleaseContext() failed: %s (%d)", fbc.nvFBCGetLastErrorStr(session->nvfbc_handle), status);

        return 1;
    }

    log_debug("NVFBC", "Created capture session for display %s with dimensions %d, %d, %dx%d", session->display, session->area.x, session->area.y, session->area.width, session->area.height);
    return 0;
}

int capture_grab_frame(const capture_session* session) {
    // bind context
    NVFBCSTATUS status = fbc.nvFBCBindContext(session->nvfbc_handle, &(NVFBC_BIND_CONTEXT_PARAMS) { .dwVersion = NVFBC_BIND_CONTEXT_PARAMS_VER });
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "nvFBCBindContext() failed: %s (%d)", fbc.nvFBCGetLastErrorStr(session->nvfbc_handle), status);

        return 1;
    }

    // grab frame
    status = fbc.nvFBCToSysGrabFrame(session->nvfbc_handle, &(NVFBC_TOSYS_GRAB_FRAME_PARAMS) {
        .dwVersion = NVFBC_TOSYS_GRAB_FRAME_PARAMS_VER,
        .dwFlags = NVFBC_TOSYS_GRAB_FLAGS_NOWAIT
    });
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "nvFBCToSysGrabFrame() failed: %s (%d)", fbc.nvFBCGetLastErrorStr(session->nvfbc_handle), status);

        return 1;
    }

    // unbind context
    status = fbc.nvFBCReleaseContext(session->nvfbc_handle, &(NVFBC_RELEASE_CONTEXT_PARAMS) { .dwVersion = NVFBC_RELEASE_CONTEXT_PARAMS_VER });
    if (status != NVFBC_SUCCESS) {
        log_trace("NVFBC", "nvFBCReleaseContext() failed: %s (%d)", fbc.nvFBCGetLastErrorStr(session->nvfbc_handle), status);

        return 1;
    }

    //log_debug("NVFBC", "Grabbed frame from capture session for display %s with dimensions %d, %d, %dx%d", session->display, session->area.x, session->area.y, session->area.width, session->area.height);
    return 0;
}

void capture_destroy_session(const capture_session* session) {
    fbc.nvFBCDestroyCaptureSession(session->nvfbc_handle, &(NVFBC_DESTROY_CAPTURE_SESSION_PARAMS) { .dwVersion = NVFBC_DESTROY_CAPTURE_SESSION_PARAMS_VER });
    fbc.nvFBCDestroyHandle(session->nvfbc_handle, &(NVFBC_DESTROY_HANDLE_PARAMS) { .dwVersion = NVFBC_DESTROY_HANDLE_PARAMS_VER });
    log_debug("NVFBC", "Destroyed capture session for display %s with dimensions %d, %d, %dx%d", session->display, session->area.x, session->area.y, session->area.width, session->area.height);
}
