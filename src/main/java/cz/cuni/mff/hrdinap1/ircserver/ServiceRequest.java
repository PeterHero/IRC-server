package cz.cuni.mff.hrdinap1.ircserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ServiceRequest implements Runnable {
    private final Socket socket;

    public ServiceRequest(Socket connection) {
        this.socket = connection;
    }

    public void run() {
        System.out.println("Servicing a connection");

        try (InputStream in = socket.getInputStream(); /*OutputStream out = socket.getOutputStream(); */) {
            while (true) {
                int b = in.read();
                if (b == -1) {
                    break;
                }

                System.out.write(b);
            }
            socket.close();
        } catch (IOException e) {
            System.out.println("Error! Closing a connection");
        }
    }
}
