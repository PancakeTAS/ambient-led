package de.pancake.ambientled.rpi;

import java.awt.*;
import java.io.InputStream;
import java.net.ServerSocket;

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
        this.led = new PiLed(10);

        while (this.serverSocket.isBound()) {
            try (var socket = this.serverSocket.accept()) {
                socket.setTcpNoDelay(true);
                handle(socket.getInputStream());
            } catch (Exception e) {
                System.err.println("Error while handling client:");
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Handle incoming client
     * @param in Input stream of client
     * @throws Exception If an error occurs while handling client
     */
    private void handle(InputStream in) throws Exception {
        byte[] buffer = new byte[3*144];
        while (in.read(buffer) != -1) {
            for (int i = 0; i < 144; i++)
                this.led.write(i, new Color(buffer[3*i] & 0xFF, buffer[3*i+1] & 0xFF, buffer[3*i+2] & 0xFF));

            this.led.flush();
        }
    }

    public static void main(String[] args) throws Exception { new AmbientLedRpi(); }
}
