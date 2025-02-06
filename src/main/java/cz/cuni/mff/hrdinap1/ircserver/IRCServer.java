package cz.cuni.mff.hrdinap1.ircserver;

import java.net.Socket;
import java.util.*;
import static cz.cuni.mff.hrdinap1.ircserver.Numerics.*;

public class IRCServer {
    public static final char channelPrefix = '#';

    private final ChannelManager channelManager;
    private final ConnectionManager connectionManager;
    private final UserManager userManager;

    public IRCServer() {
        this.channelManager = new ChannelManager();
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

    public void sendReply(int targetConnId, String source, int replyNumber, String target, String message) {
        String completeMessage = ":" + source + " " + replyNumber + " " + target + " " + message;
        connectionManager.sendMessage(targetConnId, completeMessage);
    }

    private void sendMessage(int targetConnId, String source, String command, String parameters) {
        String completeMessage;
        if (parameters == null) {
            completeMessage = ":" + source + " " + command;
        } else {
            completeMessage = ":" + source + " " + command + " " + parameters;
        }
        connectionManager.sendMessage(targetConnId, completeMessage);
    }

    public void sendMessage(String target, String source, String command, String parameters) {
        if (target.charAt(0) == channelPrefix) {
            if (channelManager.channelExists(target)) {
                for (int userConnId: channelManager.getChannelUsers(target)) {
                    if (userConnId != userManager.getConnId(source)) {
                        sendMessage(userConnId, source, command, parameters);
                    }
                }
            } else {
                // send ERR_CANNOTSENDTOCHAN
            }
        } else {
            if (userManager.userIsRegistered(target)) {
                sendMessage(userManager.getConnId(target), source, command, parameters);
            } else {
                // send ERR_NOSUCHNICK
            }
        }
    }

    // public void sendMessageToChannel()

    // send mess to channel

    private List<String> splitBy(String string, String delimiter) {
        return Arrays.stream(string.split(delimiter)).toList();
    }

    private String joinBy(List<String> list, String delimiter, int from) {
        return String.join(delimiter, list.subList(from, list.size()));
    }

    public void cmdNick(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            // send ERR_NONICKNAMEGIVEN
            return;
        }

        String nickname = parameters.getFirst();
        assert !nickname.isEmpty();

        if (nickname.charAt(0) == channelPrefix || nickname.charAt(0) == ':' || nickname.contains(" ")) {
            // send ERR_ERRONEUSNICKNAME
            return;
        }

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
        String realname = joinBy(parameters, " ", 3);

        if (realname.charAt(0) != ':') {
            // send syntax error?
            return;
        }

        realname = realname.substring(1);
        userManager.setUserDetails(connId, username, hostname, servername, realname);
        System.out.println("Paired connection " + connId + " to username " + username);
    }

    public void cmdJoin(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            // send ERR_NEEDMOREPARAMS
            return;
        }

        List<String> channels = splitBy(parameters.getFirst(), ",");

        List<String> keys = new ArrayList<>();
        if (parameters.size() >= 2) {
            keys = splitBy(parameters.get(1), ",");
        }

        for (String channel: channels) {
            channelManager.join(connId, channel, null);
            sendMessage(channel, userManager.getNickname(connId), "JOIN", channel);
            // send RPL with list of users
        }
    }

    public void cmdPrivmsg(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            // send ERR_NORECIPIENT
            return;
        }

        List<String> targets = splitBy(parameters.getFirst(), ",");
        String message = joinBy(parameters, " ", 1);
        String nickname = userManager.getNickname(connId);

        for (String target: targets) {
            sendMessage(target, nickname, "PRIVMSG", target + " " + message);
        }
    }
}
