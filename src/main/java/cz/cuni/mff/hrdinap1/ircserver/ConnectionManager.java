package cz.cuni.mff.hrdinap1.ircserver;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {
    Map<Integer, ConnectionHandler> openedConnections = new HashMap<>();
    int hintId = 0;

    public ConnectionHandler createHandler(Socket socket, IRCServer server) {
        int connId = hintId;
        while(openedConnections.containsKey(connId)) {
            connId++;
        }
        ConnectionHandler connectionHandler = new ConnectionHandler(socket, connId, server);
        openedConnections.put(connId, connectionHandler);
        return connectionHandler;
    }

    public void removeHandler(int connId) {
        openedConnections.remove(connId);
    }

    public void sendMessage(int connId, String message) {
        if (openedConnections.containsKey(connId)) {
            openedConnections.get(connId).sendMessage(message);
        }
    }
}
