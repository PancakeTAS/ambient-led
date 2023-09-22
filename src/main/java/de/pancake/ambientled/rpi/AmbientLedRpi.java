package de.pancake.ambientled.rpi;

import lombok.SneakyThrows;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main class for Raspberry Pi
 * @author Pancake
 */
public class AmbientLedRpi {

    /** Server socket for communication with host */
    private final ServerSocket serverSocket;

    /** LED to control */
    private final PiLed led;

    /**
     * Initialize rpi server
     * @param port Port to listen on
     * @param gpio GPIO pin to use
     * @throws Exception If server socket can't be initialized
     */
    private AmbientLedRpi(int port, int gpio) throws Exception {
        this.serverSocket = new ServerSocket(port);
        this.led = new PiLed(gpio);
        while (true) {
            var socket = this.serverSocket.accept();
            socket.setTcpNoDelay(true);
            new Thread(() -> this.handle(socket), "Client Handler").start();
        }
    }

    /**
     * Handle incoming client
     * @param socket Client socket
     */
    @SneakyThrows
    private void handle(Socket s) {
        var in = s.getInputStream();
        var buffer = new byte[3*144];
        while (in.read(buffer) != -1) {
            for (int i = 0; i < 144; i++)
                this.led.write(i, buffer[i*3], buffer[i*3+1], buffer[i*3+2]);

            this.led.flush();
        }
    }

    public static void main(String[] args) throws Exception { new AmbientLedRpi(Integer.parseInt(args[0]), Integer.parseInt(args[1])); }
}
