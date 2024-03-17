/// \file configuration.h This file parses the config.json

#pragma once

#include <stdbool.h>
#include "capture.h"

typedef enum {
    CONFIGURATION_ORIENTATION_VERTICAL, //!< Vertical orientation
    CONFIGURATION_ORIENTATION_HORIZONTAL //!< Horizontal orientation
} configuration_orientation; //!< Orientation of an led strip segment

typedef struct {
    int offset; //!< Led offset for this segment of the strip
    int length; //!< Length of this segment of the strip
    char* display; //!< Display name for this segment
    int x; //!< X coordinate of capture area
    int y; //!< Y coordinate of capture area
    int width; //!< Width of the capture area
    int height; //!< Height of the capture area
    configuration_orientation orientation; //!< Orientation of this segment in the strip
    bool flip; //!< Flip the direction of this segment
    capture_session capture; //!< Capture session for this segment
} configuration_segment; //!< Configuration for a segment of the led strip

typedef enum {
    CONFIGURATION_TYPE_RPI, //!< Raspberry Pi
    CONFIGURATION_TYPE_ARDUINO //!< Arduino
} configuration_type; //!< Type of led strip controller

typedef struct {
    configuration_type type; //!< Type of led strip controller
    char* addr; //!< COM port or ip of the controller
    int port; //!< Port of the controller
    int leds; //!< Number of leds in the strip
    configuration_segment** segments; //!< Array of segments in the led strip
    int num_segments; //!< Number of segments in the led strip
    int max_brightness; //!< Maximum brightness of the led strip
    float r_mult; //!< Red multiplier
    float g_mult; //!< Green multiplier
    float b_mult; //!< Blue multiplier
    int fd; //!< File descriptor for the controller
} configuration_strip; //!< Configuration for an led strip

typedef struct {
    configuration_strip** strips; //!< Array of led strips
    int num_strips; //!< Number of led strips
    int ups; //!< Updates per second
    int fps; //!< Frames per second
    float lerp; //!< Lerp factor
} configuration_data; //!< Configuration data

/**
 * Parse ambient led configuration
 *
 * \return
 *   Configuration data or NULL on error
 */
configuration_data* configuration_parse();

/**
 * Free configuration data
 *
 * \param config
 *   Configuration data to free
 */
void configuration_free(configuration_data* data);
