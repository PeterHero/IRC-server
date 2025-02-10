package cz.cuni.mff.hrdinap1.ircserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class Main {
    /** Starts an IRC server and listens to connections
     * @param args port number as optional first argument
     */
    public static void main(String[] args) {
        int serverPort = 6667;
        if (args.length > 0) {
            try {
                serverPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port argument. Defaulting to " + serverPort);
            }
        }
        IRCServer server = new IRCServer("mff.testing.cz");

        try (ServerSocket s = new ServerSocket(serverPort)) {
            System.out.println("Started server on port " + serverPort);
            try (var executor = Executors.newWorkStealingPool()) {
                while (true) {
                    Socket socket = s.accept();
                    executor.submit(server.createHandler(socket));
                }
            }
        } catch (IOException e) {
            System.out.println("Error could not start server on port" + serverPort);
            throw new RuntimeException(e);
        }
    }
}