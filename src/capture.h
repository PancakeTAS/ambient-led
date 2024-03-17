/// \file capture.h This file contains methods for capturing the screen using nvfbc.

#pragma once

#include <stdint.h>

typedef struct {
    uint32_t x; //!< [in] x-coordinate of the top-left corner
    uint32_t y; //!< [in] y-coordinate of the top-left corner
    uint32_t width; //!< [in] width of the captured area
    uint32_t height; //!< [in] height of the captured area
} capture_area; //!< Area to capture

typedef struct {
    uint32_t width;  //!< [in] width of the output buffer
    uint32_t height; //!< [in] height of the output buffer
} buffer_size; //!< Area to output

typedef struct {
    char* display; //!< [in] Display to capture
    int framerate; //!< [in] Capture framerate
    capture_area area; //!< [in] Area to capture
    buffer_size size; //!< [in] Area to output
    char* buffer; //!< [out] Output buffer

    uint64_t nvfbc_handle; //!< [out] Capture session handle
} capture_session; //!< Capture session

/**
 * Create a capture session.
 *
 * \param session
 *   Capture session to create
 * \return
 *   0 on success, 1 on failure
 */
int capture_create_session(capture_session* session);

/**
 * Grab a frame.
 *
 * \param session
 *   Capture session to grab a frame from
 * \return
 *   0 on success, 1 on failure
 */
int capture_grab_frame(const capture_session* session);

/**
 * Destroy a capture session.
 *
 * \param session
 *   Capture session to destroy
 */
void capture_destroy_session(const capture_session* session);
