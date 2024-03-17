/// \file raspberrypi.h This file contains necessary methods to communicate with a raspberry pi

#pragma once

/**
 * Open serial port to raspberry pi
 *
 * \param ip_address
 *   IP address of the raspberry pi
 * \param port
 *   Port number to connect to
 * \param header
 *   Optional header to send to raspberry pi
 * \param header_len
 *   Size of the header
 *
 * \return
 *   File descriptor of the opened socket or -1 if an error occurred
*/
int raspberrypi_open(const char* ip_address, int port, const char* header, int header_len);

/**
 * Close serial port to raspberry pi
 *
 * \param fd
 *   File descriptor of the socket to close
*/
void raspberrypi_close(int raspberrypi);
