package cz.cuni.mff.hrdinap1.ircserver;

import java.net.Socket;
import java.util.*;
import static cz.cuni.mff.hrdinap1.ircserver.Numerics.*;

/** Server servicing commands from users
 * Divides the responsibility to channel, connection and user managers
 */
public class IRCServer {
    public static final char channelPrefix = '#';
    public static final char publicChannelSymbol = '=';
    public static final char channelOperatorPrefix = '@';

    private final ChannelManager channelManager;
    private final ConnectionManager connectionManager;
    private final UserManager userManager;
    private final String serverName;

    /** Server constructor
     *
     * @param serverName name used in server responses
     */
    public IRCServer(String serverName) {
        this.channelManager = new ChannelManager();
        this.connectionManager = new ConnectionManager();
        this.userManager = new UserManager();
        this.serverName = serverName;
    }

    /** Factory method creating ConnectionHandler
     * @param socket socket with the connected user
     * @return new connection handler servicing the socket and connection
     */
    public synchronized ConnectionHandler createHandler(Socket socket) {
        return connectionManager.createHandler(socket, this);
    }

    /** Connect a new user to server
     * @param connId id of the user's connection
     */
    public synchronized void connect(int connId) {
        userManager.addUser(connId);
    }

    /** Disconnect a user
     * used for cleanup
     * @param connId id of the user's connection
     */
    public synchronized void disconnect(int connId) {
        connectionManager.removeHandler(connId);
        userManager.removeUser(connId);
        channelManager.removeUser(connId);
    }

    /** Send a formatted reply to user on a connection
     * @param targetConnId id of the user's connection
     * @param replyNumber numeric with the type of reply
     * @param message text of the reply
     */
    private void sendReply(int targetConnId, int replyNumber, String message) {
        String completeMessage = ":" + serverName + " " + replyNumber + " " + userManager.getNickname(targetConnId) + " " + message;
        connectionManager.sendMessage(targetConnId, completeMessage);
    }

    /** Send a formatted command message to user on a connection
     * @param targetConnId id of the user's connection
     * @param source original sender of the message
     * @param command command to send
     * @param parameters other parameters of the message
     */
    private void sendMessage(int targetConnId, String source, String command, String parameters) {
        String completeMessage;
        if (parameters == null) {
            completeMessage = ":" + source + " " + command;
        } else {
            completeMessage = ":" + source + " " + command + " " + parameters;
        }
        connectionManager.sendMessage(targetConnId, completeMessage);
    }

    /** Send a formatted command message to a nick/channel
     * If the target is channel, sends the message to all its users
     * @param target nickname or name of a channel
     * @param source sender of the message
     * @param command command to send
     * @param parameters other parameters of the message
     * @param includeSender if true and target is a channel the sender will also receive the message
     */
    private void sendMessage(String target, String source, String command, String parameters, boolean includeSender) {
        if (target.charAt(0) == channelPrefix) {
            assert channelManager.channelExists(target);
            for (int userConnId: channelManager.getChannelUsers(target)) {
                if (includeSender || userConnId != userManager.getConnId(source)) {
                    sendMessage(userConnId, source, command, parameters);
                }
            }
        } else {
            assert userManager.userIsRegistered(target);
            sendMessage(userManager.getConnId(target), source, command, parameters);
        }
    }

    /** Splits a string by delimiter into list
     * @param string String to split
     * @param delimiter Delimiter to split by
     * @return list of words after splitting
     */
    private List<String> splitBy(String string, String delimiter) {
        return Arrays.stream(string.split(delimiter)).toList();
    }

    /** Joins a list of words using delimiter
     * @param list List of words to join
     * @param delimiter String used as delimiter
     * @param from Index in the list to start from
     * @return String of words connected by delimiter
     */
    private String joinBy(List<String> list, String delimiter, int from) {
        return String.join(delimiter, list.subList(from, list.size()));
    }

    /** Get list of users in a channel
     * @param channel Channel name
     * @return list of users in channel divided by space
     */
    private String getChannelUsers(String channel) {
        Set<Integer> connIds = channelManager.getChannelUsers(channel);
        List<String> nicknames = new ArrayList<>();
        for (int connId: connIds) {
            boolean isOperator = channelManager.isChannelOperator(connId, channel);
            String nickname = userManager.getNickname(connId);
            String fullNickname = isOperator ? channelOperatorPrefix + nickname : nickname;
            nicknames.add(fullNickname);
        }
        return joinBy(nicknames, " ", 0);
    }

    /** Service NICK command message
     * Set or change user nickname. Needed to register connection with USER command.
     * Possible errors:
     * ERR_NOCICKNAMEGIVEN - no nickname was given
     * ERR_ERRONEUSNICKNAME - nickname is in incorrect format
     * ERR_NICKNAMEINUSE - nickname is already used
     * @param parameters &lt;nickname&gt;
     * @param connId id of the user's connection
     */
    public synchronized void cmdNick(List<String> parameters, int connId) {
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
    }

    /** Service USER command message
     * Set user details. Needed to register connection with NICK command.
     * Possible errors:
     * ERR_NEEDMOREPARAMS - not enough parameters were given
     * ERR_ALREADYREGISTERED - USER command was already handled
     * @param parameters &lt;username&gt; &lt;hostname&gt; &lt;servername&gt; &lt;realname&gt; - realname is a trailing
     * parameter - multiple words, the first must start with a ':'
     * @param connId id of the user's connection
     */
    public synchronized void cmdUser(List<String> parameters, int connId) {
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
    }

    /** Service JOIN command message
     * Joins user to channel. If the channel does not exist it is created with the user as an operator
     * Possible errors:
     * ERR_NEEDMOREPARAMS - not enough parameters were given
     * ERR_BADCHANMASK - channel name in incorrect format
     * Reply on success:
     * RPL_TOPIC - channel topic if the topic is set
     * RPL_NAMREPLY - list of users in the channel
     * RPL_ENDOFNAMES - message signaling end of the list
     *
     * @param parameters &lt;channel&gt;{,&lt;channel&gt;} [&lt;key&gt;{,&lt;key&gt;}] - list of channels to join and their keys - keys are not used in this implementation
     * @param connId id of the user's connection
     */
    public synchronized void cmdJoin(List<String> parameters, int connId) {
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
            sendMessage(channel, userManager.getNickname(connId), "JOIN", channel, false);
            if (channelManager.isTopicSet(channel))
                sendReply(connId, RPL_TOPIC, channel + " :" + channelManager.getTopic(channel));
            sendReply(connId, RPL_NAMREPLY, publicChannelSymbol + " " + channel + " :" + getChannelUsers(channel));
            sendReply(connId, RPL_ENDOFNAMES, channel + " :End of /NAMES list");
        }
    }

    /** Service PRIVMSG command message
     * The PRIVMSG command is used to send private messages between users, as well as to send messages to channels.
     * &lt;target&gt; is the nickname of a client or the name of a channel.
     * Possible errors:
     * ERR_NORECIPIENT - no target given
     * ERR_NOSUCHNICK - the target nick/channel does not exist
     * @param parameters &lt;target&gt;{,&lt;target&gt;} &lt;text to be sent&gt;
     * @param connId id of the user's connection
     */
    public synchronized void cmdPrivmsg(List<String> parameters, int connId) {
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
                sendMessage(target, nickname, "PRIVMSG", target + " " + message, false);
            }
        }
    }

    /** Service PART command message
     * The PART command removes the client from the given channel(s). On sending a successful PART command, the user will
     * receive a PART message from the server for each channel they have been removed from. &lt;reason&gt; is the reason that
     * the client has left the channel(s).
     * Possible errors:
     * ERR_NEEDMOREPARAMS - not enough parameters
     * ERR_NOSUCHCHANNEL - the channel does not exist
     * ERR_NOTONCHANNEL - user is not on the channel
     * @param parameters &lt;channel&gt;{,&lt;channel&gt;} [&lt;reason&gt;]
     * @param connId id of the user's connection
     */
    public synchronized void cmdPart(List<String> parameters, int connId) {
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
                        sendMessage(channel, userManager.getNickname(connId), "PART", channel + " " + reason, false);
                    } else {
                        sendMessage(channel, userManager.getNickname(connId), "PART", channel, false);
                    }
                } else {
                    sendReply(connId, ERR_NOTONCHANNEL, channel + " :You're not on that channel");
                }
            } else {
                sendReply(connId, ERR_NOSUCHCHANNEL, channel + " :No such channel");
            }
        }
    }

    /** Service NAMES command message
     * The NAMES command is used to view the nicknames joined to a channel
     * @param parameters &lt;channel&gt;{,&lt;channel&gt;}
     * @param connId id of the user's connection
     */
    public synchronized void cmdNames(List<String> parameters, int connId) {
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

    /** Service LIST command message
     * The LIST command is used to get a list of channels along with some information about each channel.
     * If no parameter is given all server's channels are listed
     * @param parameters [&lt;channel&gt;{,&lt;channel&gt;}]
     * @param connId id of the user's connection
     */
    public synchronized void cmdList(List<String> parameters, int connId) {
        List<String> channels;
        if (parameters.isEmpty()) {
            channels = channelManager.getChannels();
        } else {
            channels = splitBy(parameters.getFirst(), ",");
        }

        sendReply(connId, RPL_LISTSTART, "Channel :Users  Name");
        for (String channel: channels) {
            if (channelManager.channelExists(channel)) {
                sendReply(connId, RPL_LIST, channel + " " + channelManager.getCount(channel) + " :" + channelManager.getTopic(channel));
            }
        }
        sendReply(connId, RPL_LISTEND, ":End of /LIST");
    }

    /** Service TOPIC command message
     * The TOPIC command is used to change or view the topic of the given channel. If &lt;topic&gt; is not given, either
     * RPL_TOPIC or RPL_NOTOPIC is returned specifying the current channel topic or lack of one. If &lt;topic&gt; is an empty
     * string, the topic for the channel will be cleared.
     * Possible errors:
     * ERR_NEEDMOREPARAMS - not enough parameters
     * ERR_NOTONCHANNEL - user tries to change topic but is not on that channel
     * ERR_UNKNOWNERROR - topic string in wrong format
     * @param parameters &lt;channel&gt; [&lt;topic&gt;]
     * @param connId id of the user's connection
     */
    public synchronized void cmdTopic(List<String> parameters, int connId) {
        if (parameters.isEmpty()) {
            sendReply(connId, ERR_NEEDMOREPARAMS, "TOPIC :Not enough parameters");
            return;
        }

        String channel = parameters.getFirst();

        if (parameters.size() == 1) {
            if (channelManager.isTopicSet(channel)) {
                sendReply(connId, RPL_TOPIC, channel + " :" + channelManager.getTopic(channel));
            } else {
                sendReply(connId, RPL_NOTOPIC, channel + " :No topic is set");
            }
        } else {
            if (!channelManager.isUserInChannel(connId, channel)) {
                sendReply(connId, ERR_NOTONCHANNEL, channel + " :You're not on that channel");
                return;
            }

            String topic = joinBy(parameters, " ", 1);
            if (topic.charAt(0) == ':') {
                if (topic.length() == 1) {
                    channelManager.clearTopic(channel);
                    sendMessage(channel, userManager.getNickname(connId), "TOPIC", null, true);
                } else {
                    channelManager.setTopic(channel, topic.substring(1));
                    sendMessage(channel, userManager.getNickname(connId), "TOPIC", topic, true);
                }
            } else {
                sendReply(connId, ERR_UNKNOWNERROR, "TOPIC :missing colon for trailing parameter");
            }
        }
    }

    /** Service KICK command message
     * The KICK command can be used to request the forced removal of a user from a channel. It causes the &lt;user&gt; to be
     * removed from the &lt;channel&gt; by force.
     * Possible errors:
     * ERR_NEEDMOREPARAMS - not enough parameters
     * ERR_NOSUCHCHANNEL - the channel does not exist
     * ERR_NOTONCHANNEL - not in channel, cannot perform this action
     * ERR_CHANOPRIVSNEEDED - user is not a channel operator
     * ERR_USERNOTINCHANNEL - the target user is not on the channel
     * @param parameters &lt;channel&gt; &lt;user&gt; *( "," &lt;user&gt; ) [&lt;comment&gt;]
     * @param connId id of the user's connection
     */
    public synchronized void cmdKick(List<String> parameters, int connId) {
        if (parameters.size() < 2) {
            sendReply(connId, ERR_NEEDMOREPARAMS, "KICK :Not enough parameters");
            return;
        }

        String channel = parameters.getFirst();
        List<String> users = splitBy(parameters.get(1), ",");
        String reason = ":" + userManager.getNickname(connId);

        if (parameters.size() > 2) {
            reason = joinBy(parameters, " ", 2);
        }

        if (!channelManager.channelExists(channel)) {
            sendReply(connId, ERR_NOSUCHCHANNEL, channel + " : No such channel");
            return;
        }

        if (!channelManager.isUserInChannel(connId, channel)) {
            sendReply(connId, ERR_NOTONCHANNEL, channel + " :You're not on that channel");
            return;
        }

        if (!channelManager.isChannelOperator(connId, channel)) {
            sendReply(connId, ERR_CHANOPRIVSNEEDED, channel + ":You're not channel operator");
            return;
        }

        for (String user: users) {
            if (!userManager.userIsRegistered(user) || !channelManager.isUserInChannel(userManager.getConnId(user), channel)) {
                sendReply(connId, ERR_USERNOTINCHANNEL, user + " " + channel + " :They aren't on that channel");
                continue;
            }

            sendMessage(channel, userManager.getNickname(connId), "KICK", channel + " " + user + " " + reason, true);
            channelManager.leave(userManager.getConnId(user), channel);
        }
    }
}
