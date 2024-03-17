/// \file arduino.h This file contains necessary methods to communicate with an arduino

#pragma once

/**
 * Open serial port to arduino
 *
 * \param port_name
 *   Name of the port to open
 * \param baud_rate
 *   Baud rate to use
 * \param header
 *   Optional header to send to arduino (this will wait for the arduino to return 1)
 * \param header_len
 *   Size of the header
 *
 * \return
 *   File descriptor of the opened port or -1 if an error occurred
*/
int arduino_open(const char* port_name, int baud_rate, const char* header, int header_len);

/**
 * Close serial port to arduino
 *
 * \param fd
 *   File descriptor of the port to close
*/
void arduino_close(int fd);
