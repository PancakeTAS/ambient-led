#include "log.h"

#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>

int raspberrypi_open(const char* ip_address, int port, const char* header, int header_len) {
    // open socket
    int raspberrypi = socket(AF_INET, SOCK_STREAM, 0);
    if (raspberrypi < 0) {
        log_trace("RASPBERRYPI", "socket() failed: %s", strerror(errno));

        return -1;
    }

    // configure socket
    setsockopt(raspberrypi, IPPROTO_TCP, TCP_NODELAY, &(int) { 1 }, sizeof(int));

    // connect to server
    struct sockaddr_in server_addr = {
        .sin_family = AF_INET,
        .sin_port = htons(port),
    };
    if (inet_pton(AF_INET, ip_address, &server_addr.sin_addr) <= 0) {
        log_trace("RASPBERRYPI", "inet_pton() failed: %s", strerror(errno));

        close(raspberrypi);
        return -1;
    }
    if (connect(raspberrypi, (struct sockaddr*) &server_addr, sizeof(server_addr)) < 0) {
        log_trace("RASPBERRYPI", "connect() failed: %s", strerror(errno));

        close(raspberrypi);
        return -1;
    }

    // write header
    if (header && send(raspberrypi, header, header_len, 0) < 0) {
        log_trace("RASPBERRYPi", "write() failed: %s", strerror(errno));

        close(raspberrypi);
        return -1;
    }

    log_debug("RASPBERRYPI", "Connected to raspberry pi at %s:%d with fd %d", ip_address, port, raspberrypi);
    return raspberrypi;
}

void raspberrypi_close(int raspberrypi) {
    close(raspberrypi);
    log_debug("RASPBERRYPI", "Closed connection to raspberry pi with fd %d", raspberrypi);
}
