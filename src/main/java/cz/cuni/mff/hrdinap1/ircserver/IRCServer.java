package cz.cuni.mff.hrdinap1.ircserver;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRCServer {
    private class User {
        public String nickname;
    }
    // map of connId and nickname
    private Map<Integer, String> connToNick;
    private ConnectionManager connectionManager;

    public IRCServer() {
        this.connToNick = new HashMap<>();
        this.connectionManager = new ConnectionManager();
    }

    public ConnectionHandler createHandler(Socket socket) {
        return connectionManager.createHandler(socket, this);
    }

    public void disconnect(int connId) {
        // todo maybe pair with quit, kill,...
        connectionManager.removeHandler(connId);
        // todo remove user with connection - cleanup
    }

    public void cmdNick(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            // send ERR_NONICKNAMEGIVEN
            return;
        }

        String nickname = parameters.getFirst();
        if (connToNick.containsValue(nickname)) {
            // send ERR_NICKNAMEINUSE
            return;
        }

        connToNick.put(connId, nickname);
        System.out.println("Paired connection " + connId + " to nickname " + nickname);

        // todo add feature to change nickname
    }
    // this will be the class that will hold all data about server, its users, channels and so on
}
