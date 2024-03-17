#include "log.h"
#include "configuration.h"

#include <unistd.h>
#include <sys/socket.h>
#include <string.h>

int main() {
    log_info("MAIN", "Starting ambient led");

    // load configuration
    configuration_data* config = configuration_parse();
    if (!config) {
        log_error("MAIN", "Couldn't load configuration");

        return 1;
    }
    log_info("MAIN", "Configuration loaded successfully");

    // main loop
    while (1) {

        // process strips
        for (int i = 0; i < config->num_strips; i++) {
            configuration_strip* strip = config->strips[i];

            // process segments
            for (int j = 0; j < strip->num_segments; j++) {
                configuration_segment* segment = strip->segments[j];

                // capture frame
                capture_grab_frame(&segment->capture);

                // copy buffer
                if (!segment->flip) {
                    memcpy(strip->buffer + (segment->offset * 3), segment->capture.buffer, segment->length * 3);
                } else {
                    for (int k = 0; k < segment->length; k++) {
                        memcpy(strip->buffer + ((segment->offset + segment->length - k - 1) * 3), segment->capture.buffer + (k * 3), 3);
                    }
                }
            }

            // send data to controller
            int status = 0;
            if (strip->type == CONFIGURATION_TYPE_RPI) {
                status = send(strip->fd, strip->buffer, strip->leds * 3, MSG_NOSIGNAL | MSG_DONTWAIT);
            } else if (strip->type == CONFIGURATION_TYPE_ARDUINO) {
                status = write(strip->fd, strip->buffer, strip->leds * 3);
            }

            if (status < 0) {
                log_error("MAIN", "Couldn't send data to controller");

                configuration_free(config);
                return 1;
            }

        }

        // sleep until next frame
        usleep(1000000 / config->fps);
    }

    // free configuration
    configuration_free(config);

    return 0;
}
