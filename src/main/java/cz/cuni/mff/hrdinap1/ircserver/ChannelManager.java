package cz.cuni.mff.hrdinap1.ircserver;

import java.util.*;

/** Class responsible for managing channels
 */
public class ChannelManager {
    /** Class representing user in a channel
     */
    private class ChannelUser {
        public int connId;
        public boolean isOperator;

        /**
         * @param connId id of the user's connection
         */
        public ChannelUser(int connId) {
            this.connId = connId;
            this.isOperator = false;
        }
    }

    /** Class representing a channel
     * Includes information about users and topic of the channel.
     */
    private class Channel {
        private final Map<Integer, ChannelUser> users;
        String topic;

        /** Channel constructor
         */
        public Channel() {
            this.users = new HashMap<>();
        }

        /** Checks if user is joined
         * @param connId id of the user's connection
         * @return true if user is joined
         */
        public boolean isJoined(int connId) {
            return users.containsKey(connId);
        }

        /** Add user to channel
         * @param connId id of the user's connection
         */
        public void join(int connId) {
            if (!users.containsKey(connId)) {
                users.put(connId, new ChannelUser(connId));
            }
        }

        /** Remove user from channel
         * @param connId id of the user's connection
         */
        public void quit(int connId) {
            users.remove(connId);
        }

        /** Check if user is operator
         * @param connId id of the user's connection
         * @return true if user is operator
         */
        public boolean isOperator(int connId) {
            if (users.containsKey(connId)) {
                return users.get(connId).isOperator;
            } else {
                return false;
            }
        }

        /** Give operator status to user
         * @param connId id of the user's connection
         */
        public void setOperator(int connId) {
            if (users.containsKey(connId)) {
                users.get(connId).isOperator = true;
            } else {
                System.out.println("Error user not in channel.");
            }
        }

        /** Get connection ids of channel users
         * @return set of connection ids of channel users
         */
        public Set<Integer> getUsers() {
            return users.keySet();
        }

        /** Get number of users in channel
         * @return number of users in channel
         */
        public int count() {
            return users.size();
        }

        /** Get channel topic
         * @return text of channel topic
         */
        public String getTopic() {
            return topic;
        }

        /** Set channel topic
         * @param newTopic new topic text
         */
        public void setTopic(String newTopic) {
            topic = newTopic;
        }

        /** Clear channel topic
         */
        public void clearTopic() {
            topic = null;
        }

        /** Checks if channel topic is set
         * @return true if channel topic is set
         */
        public boolean isTopicSet() {
            return topic != null;
        }

    }

    /** List of channels stored as mapping of names to corresponding channel class for fast look up */
    private final Map<String, Channel> nameToChann;

    public ChannelManager() {
        nameToChann = new HashMap<>();
    }

    /** Get channel instance by channel name
     * @param channel channel name
     * @return channel instance or null if the channel does not exist
     */
    private Channel getChannel(String channel) {
        return nameToChann.getOrDefault(channel, null);
    }

    /** Create a channel and join user
     * @param channel channel name
     * @param connId id of the user's connection
     */
    private void addChannel(String channel, int connId) {
        Channel newChannel = new Channel();
        newChannel.join(connId);
        newChannel.setOperator(connId);
        nameToChann.put(channel, newChannel);
    }

    /** Remove channel
     * @param channel channel name
     */
    private void removeChannel(String channel) {
        nameToChann.remove(channel);
    }

    /** Get list of channel names
     * @return list of channel names
     */
    public List<String> getChannels() {
        return nameToChann.keySet().stream().toList();
    }

    /** Checks if the channel exists
     * @param channel channel name
     * @return true if channel exists, else false
     */
    public boolean channelExists(String channel) { return nameToChann.containsKey(channel); }

    /** Checks if user is channel operator
     * @param connId id of the user's connection
     * @param channel channel name
     * @return true if user is channel operator
     */
    public boolean isChannelOperator(int connId, String channel) { return isUserInChannel(connId, channel) && nameToChann.get(channel).isOperator(connId); }

    /** Checks if user is in a channel
     * @param connId id of the user's connection
     * @param channel channel name
     * @return true if user is in a channel
     */
    public boolean isUserInChannel(int connId, String channel) { return nameToChann.containsKey(channel) && nameToChann.get(channel).isJoined(connId); }

    /** Get connIds of users in a channel
     * @param channel channel name
     * @return set of connIds of users in the channel
     */
    public Set<Integer> getChannelUsers(String channel) {
        if (channelExists(channel)) {
            return getChannel(channel).getUsers();
        } else {
            return new HashSet<>();
        }
    }

    /** Get number of users in a channel
     * @param channel channel name
     * @return number of users in the channel
     */
    public int getCount(String channel) {
        if (channelExists(channel)) {
            return getChannel(channel).count();
        } else {
            return 0;
        }
    }

    /** Add user to channel or create channel if it does not exist
     * @param connId id of the user's connection
     * @param channel channel name
     * @param key not used
     */
    public void join(int connId, String channel, String key) {
        if (channelExists(channel)) {
            getChannel(channel).join(connId);
        } else {
            addChannel(channel, connId);
        }
    }

    /** Remove user from a channel
     * @param connId id of the user's connection
     * @param channel channel name
     */
    public void leave(int connId, String channel) {
        if (channelExists(channel)) {
            Channel ch = getChannel(channel);
            ch.quit(connId);
            if (ch.count() == 0) {
                removeChannel(channel);
            }
        }
    }

    /** Removes a user from all channels
     * should be used for cleanup when user is disconnected
     * @param connId id of the user's connection
     */
    public void removeUser(int connId) {
        for (Channel channel: nameToChann.values()) {
            channel.quit(connId);
        }
    }

    /** Checks if topic is set
     * @param channel channel name
     * @return true if topic is set
     */
    public boolean isTopicSet(String channel) {
        if (channelExists(channel)) {
            return getChannel(channel).isTopicSet();
        } else {
            return false;
        }
    }

    /** Get channel topic
     * @param channel channel name
     * @return Channel topic string or empty string
     */
    public String getTopic(String channel) {
        if (channelExists(channel)) {
            return getChannel(channel).getTopic();
        } else {
            return "";
        }
    }

    /** Set channel topic
     * @param channel channel name
     * @param topic topic string
     */
    public void setTopic(String channel, String topic) {
        if (channelExists(channel)) {
            getChannel(channel).setTopic(topic);
        }
    }

    /** Clear channel topic
     * @param channel channel name
     */
    public void clearTopic(String channel) {
        if (channelExists(channel)) {
            getChannel(channel).clearTopic();
        }
    }
}
