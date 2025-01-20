package cz.cuni.mff.hrdinap1.ircserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        int serverPort = 6667;

        try (ServerSocket s = new ServerSocket(serverPort)) {
            System.out.println("Server ready");

            try (var executor = Executors.newWorkStealingPool()) {
                while (true) {
                    Socket socket = s.accept();
                    executor.submit(new ServiceRequest(socket));
                }
            }

        } catch (IOException e) {
            System.out.println("Error could not start server on port" + serverPort);
            throw new RuntimeException(e);
        }
    }
}