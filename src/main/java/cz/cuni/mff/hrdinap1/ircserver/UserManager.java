package cz.cuni.mff.hrdinap1.ircserver;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private class User {
        public int connId;
        public String nickname;
        public String username;
        public String hostname;
        public String servername;
        public String realname;

        public boolean registered; // todo set registered and check it with every command

        public User(int connId) {
            this.connId = connId;
        }
    }

    private final Map<Integer, User> connToUser;
    private final Map<String, User> nickToUser;

    public UserManager() {
        this.connToUser = new HashMap<>();
        this.nickToUser = new HashMap<>();
    }

    public boolean nicknameInUse(String nickname) { return nickToUser.containsKey(nickname) && nickToUser.get(nickname).nickname != null; }
    public boolean userHasNickname(int connId) {
        return connToUser.containsKey(connId) && connToUser.get(connId).nickname != null;
    }
    public boolean userHasUsername(int connId) { return connToUser.containsKey(connId) && connToUser.get(connId).username != null; }

    public int getConnId(String nickname) {
        return nickToUser.get(nickname).connId;
    }

    public void addUser(int connId) {
        connToUser.put(connId, new User(connId));

    }

    public void removeUser(int connId) {
        if (userHasNickname(connId)) {
            nickToUser.remove(getNickname(connId));
            connToUser.remove(connId);
        }
    }

    public String getNickname(int connId) {
        if (!connToUser.containsKey(connId)) {
            return null;
        }
        return connToUser.get(connId).username;
    }

    public void setNickname(int connId, String newNickname) {
        User user = connToUser.get(connId);
        if (userHasNickname(connId)) {
            nickToUser.remove(getNickname(connId));
        }

        user.nickname = newNickname;
        nickToUser.put(newNickname, user);
    }

    public void setUserDetails(int connId, String username, String hostname, String servername, String realname) {
        User user = connToUser.get(connId);
        user.username = username;
        user.hostname = hostname;
        user.servername = servername;
        user.realname = realname;
    }
}
