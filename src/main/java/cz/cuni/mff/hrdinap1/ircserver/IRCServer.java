package cz.cuni.mff.hrdinap1.ircserver;

import java.net.Socket;
import java.util.*;
import static cz.cuni.mff.hrdinap1.ircserver.Numerics.*;

public class IRCServer {
    public static final char channelPrefix = '#';
    public static final char publicChannelSymbol = '=';

    private final ChannelManager channelManager;
    private final ConnectionManager connectionManager;
    private final UserManager userManager;
    private final String serverName;

    public IRCServer(String serverName) {
        this.channelManager = new ChannelManager();
        this.connectionManager = new ConnectionManager();
        this.userManager = new UserManager();
        this.serverName = serverName;
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
        channelManager.removeUser(connId);
    }

    public void sendReply(int targetConnId, int replyNumber, String message) {
        String completeMessage = ":" + serverName + " " + replyNumber + " " + userManager.getNickname(targetConnId) + " " + message;
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
            assert channelManager.channelExists(target);
            for (int userConnId: channelManager.getChannelUsers(target)) {
                if (userConnId != userManager.getConnId(source)) {
                    sendMessage(userConnId, source, command, parameters);
                }
            }
        } else {
            assert userManager.userIsRegistered(target);
            sendMessage(userManager.getConnId(target), source, command, parameters);
        }
    }

    private List<String> splitBy(String string, String delimiter) {
        return Arrays.stream(string.split(delimiter)).toList();
    }

    private String joinBy(List<String> list, String delimiter, int from) {
        return String.join(delimiter, list.subList(from, list.size()));
    }

    private String getChannelUsers(String channel) {
        Set<Integer> connIds = channelManager.getChannelUsers(channel);
        Set<String> nicknames = userManager.getNicknames(connIds);
        return joinBy(nicknames.stream().toList(), " ", 0);
    }

    public void cmdNick(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            sendReply(connId, ERR_NONICKNAMEGIVEN, ":No nickname given");
            return;
        }

        String nickname = parameters.getFirst();
        assert !nickname.isEmpty();

        if (nickname.charAt(0) == channelPrefix || nickname.charAt(0) == ':' || nickname.contains(" ")) {
            sendReply(connId, ERR_ERRONEUSNICKNAME, nickname + " :Erroneus nickname");
            return;
        }

        if (userManager.nicknameInUse(nickname)) {
            sendReply(connId, ERR_NICKNAMEINUSE, nickname + ":Nickname is already in use");
            return;
        }

        userManager.setNickname(connId, nickname);

        System.out.println("Paired connection " + connId + " to nickname " + nickname);
    }

    public void cmdUser(List<String> parameters, int connId) {
        if (parameters.size() < 4) {
            sendReply(connId, ERR_NEEDMOREPARAMS, "USER :Not enough parameters");
            return;
        }

        if (userManager.userHasUsername(connId)) {
            sendReply(connId, ERR_ALREADYREGISTERED, ":You may not reregister");
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
            sendReply(connId, ERR_NEEDMOREPARAMS, "JOIN :Not enough parameters");
            return;
        }

        List<String> channels = splitBy(parameters.getFirst(), ",");

        List<String> keys = new ArrayList<>();
        if (parameters.size() >= 2) {
            keys = splitBy(parameters.get(1), ",");
        }

        for (String channel: channels) {
            if (channel.charAt(0) != channelPrefix) {
                sendReply(connId, ERR_BADCHANMASK, ":Bad Channel Mask");
                continue;
            }
            channelManager.join(connId, channel, null);
            sendMessage(channel, userManager.getNickname(connId), "JOIN", channel);
            sendReply(connId, RPL_NAMREPLY, publicChannelSymbol + " " + channel + " :" + getChannelUsers(channel));
            sendReply(connId, RPL_ENDOFNAMES, channel + " :End of /NAMES list");
        }
    }

    public void cmdPrivmsg(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            sendReply(connId, ERR_NORECIPIENT, ":No recipient given (PRIVMSG)");
            return;
        }

        List<String> targets = splitBy(parameters.getFirst(), ",");
        String message = joinBy(parameters, " ", 1);
        String nickname = userManager.getNickname(connId);

        for (String target: targets) {
            if (target.charAt(0) == channelPrefix && !channelManager.channelExists(target)) {
                sendReply(connId, ERR_NOSUCHNICK, target + " :No such nick/channel");
            } else if (target.charAt(0) != channelPrefix && !userManager.userIsRegistered(target)) {
                sendReply(connId, ERR_NOSUCHNICK, target + " :No such nick/channel");
            } else {
                sendMessage(target, nickname, "PRIVMSG", target + " " + message);
            }
        }
    }

    public void cmdPart(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            sendReply(connId, ERR_NEEDMOREPARAMS, "PART :Not enough parameters");
            return;
        }

        List<String> channels = splitBy(parameters.getFirst(), ",");
        String reason = null;
        if (parameters.size() >= 2) {
            reason = joinBy(parameters, " ", 1);
        }

        for (String channel: channels) {
            if (channelManager.channelExists(channel)) {
                if (channelManager.isUserInChannel(connId, channel)) {
                    channelManager.leave(connId, channel);
                    if (reason != null) {
                        sendMessage(channel, userManager.getNickname(connId), "PART", channel + " " + reason);
                    } else {
                        sendMessage(channel, userManager.getNickname(connId), "PART", channel);
                    }
                    // leave
                } else {
                    sendReply(connId, ERR_NOTONCHANNEL, channel + " :You're not on that channel");
                }
            } else {
                sendReply(connId, ERR_NOSUCHCHANNEL, channel + " :No such channel");
            }
        }
    }

    public void cmdNames(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            // list all users, not implemented yet
            return;
        }

        List<String> channels = splitBy(parameters.getFirst(), ",");
        for (String channel: channels) {
            if (channelManager.channelExists(channel)) {
                sendReply(connId, RPL_NAMREPLY, publicChannelSymbol + " " + channel + " :" + getChannelUsers(channel));
                sendReply(connId, RPL_ENDOFNAMES, channel + " :End of /NAMES list");
            } else {
                sendReply(connId, RPL_ENDOFNAMES, channel);
            }
        }
    }
}
