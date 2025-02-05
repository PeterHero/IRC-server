package cz.cuni.mff.hrdinap1.ircserver;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final int connId;
    private final IRCServer server;

    public ConnectionHandler(Socket connection, int connId, IRCServer server) {
        this.socket = connection;
        this.connId = connId;
        this.server = server;
        server.connect(connId);
    }

    private void callCommand(String cmd, List<String> parameters) {
        if ("NICK".equals(cmd)) {
            server.cmdNick(parameters, connId);
        } else if ("USER".equals(cmd)) {
            server.cmdUser(parameters, connId);
        }
    }

    private void processLine(String line) {
        // todo add :nick cmd option

        List<String> words = new ArrayList<>(Arrays.stream(line.split("\\s+")).toList());
        String cmd = words.getFirst();
        words.removeFirst();

        callCommand(cmd, words);
    }

    private void disconnect() {

    }

    public void run() {
        System.out.println("Servicing a connection " + connId);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        /*OutputStream out = socket.getOutputStream(); */
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
            System.out.println("Disconnected! closing a connection");
            server.disconnect(connId);
            socket.close();
        } catch (IOException e) {
            System.out.println("Error! Closing a connection");
            server.disconnect(connId);
        }
    }
}
