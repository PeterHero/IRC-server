package cz.cuni.mff.hrdinap1.ircserver;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/** Class responsible for managing connections
 * Holds mappings of connection ids to opened connections
 */
public class ConnectionManager {
    Map<Integer, ConnectionHandler> openedConnections = new HashMap<>();
    int hintId = 0;

    /** Creates a connection handler
     * Factory method which creates a handler that will call server methods
     * @param socket socket with opened connection
     * @param server instance of a server which will be called by the handler
     * @return
     */
    public ConnectionHandler createHandler(Socket socket, IRCServer server) {
        int connId = hintId;
        while(openedConnections.containsKey(connId)) {
            connId++;
        }
        ConnectionHandler connectionHandler = new ConnectionHandler(socket, connId, server);
        openedConnections.put(connId, connectionHandler);
        return connectionHandler;
    }

    /** Removes a connection handler
     * @param connId id of the user's connection
     */
    public void removeHandler(int connId) {
        openedConnections.remove(connId);
    }

    /** Sends a message to a user on a connection
     * @param connId id of the user's connection
     * @param message text of the message
     */
    public void sendMessage(int connId, String message) {
        if (openedConnections.containsKey(connId)) {
            openedConnections.get(connId).sendMessage(message);
        }
    }
}
