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
     * @throws Exception If server socket can't be initialized
     */
    private AmbientLedRpi() throws Exception {
        this.serverSocket = new ServerSocket(5163);
        this.led = new PiLed(18);
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

    public static void main(String[] args) throws Exception { new AmbientLedRpi(); }
}
