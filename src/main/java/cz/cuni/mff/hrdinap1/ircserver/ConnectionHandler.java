package cz.cuni.mff.hrdinap1.ircserver;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final int connId;
    private final IRCServer server;
    private PrintWriter out;

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
        } else if ("JOIN".equals(cmd)) {
            server.cmdJoin(parameters, connId);
        } else if ("PRIVMSG".equals(cmd)) {
            server.cmdPrivmsg(parameters, connId);
        } else if ("PART".equals(cmd)) {
            server.cmdPart(parameters, connId);
        } else if ("NAMES".equals(cmd)) {
            server.cmdNames(parameters, connId);
        } else if ("LIST".equals(cmd)) {
            server.cmdList(parameters, connId);
        }
    }

    private void processLine(String line) {
        // todo add :nick cmd option

        List<String> words = Arrays.stream(line.split("\\s+")).toList();
        String cmd = words.getFirst().toUpperCase(Locale.ROOT);

        System.out.println(line);
        callCommand(cmd, words.subList(1, words.size()));
    }

    private void disconnect() {

    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void run() {
        System.out.println("Servicing a connection " + connId);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        ) {
            out = writer;
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
            System.out.println("Disconnected! closing a connection");
            server.disconnect(connId);
            socket.close();
            out = null;
        } catch (IOException e) {
            System.out.println("Error! Closing a connection");
            server.disconnect(connId);
            out = null;
        }
    }
}
