package cz.cuni.mff.hrdinap1.ircserver;

import java.util.*;

public class ChannelManager {
    private class ChannelUser {
        public int connId;
        public boolean isOperator;

        public ChannelUser(int connId) {
            this.connId = connId;
            this.isOperator = false;
        }
    }

    private class Channel {
        private final Map<Integer, ChannelUser> users;
        String topic;
        // todo no external messages mode

        public Channel() {
            this.users = new HashMap<>();
        }

        public boolean isJoined(int connId) {
            return users.containsKey(connId);
        }

        public void join(int connId) {
            if (!users.containsKey(connId)) {
                users.put(connId, new ChannelUser(connId));
            }
        }

        public void quit(int connId) {
            users.remove(connId);
        }

        public void setOperator(int connId) {
            if (users.containsKey(connId)) {
                users.get(connId).isOperator = true;
            } else {
                System.out.println("Error user not in channel.");
            }
        }

        public Set<Integer> getUsers() {
            return users.keySet();
        }

        public int count() {
            return users.size();
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String newTopic) {
            topic = newTopic;
        }

        public void clearTopic() {
            topic = null;
        }

        public boolean isTopicSet() {
            return topic != null;
        }

    }

    private final Map<String, Channel> nameToChann;

    public ChannelManager() {
        nameToChann = new HashMap<>();
    }

    private Channel getChannel(String channel) {
        return nameToChann.getOrDefault(channel, null);
    }
    private void addChannel(String channel, int connId) {
        Channel newChannel = new Channel();
        newChannel.join(connId);
        newChannel.setOperator(connId);
        nameToChann.put(channel, newChannel);
    }

    public List<String> getChannels() {
        return nameToChann.keySet().stream().toList();
    }

    public boolean channelExists(String channel) { return nameToChann.containsKey(channel); }
    public boolean isUserInChannel(int connId, String channel) { return nameToChann.containsKey(channel) && nameToChann.get(channel).isJoined(connId); }

    public Set<Integer> getChannelUsers(String channel) {
        if (channelExists(channel)) {
            return getChannel(channel).getUsers();
        } else {
            return new HashSet<>();
        }
    }

    public int getCount(String channel) {
        if (channelExists(channel)) {
            return getChannel(channel).count();
        } else {
            return 0;
        }
    }

    public void join(int connId, String channel, String key) {
        if (channelExists(channel)) {
            getChannel(channel).join(connId);
        } else {
            addChannel(channel, connId);
        }
    }

    public void leave(int connId, String channel) {
        if (channelExists(channel)) {
            getChannel(channel).quit(connId);
        }
    }

    public void removeUser(int connId) {
        for (Channel channel: nameToChann.values()) {
            channel.quit(connId);
        }
    }

    public boolean isTopicSet(String channel) {
        if (channelExists(channel)) {
            return getChannel(channel).isTopicSet();
        } else {
            return false;
        }
    }

    public String getTopic(String channel) {
        if (channelExists(channel)) {
            return getChannel(channel).getTopic();
        } else {
            return "";
        }
    }

    public void setTopic(String channel, String topic) {
        if (channelExists(channel)) {
            getChannel(channel).setTopic(topic);
        }
    }

    public void clearTopic(String channel) {
        if (channelExists(channel)) {
            getChannel(channel).clearTopic();
        }
    }
}
