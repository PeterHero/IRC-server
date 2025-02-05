package cz.cuni.mff.hrdinap1.ircserver;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRCServer {

    // map of connId and nickname
    private final ConnectionManager connectionManager;
    private final UserManager userManager;

    public IRCServer() {
        this.connectionManager = new ConnectionManager();
        this.userManager = new UserManager();
    }

    public ConnectionHandler createHandler(Socket socket) {
        return connectionManager.createHandler(socket, this);
    }

    public void connect(int connId) {
        userManager.addUser(connId);
    }

    public void disconnect(int connId) {
        // todo maybe pair with quit, kill,...
        connectionManager.removeHandler(connId);
        userManager.removeUser(connId);
        // todo remove user with connection - cleanup
    }

    public void cmdNick(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            // send ERR_NONICKNAMEGIVEN
            return;
        }

        String nickname = parameters.getFirst();
        assert !nickname.isEmpty();

        if (userManager.nicknameInUse(nickname)) {
            // send ERR_NICKNAMEINUSE
            return;
        }

        userManager.setNickname(connId, nickname);

        System.out.println("Paired connection " + connId + " to nickname " + nickname);
    }

    public void cmdUser(List<String> parameters, int connId) {
        if (parameters.size() < 4) {
            // send ERR_NEEDMOREPARAMS
            return;
        }

        if (userManager.userHasUsername(connId)) {
            // send ERR_ALREADYREGISTRED
            return;
        }

        String username = parameters.getFirst();
        String hostname = parameters.get(1);
        String servername = parameters.get(2);
        String realname = String.join(" ", parameters.subList(3, parameters.size()));

        if (realname.charAt(0) != ':') {
            // send syntax error?
            return;
        }

        realname = realname.substring(1);
        userManager.setUserDetails(connId, username, hostname, servername, realname);
        System.out.println("Paired connection " + connId + " to username " + username);
    }
}
