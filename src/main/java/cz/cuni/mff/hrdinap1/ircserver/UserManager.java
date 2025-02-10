package cz.cuni.mff.hrdinap1.ircserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Class responsible for managing users
 */
public class UserManager {
    /** Class representing a user */
    private class User {
        public int connId;
        public String nickname;
        public String username;
        public String hostname;
        public String servername;
        public String realname;

        public boolean registered;

        public User(int connId) {
            this.connId = connId;
        }
    }

    /** Mapping of connIds to users */
    private final Map<Integer, User> connToUser;
    /** Mapping of nicknames to users */
    private final Map<String, User> nickToUser;

    /** UserManager constructor
     */
    public UserManager() {
        this.connToUser = new HashMap<>();
        this.nickToUser = new HashMap<>();
    }

    /** Checks if nickname is used
     * @param nickname nickname
     * @return true if nickname is used
     */
    public boolean nicknameInUse(String nickname) { return nickToUser.containsKey(nickname) && nickToUser.get(nickname).nickname != null; }

    /** Checks if user has a nickname set
     * @param connId id of the user's connection
     * @return true if user has a nickname set
     */
    public boolean userHasNickname(int connId) {
        return connToUser.containsKey(connId) && connToUser.get(connId).nickname != null;
    }

    /** Checks if user has a username set
     * @param connId id of the user's connection
     * @return true if user has username set
     */
    public boolean userHasUsername(int connId) { return connToUser.containsKey(connId) && connToUser.get(connId).username != null; }

    /** Checks if user is registered
     * @param connId id of the user's connection
     * @return true if user is registered
     */
    public boolean userIsRegistered(int connId) { return connToUser.containsKey(connId) && connToUser.get(connId).registered; }

    /** Checks if user is registered
     * @param nickname user's nickname
     * @return true if user is registered
     */
    public boolean userIsRegistered(String nickname) { return nickToUser.containsKey(nickname) && nickToUser.get(nickname).registered; }

    /** Get connection id bound to nickname
     * @param nickname user's nickname
     * @return id of the user's connection
     */
    public int getConnId(String nickname) {
        return nickToUser.get(nickname).connId;
    }

    /** Create a user
     * @param connId id of the user's connection
     */
    public void addUser(int connId) {
        connToUser.put(connId, new User(connId));
    }

    /** Delete a user
     * @param connId id of the user's connection
     */
    public void removeUser(int connId) {
        if (userHasNickname(connId)) {
            nickToUser.remove(getNickname(connId));
            connToUser.remove(connId);
        }
    }

    /** Get nickname bound to connection id
     * @param connId id of the user's connection
     * @return user's nickname
     */
    public String getNickname(int connId) {
        if (!connToUser.containsKey(connId)) {
            return null;
        }
        return connToUser.get(connId).username;
    }

    /** Get nicknames bound to connection ids
     * @param connIds set of user connection ids
     * @return set of corresponding nicknames
     */
    public Set<String> getNicknames(Set<Integer> connIds) {
        Set<String> nicknames = new HashSet<>();
        for (int connId: connIds) {
            nicknames.add(getNickname(connId));
        }
        return nicknames;
    }

    /** Set or change nickname
     * @param connId id of the user's connection
     * @param newNickname new nickname
     */
    public void setNickname(int connId, String newNickname) {
        User user = connToUser.get(connId);
        if (userHasNickname(connId)) {
            nickToUser.remove(getNickname(connId));
        }

        user.nickname = newNickname;
        nickToUser.put(newNickname, user);
        if (userHasUsername(connId)) {
            user.registered = true;
        }
    }

    /** Set user details
     * @param connId id of the user's connection
     * @param username username
     * @param hostname hostname
     * @param servername servername
     * @param realname real name of the user
     */
    public void setUserDetails(int connId, String username, String hostname, String servername, String realname) {
        User user = connToUser.get(connId);
        user.username = username;
        user.hostname = hostname;
        user.servername = servername;
        user.realname = realname;

        if (userHasNickname(connId)) {
            user.registered = true;
        }
    }
}
