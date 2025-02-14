package cz.cuni.mff.hrdinap1.ircserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTests {
    private ChannelManager channelManager;
    private ConnectionManager connectionManager;
    private UserManager userManager;
    private IRCServer server;

    @BeforeEach
    public void setup() {
        channelManager = new ChannelManager();
        connectionManager = new ConnectionManager();
        userManager = new UserManager();
        server = new IRCServer("unit.test.server", channelManager, connectionManager, userManager);
    }

    private void connectUser(int connId, String nickname) {
        server.connect(connId);
        List<String> nick = Arrays.asList(nickname);
        server.cmdNick(nick, connId);
        List<String> user = Arrays.asList(nickname, "0", "*", ":Petr", "Hrdina");
        server.cmdUser(user, connId);
    }

    private void joinChannel(int connId, String channel) {
        List<String> join = Arrays.asList(channel);
        server.cmdJoin(join, connId);
    }

    private void setTopic(int connId, String channel, String topicText) {
        List<String> topic = Arrays.asList(channel, ":" + topicText);
        server.cmdTopic(topic, connId);
    }

    private void leaveChannel(int connId, String channel) {
        List<String> part = Arrays.asList(channel);
        server.cmdPart(part, connId);
    }

    private void kickUser(int operator, String channel, String kicked) {
        List<String> kick = Arrays.asList(channel, kicked);
        server.cmdKick(kick, operator);
    }

    @Test
    public void testRegistration() {
        connectUser(0, "MFF");
        assertTrue(userManager.userIsRegistered(0));
    }

    @Test
    public void testChannel() {
        String channel = "#matfyz";
        connectUser(0, "MFF");
        connectUser(1, "MFF2");
        joinChannel(0, channel);
        joinChannel(1, channel);
        assertTrue(channelManager.channelExists(channel));
        assertTrue(channelManager.isChannelOperator(0, channel));
        assertTrue(channelManager.isUserInChannel(0, channel));
        assertTrue(channelManager.isUserInChannel(1, channel));
        assertEquals(2, channelManager.getCount(channel));
        assertFalse(channelManager.isTopicSet(channel));
    }

    @Test
    public void testTopic() {
        String channel = "#matfyz";
        connectUser(0, "MFF");
        joinChannel(0, channel);
        String topic = "MATFYZjeNEJ";
        setTopic(0, channel, topic);
        assertTrue(channelManager.isTopicSet(channel));
        assertEquals(topic, channelManager.getTopic(channel));
        setTopic(0, channel, "");
        assertFalse(channelManager.isTopicSet(channel));
    }

    @Test
    public void testLeave() {
        String channel = "#matfyz";
        connectUser(0, "MFF");
        connectUser(1, "MFF2");
        joinChannel(0, channel);
        joinChannel(1, channel);
        assertTrue(channelManager.channelExists(channel));
        assertEquals(2, channelManager.getCount(channel));

        leaveChannel(1, channel);
        assertEquals(1, channelManager.getCount(channel));

        leaveChannel(0, channel);
        assertFalse(channelManager.channelExists(channel));
    }

    @Test
    public void testKick() {
        String channel = "#matfyz";
        connectUser(0, "MFF");
        connectUser(1, "MFF2");
        joinChannel(0, channel);
        joinChannel(1, channel);
        assertTrue(channelManager.channelExists(channel));
        assertEquals(2, channelManager.getCount(channel));

        kickUser(0, "#matfyz", "MFF2");
        assertFalse(channelManager.isUserInChannel(1, channel));
    }

    @Test
    public void testCleanup() {
        String channel = "#matfyz";
        connectUser(0, "MFF");
        connectUser(1, "MFF2");
        joinChannel(0, channel);
        joinChannel(1, channel);
        server.disconnect(1);
        assertFalse(userManager.userIsRegistered(1));
        assertFalse(channelManager.isUserInChannel(1, channel));

        server.disconnect(0);
        assertFalse(userManager.userIsRegistered(0));
        assertFalse(channelManager.channelExists(channel));
    }
}
