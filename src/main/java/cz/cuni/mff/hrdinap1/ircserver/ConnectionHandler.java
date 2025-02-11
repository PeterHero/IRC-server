package cz.cuni.mff.hrdinap1.ircserver;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Class responsible for handling the communication on the socket
 * It reads commands on the connection and calls the server methods
 */
public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final int connId;
    private final IRCServer server;
    private PrintWriter out;

    /** ConnectionHandler constructor
     * @param connection socket with opened connection
     * @param connId id of the user's connection
     * @param server server instance
     */
    public ConnectionHandler(Socket connection, int connId, IRCServer server) {
        this.socket = connection;
        this.connId = connId;
        this.server = server;
        server.connect(connId);
    }

    /** Calls command on the server instance
     * @param cmd command to call
     * @param parameters command's parameters
     */
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
        } else if ("TOPIC".equals(cmd)) {
            server.cmdTopic(parameters, connId);
        } else if ("KICK".equals(cmd)) {
            server.cmdKick(parameters, connId);
        }
    }

    /** Parse line and call command
     * @param line line to parse
     */
    private void processLine(String line) {
        List<String> words = Arrays.stream(line.split("\\s+")).toList();
        String cmd = words.getFirst().toUpperCase(Locale.ROOT);

        System.out.println(connId + "> " + line);
        callCommand(cmd, words.subList(1, words.size()));
    }

    /** Cleanup method
     */
    private void disconnect() {
        server.disconnect(connId);
        out.close();
        out = null;
    }

    /** Send message to socket
     * @param message text of the message
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /** Read loop on the socket and service commands
     * Can be called as a task for parallel run
     */
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
            System.out.println("Disconnected! closing a connection " + connId);
            disconnect();
            socket.close();
        } catch (IOException e) {
            System.out.println("Error! Closing a connection " + connId);
            disconnect();
        }
    }
}
