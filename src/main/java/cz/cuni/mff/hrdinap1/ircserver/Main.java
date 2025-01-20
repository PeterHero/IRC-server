package cz.cuni.mff.hrdinap1.ircserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        try (ServerSocket s = new ServerSocket(6667)) {
            System.out.println("Server ready");
            try (Socket socket = s.accept()) {
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                while (true) {
                    int b = in.read();
                    if (b == -1) {
                        break;
                    }

                    out.write(b);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}