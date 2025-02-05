package cz.cuni.mff.hrdinap1.ircserver;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRCServer {
    private class User {
        public String nickname;
        public String username;
        public String hostname;
        public String servername;
        public String realname;

        public boolean registered;
    }
    // map of connId and nickname
    private final ConnectionManager connectionManager;
    private Map<Integer, User> connToUser;
    private Map<String, User> nickToUser;

    public IRCServer() {
        this.connectionManager = new ConnectionManager();
        this.connToUser = new HashMap<>();
        this.nickToUser = new HashMap<>();
    }

    public ConnectionHandler createHandler(Socket socket) {
        return connectionManager.createHandler(socket, this);
    }

    public void connect(int connId) {
        connToUser.put(connId, new User());
    }

    public void disconnect(int connId) {
        // todo maybe pair with quit, kill,...
        connectionManager.removeHandler(connId);
        // todo remove user with connection - cleanup
    }

    private boolean nicknameInUse(String nickname) { return nickToUser.containsKey(nickname) && nickToUser.get(nickname).nickname != null; }
    private boolean userHasNickname(int connId) {
        return connToUser.containsKey(connId) && connToUser.get(connId).nickname != null;
    }
    private boolean userHasUsername(int connId) { return connToUser.containsKey(connId) && connToUser.get(connId).username != null; }
    // todo move user logic to UserManager

    private void changeNickname(int connId, String newNickname) {
        User user = connToUser.get(connId);
        String oldNickname = user.nickname;
        user.nickname = newNickname;
        nickToUser.remove(oldNickname);
        nickToUser.put(newNickname, user);
    }

    private void setNickname(int connId, String newNickname) {
        User user = connToUser.get(connId);
        user.nickname = newNickname;
        nickToUser.put(newNickname, user);
    }

    public void cmdNick(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            // send ERR_NONICKNAMEGIVEN
            return;
        }

        String nickname = parameters.getFirst();
        assert !nickname.isEmpty();

        if (nicknameInUse(nickname)) {
            // send ERR_NICKNAMEINUSE
            return;
        }

        if (userHasNickname(connId)) {
            changeNickname(connId, nickname);
        } else {
            setNickname(connId, nickname);
        }

        System.out.println("Paired connection " + connId + " to nickname " + nickname);
    }

    private void setUserDetails(int connId, String username, String hostname, String servername, String realname) {
        User user = connToUser.get(connId);
        user.username = username;
        user.hostname = hostname;
        user.servername = servername;
        user.realname = realname;
    }

    public void cmdUser(List<String> parameters, int connId) {
        if (parameters.size() < 4) {
            // send ERR_NEEDMOREPARAMS
            return;
        }

        if (userHasUsername(connId)) {
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
        setUserDetails(connId, username, hostname, servername, realname);
        System.out.println("Paired connection " + connId + " to username " + username);
    }
}
