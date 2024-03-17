#include "arduino.h"
#include "log.h"

#include <string.h>

#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <asm/termbits.h>

int arduino_open(const char* port_name, int baud_rate, const char* header, int header_len) {
    // open serial port
    int arduino = open(port_name, O_RDWR | O_NOCTTY | O_NDELAY);
    if (arduino < 0) {
        log_trace("SERIAL", "fopen() failed: %s", strerror(errno));
        return -1;
    }

    // configure serial port
    struct termios2 config = {
        .c_iflag = 0,
        .c_oflag = 0,
        .c_cflag = CS8 | CREAD | CLOCAL | BOTHER,
        .c_lflag = 0,
        .c_line = 0,
        .c_cc = {
            [VMIN] = 0,
            [VTIME] = 0,
        },
        .c_ispeed = baud_rate,
        .c_ospeed = baud_rate,
    };
    if (ioctl(arduino, TCSETS2, &config) < 0) {
        log_trace("SERIAL", "ioctl() failed: %s", strerror(errno));
        close(arduino);
        return -1;
    }

    if (header) {
        // write header
        if (write(arduino, header, header_len) < 0) {
            log_trace("SERIAL", "write() failed: %s", strerror(errno));

            close(arduino);
            return -1;
        }

        // wait until byte is received
        int retries = 10;
        while (1) {
            char c = 0;
            if (read(arduino, &c, 1) < 0) {
                log_trace("SERIAL", "read() failed: %s", strerror(errno));

                close(arduino);
                return -1;
            }

            if (c == 1)
                break;
            else if (c >= 2) {
                log_trace("SERIAL", "read() failed: Invalid byte");

                close(arduino);
                return -1;
            }

            usleep(100000);

            if (--retries == 0) {
                log_trace("SERIAL", "read() failed: Timeout");

                close(arduino);
                return -1;
            }
        }
    }

    log_debug("SERIAL", "Connected to arduino on port %s with fd %d", port_name, arduino);
    return arduino;
}

void arduino_close(int fd) {
    close(fd);
    log_debug("SERIAL", "Closed connection to arduino with fd %d", fd);
}
