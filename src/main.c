#include "log.h"
#include "configuration.h"
#include "arduino.h"
#include "raspberrypi.h"
#include "capture.h"

#include <stdlib.h>

typedef struct {
    configuration_strip* strip; //!< Configuration for the led strip
    int fd; //!< File descriptor of the led strip controller
} strip_instance; //!< Instance of a led strip

typedef struct {
    configuration_segment* segment; //!< Configuration for the segment
    capture_session session; //!< Capture session for the segment
} segment_instance; //!< Instance of a segment

/**
 * Create a Raspberry Pi led strip controller
 *
 * \param strip
 *   Configuration for the led strip
 * \param config
 *   Configuration data
 *
 * \return
 *   File descriptor of the led strip controller or -1 on error
 */
static int create_rpi(configuration_strip* strip, configuration_data* config) {
    union {
        float f;
        char bytes[4];
    } ftb;
    char rpi_header[] = {
        // 4-byte max brightness
        (char) (strip->max_brightness >> 24), (char) (strip->max_brightness >> 16), (char) (strip->max_brightness >> 8), (char) strip->max_brightness,
        // 4-byte LED count
        (char) (strip->leds >> 24), (char) (strip->leds >> 16), (char) (strip->leds >> 8), (char) strip->leds,
        // float multipliers (padding)
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        // 4-byte updates per second
        (char) (config->ups >> 24), (char) (config->ups >> 16), (char) (config->ups >> 8), (char) config->ups,
    };
    // (set multipliers in big endian)
    ftb.f = strip->r_mult;
    rpi_header[8] = ftb.bytes[3];
    rpi_header[9] = ftb.bytes[2];
    rpi_header[10] = ftb.bytes[1];
    rpi_header[11] = ftb.bytes[0];
    ftb.f = strip->g_mult;
    rpi_header[12] = ftb.bytes[3];
    rpi_header[13] = ftb.bytes[2];
    rpi_header[14] = ftb.bytes[1];
    rpi_header[15] = ftb.bytes[0];
    ftb.f = strip->b_mult;
    rpi_header[16] = ftb.bytes[3];
    rpi_header[17] = ftb.bytes[2];
    rpi_header[18] = ftb.bytes[1];
    rpi_header[19] = ftb.bytes[0];
    ftb.f = config->lerp;
    rpi_header[20] = ftb.bytes[3];
    rpi_header[21] = ftb.bytes[2];
    rpi_header[22] = ftb.bytes[1];
    rpi_header[23] = ftb.bytes[0];
    return raspberrypi_open("192.168.178.54", 5163, rpi_header, 28);
}

/**
 * Create an Arduino led strip controller
 *
 * \param strip
 *   Configuration for the led strip
 * \param config
 *   Configuration data
 *
 * \return
 *   File descriptor of the led strip controller or -1 on error
 */
static int create_arduino(configuration_strip* strip, configuration_data* config) {
    char arduino_header[] = {
        // 4-byte max brightness
        (char) (strip->max_brightness >> 24), (char) (strip->max_brightness >> 16), (char) (strip->max_brightness >> 8), (char) strip->max_brightness,
        // 4-byte LED count
        (char) (strip->leds >> 24), (char) (strip->leds >> 16), (char) (strip->leds >> 8), (char) strip->leds,
        // multipliers (fixed point)
        (char) (strip->r_mult * 255.0 - 128.0), (char) (strip->g_mult * 255.0 - 128.0), (char) (strip->b_mult * 255.0 - 128.0), (char) (config->lerp * 255.0 - 128.0),
        // 4-byte updates per second
        (char) (config->ups >> 24), (char) (config->ups >> 16), (char) (config->ups >> 8), (char) config->ups,
    };
    return arduino_open(strip->addr, 981600, arduino_header, 16);
}

int main() {
    log_info("MAIN", "Starting ambient led");

    // load configuration
    configuration_data* config = configuration_parse();
    if (!config) {
        log_error("MAIN", "Couldn't load configuration");

        return 1;
    }
    log_info("MAIN", "Configuration loaded successfully");

    // initialize led controllers
    strip_instance* strips = malloc(sizeof(strip_instance) * config->num_strips);
    for (int i = 0; i < config->num_strips; i++) {
        configuration_strip* strip = config->strips[i];
        strip_instance* instance = &strips[i];

        switch (strip->type) {
            case CONFIGURATION_TYPE_RPI:
                instance->fd = create_rpi(strip, config);
                break;
            case CONFIGURATION_TYPE_ARDUINO:
                instance->fd = create_arduino(strip, config);
                break;
            default:
                log_error("MAIN", "Unknown led strip controller type");

                free(strips);
                configuration_free(config);
                return 1;
        }

        if (instance->fd < 0) {
            log_error("MAIN", "Couldn't initialize led strip controller");

            free(strips);
            configuration_free(config);
            return 1;
        }

        instance->strip = strip;
    }

    return 0;
}
