#include "log.h"
#include "configuration.h"

#include <unistd.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <string.h>
#include <pthread.h>

void* capture_loop(void* data) {
    configuration_data* config = (configuration_data*) data;

    while (1) {
        // process strips
        for (int i = 0; i < config->num_strips; i++) {
            configuration_strip* strip = config->strips[i];

            // process segments
            for (int j = 0; j < strip->num_segments; j++) {
                configuration_segment* segment = strip->segments[j];

                // grab frame
                if (capture_grab_frame(&segment->capture)) {
                    log_error("CAPTURE", "Couldn't grab frame");

                    exit(1);
                }

                // copy buffer
                if (!segment->flip) {
                    memcpy(strip->buffer + (segment->offset * 3), segment->capture.buffer, segment->length * 3);
                } else {
                    for (int k = 0; k < segment->length; k++) {
                        memcpy(strip->buffer + ((segment->offset + segment->length - k - 1) * 3), segment->capture.buffer + (k * 3), 3);
                    }
                }
            }
        }

        usleep(1000000 / config->fps);
    }

    return NULL;
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

    // capture thread
    pthread_t capture_thread;
    if (pthread_create(&capture_thread, NULL, (void* (*)(void*)) capture_loop, config)) {
        log_error("MAIN", "Couldn't create capture thread");

        configuration_free(config);
        return 1;
    }
    log_info("MAIN", "Capture thread created successfully");

    // main loop
    while (1) {

        // process strips
        for (int i = 0; i < config->num_strips; i++) {
            configuration_strip* strip = config->strips[i];

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
                exit(1);
            }

        }

        // sleep until next frame
        usleep(1000000 / config->fps);
    }
}
