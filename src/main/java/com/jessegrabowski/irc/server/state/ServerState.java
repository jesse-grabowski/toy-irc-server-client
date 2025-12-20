/*
 * This project is licensed under the MIT License.
 *
 * In addition to the rights granted under the MIT License, explicit permission
 * is granted to the faculty, instructors, teaching assistants, and evaluators
 * of Ritsumeikan University for unrestricted educational evaluation and grading.
 *
 * ---------------------------------------------------------------------------
 *
 * MIT License
 *
 * Copyright (c) 2026 Jesse Grabowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jessegrabowski.irc.server.state;

import com.jessegrabowski.irc.network.IRCConnection;
import com.jessegrabowski.irc.protocol.IRCCapability;
import com.jessegrabowski.irc.protocol.IRCChannelMembershipMode;
import com.jessegrabowski.irc.protocol.model.IRCMessage401;
import com.jessegrabowski.irc.protocol.model.IRCMessage403;
import com.jessegrabowski.irc.protocol.model.IRCMessage405;
import com.jessegrabowski.irc.protocol.model.IRCMessage432;
import com.jessegrabowski.irc.protocol.model.IRCMessage433;
import com.jessegrabowski.irc.protocol.model.IRCMessage442;
import com.jessegrabowski.irc.protocol.model.IRCMessage451;
import com.jessegrabowski.irc.protocol.model.IRCMessage462;
import com.jessegrabowski.irc.protocol.model.IRCMessage476;
import com.jessegrabowski.irc.protocol.model.IRCMessage482;
import com.jessegrabowski.irc.server.IRCServerParameters;
import com.jessegrabowski.irc.server.IRCServerProperties;
import com.jessegrabowski.irc.util.Pair;
import com.jessegrabowski.irc.util.Transaction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ServerState {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z]+[a-z0-9_-]*$");

    private static final int REAL_NAME_MAX_LENGTH = 50;

    private final Map<IRCConnection, ServerUser> users = new HashMap<>();
    private final Map<ServerUser, IRCConnection> connectionsByUser = new HashMap<>();
    private final Map<String, ServerUser> usersByNickname = new HashMap<>();

    private final Map<String, ServerChannel> channels = new HashMap<>();

    private final IRCServerProperties properties;

    private IRCServerParameters parameters;

    public ServerState(IRCServerProperties properties, IRCServerParameters parameters) {
        this.properties = properties;
        this.parameters = parameters;
    }

    public Set<IRCConnection> getConnections() {
        return Set.copyOf(users.keySet());
    }

    public ServerUser getUserForConnection(IRCConnection connection) {
        ServerUser user = findUser(connection);
        if (user == null) {
            throw new IllegalStateException("User not found for connection");
        }
        return user;
    }

    public IRCConnection getConnectionForUser(ServerUser user) {
        IRCConnection connection = connectionsByUser.get(user);
        if (connection == null) {
            throw new IllegalStateException("Connection not found for user");
        }
        return connection;
    }

    public void markQuit(IRCConnection connection, String quitMessage) {
        ServerUser user = findUser(connection);
        user.setQuitMessage(quitMessage);
        user.setState(ServerConnectionState.QUITTING);
    }

    public void setAway(IRCConnection connection, String awayStatus) throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        user.setAwayStatus(awayStatus);
    }

    public String getAway(String nickname) {
        ServerUser user = findUser(nickname);
        return user != null ? user.getAwayStatus() : null;
    }

    private ServerUser findUser(IRCConnection connection) {
        return users.get(connection);
    }

    private ServerUser findUser(String nickname) {
        return usersByNickname.get(normalizeNickname(nickname));
    }

    public IRCServerParameters getParameters() {
        return parameters;
    }

    public void setParameters(IRCServerParameters parameters) {
        IRCServerParameters oldParameters = this.parameters;
        Transaction.addCompensation(() -> this.parameters = oldParameters);
        this.parameters = parameters;
    }

    public void connect(IRCConnection connection) {
        if (users.containsKey(connection)) {
            return;
        }

        ServerUser user = new ServerUser();
        user.setPasswordEntered(properties.getPassword() == null);
        Transaction.putTransactionally(users, connection, user);
        Transaction.putTransactionally(connectionsByUser, user, connection);
    }

    public void disconnect(IRCConnection connection) {
        if (!users.containsKey(connection)) {
            return;
        }

        ServerUser user = Transaction.removeTransactionally(users, connection);
        Transaction.removeTransactionally(connectionsByUser, user);
        if (user.getNickname() != null) {
            Transaction.removeTransactionally(usersByNickname, user.getNickname());
        }
        for (ServerChannel channel : user.getChannels()) {
            channel.part(user);
            if (channel.isEmpty()) {
                Transaction.removeTransactionally(channels, channel.getName());
            }
        }
    }

    public void startCapabilityNegotiation(IRCConnection connection) {
        ServerUser user = findUser(connection);
        user.setNegotiatingCapabilities(true);
    }

    public void addCapability(IRCConnection connection, IRCCapability capability) {
        ServerUser user = findUser(connection);
        user.addCapability(capability);
    }

    public void removeCapability(IRCConnection connection, IRCCapability capability) {
        ServerUser user = findUser(connection);
        user.removeCapability(capability);
    }

    public boolean hasCapability(IRCConnection connection, IRCCapability capability) {
        ServerUser user = findUser(connection);
        return user.hasCapability(capability);
    }

    public void endCapabilityNegotiation(IRCConnection connection) {
        ServerUser user = findUser(connection);
        user.setNegotiatingCapabilities(false);
    }

    public long getLastPing(IRCConnection connection) {
        ServerUser user = findUser(connection);
        return user.getLastPinged();
    }

    public void setLastPing(IRCConnection connection, long timestamp) {
        ServerUser user = findUser(connection);
        user.setLastPinged(timestamp);
    }

    public long getLastPong(IRCConnection connection) {
        ServerUser user = findUser(connection);
        return user.getLastPonged();
    }

    public void setLastPong(IRCConnection connection, long timestamp) {
        ServerUser user = findUser(connection);
        user.setLastPonged(timestamp);
    }

    public String getNickname(IRCConnection connection) {
        ServerUser user = findUser(connection);
        String nickname = user.getNickname();
        return nickname != null ? nickname : "*";
    }

    public Pair<String, String> setNickname(IRCConnection connection, String nickname)
            throws StateInvariantException, InvalidPasswordException, NoOpException {
        String newNickname = normalizeNickname(nickname);

        ServerUser user = findUser(connection);
        if (user == null) {
            throw new StateInvariantException("Not registered", newNickname, "Not registered", IRCMessage451::new);
        }

        validateNickname(user.getNickname(), newNickname);

        if (!user.isPasswordEntered()) {
            throw new InvalidPasswordException("PASS not yet entered");
        }

        String oldNickname = user.getNickname();

        if (Objects.equals(oldNickname, nickname)) {
            throw new NoOpException();
        }

        if (usersByNickname.containsKey(newNickname) && !Objects.equals(normalizeNickname(oldNickname), newNickname)) {
            throw new StateInvariantException(
                    "NICK %s already in use".formatted(newNickname),
                    oldNickname != null ? oldNickname : newNickname,
                    newNickname,
                    IRCMessage433::new);
        }

        if (oldNickname != null) {
            Transaction.removeTransactionally(usersByNickname, oldNickname);
        }
        user.setNickname(nickname);
        Transaction.putTransactionally(usersByNickname, newNickname, user);

        return new Pair<>(oldNickname, nickname);
    }

    public void setUserInfo(IRCConnection connection, String username, String realName)
            throws StateInvariantException, InvalidPasswordException {
        ServerUser user = findUser(connection);
        if (user.getState() != ServerConnectionState.NEW) {
            throw new StateInvariantException("Already registered", user.getNickname(), IRCMessage462::new);
        }

        if (!user.isPasswordEntered()) {
            throw new InvalidPasswordException("PASS not yet entered");
        }

        String newUsername = username;
        if (newUsername.length() > parameters.getUserLength()) {
            newUsername = newUsername.substring(0, parameters.getUserLength());
        }

        String newRealName = realName;
        if (newRealName.length() > REAL_NAME_MAX_LENGTH) {
            newRealName = newRealName.substring(0, REAL_NAME_MAX_LENGTH);
        }

        user.setUsername(newUsername);
        user.setRealName(newRealName);
    }

    public void checkPassword(IRCConnection connection, String password)
            throws StateInvariantException, InvalidPasswordException {
        ServerUser user = findUser(connection);
        if (user.getState() != ServerConnectionState.NEW) {
            throw new StateInvariantException("Already registered", user.getNickname(), IRCMessage462::new);
        }

        if (Objects.equals(password, properties.getPassword())) {
            user.setPasswordEntered(true);
        } else {
            throw new InvalidPasswordException("Invalid password");
        }
    }

    public boolean isRegistered(IRCConnection connection) {
        ServerUser user = findUser(connection);
        return user.getState() == ServerConnectionState.REGISTERED;
    }

    public boolean tryFinishRegistration(IRCConnection connection) {
        ServerUser user = findUser(connection);
        if (user.getState() != ServerConnectionState.NEW) {
            return false;
        }

        if (!user.isPasswordEntered()
                || user.isNegotiatingCapabilities()
                || user.getNickname() == null
                || user.getUsername() == null
                || user.getRealName() == null) {
            return false;
        }

        user.setState(ServerConnectionState.REGISTERED);
        return true;
    }

    public MessageTarget resolveMask(IRCConnection connection, String mask) throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        if (parameters.getChannelTypes().stream().anyMatch(c -> mask.charAt(0) == c)) {
            ServerChannel channel = findChannel(mask);
            if (channel == null) {
                throw new StateInvariantException(
                        "Could not find channel '%s'".formatted(mask),
                        user.getNickname(),
                        mask,
                        "No such channel '%s'".formatted(mask),
                        IRCMessage403::new);
            }

            Set<IRCConnection> connections = new HashSet<>();
            for (ServerUser channelUser : channel.getMembers()) {
                if (channelUser != user) {
                    connections.add(getConnectionForUser(channelUser));
                }
            }
            return new MessageTarget(mask, connections);
        }

        ServerUser targetUser = findUser(mask);
        if (targetUser == null || targetUser.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException(
                    "Could not find user '%s'".formatted(mask),
                    user.getNickname(),
                    mask,
                    "No such nick '%s'".formatted(mask),
                    IRCMessage401::new);
        }

        return new MessageTarget(mask, Set.of(getConnectionForUser(targetUser)));
    }

    public MessageTarget getWatchers(ServerChannel channel) {
        return new MessageTarget(
                channel.getName(),
                channel.getMembers().stream().map(this::getConnectionForUser).collect(Collectors.toSet()));
    }

    public MessageTarget getWatchers(ServerUser user, boolean includeSelf) {
        Set<IRCConnection> watchers = new HashSet<>();
        watchers.add(getConnectionForUser(user));
        for (ServerChannel channel : user.getChannels()) {
            watchers.addAll(channel.getMembers().stream()
                    .map(this::getConnectionForUser)
                    .collect(Collectors.toSet()));
        }
        if (!includeSelf) {
            watchers.remove(getConnectionForUser(user));
        }
        return new MessageTarget(user.getNickname(), watchers);
    }

    public ServerChannel findChannel(String channelName) {
        return channels.get(normalizeChannelName(channelName));
    }

    public ServerChannel getExistingChannel(IRCConnection connection, String channelName)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        String normalizedChannelName = normalizeChannelName(channelName);

        ServerChannel channel = channels.get(normalizedChannelName);
        if (channel == null) {
            throw new StateInvariantException(
                    "Channel '%s' does not exist".formatted(normalizedChannelName),
                    user.getNickname(),
                    normalizedChannelName,
                    "Channel '%s' does not exist".formatted(normalizedChannelName),
                    IRCMessage403::new);
        }

        return channel;
    }

    public void setChannelTopic(IRCConnection connection, ServerChannel channel, String topic)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        if (!channel.getMembers().contains(user)) {
            throw new StateInvariantException(
                    "Not a member of channel '%s'".formatted(channel.getName()),
                    user.getNickname(),
                    channel.getName(),
                    IRCMessage442::new);
        }

        if (channel.checkFlag('t') && !channel.getMembership(user).hasAtLeast(IRCChannelMembershipMode.HALFOP)) {
            throw new StateInvariantException(
                    "User does not have permission to set topic",
                    user.getNickname(),
                    channel.getName(),
                    "Protected topic mode enabled, halfop required to set topic",
                    IRCMessage482::new);
        }

        channel.setTopic(topic);
        channel.setTopicSetAt(System.currentTimeMillis());
        channel.setTopicSetBy(new ServerSetBy.SetByUser(user));
    }

    public void joinChannel(IRCConnection connection, String channelName, String key) throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        String normalizedChannelName = normalizeChannelName(channelName);

        validateChannelName(user.getNickname(), normalizedChannelName);

        char prefix = normalizedChannelName.charAt(0);
        int limit = parameters.getChannelLimits().getOrDefault(prefix, Integer.MAX_VALUE);
        if (user.getChannels().stream()
                        .filter(c -> c.getName().charAt(0) == prefix)
                        .count()
                >= limit) {
            throw new StateInvariantException(
                    "User is already in too many channels",
                    user.getNickname(),
                    normalizedChannelName,
                    "Channel limit (%s:%d) exceeded".formatted(prefix, limit),
                    IRCMessage405::new);
        }

        ServerChannel channel = channels.get(normalizedChannelName);
        if (channel == null) {
            channel = new ServerChannel(normalizedChannelName);
            Transaction.putTransactionally(channels, normalizedChannelName, channel);
            channel.join(user, key);
            channel.addMemberModeAutomatic(user, IRCChannelMembershipMode.OPERATOR);
        } else {
            channel.join(user, key);
        }
        user.addChannel(channel);
    }

    public void partChannel(IRCConnection connection, String channelName) throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        String normalizedChannelName = normalizeChannelName(channelName);

        validateChannelName(user.getNickname(), normalizedChannelName);

        ServerChannel channel = channels.get(normalizedChannelName);
        if (channel == null || !channel.getMembers().contains(user)) {
            return;
        }

        channel.part(user);
        user.removeChannel(channel);
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) {
            return null;
        }
        String newNickname = parameters.getCaseMapping().normalizeNickname(nickname);
        if (newNickname.length() > parameters.getNickLength()) {
            newNickname = newNickname.substring(0, parameters.getNickLength());
        }
        return newNickname;
    }

    private void validateNickname(String client, String nickname) throws StateInvariantException {
        if (parameters.getChannelTypes().stream().anyMatch(c -> nickname.charAt(0) == c)) {
            throw new StateInvariantException(
                    "Nickname '%s' looks like channel".formatted(nickname),
                    client != null ? client : nickname,
                    nickname,
                    IRCMessage432::new);
        } else if (!NAME_PATTERN.matcher(nickname).matches()) {
            throw new StateInvariantException(
                    "Nickname '%s' should be only ascii".formatted(nickname),
                    client != null ? client : nickname,
                    nickname,
                    IRCMessage432::new);
        }
    }

    private String normalizeChannelName(String channelName) {
        String normalizedChannelName = parameters.getCaseMapping().normalizeChannel(channelName);
        if (normalizedChannelName.length() > parameters.getChannelLength()) {
            normalizedChannelName = normalizedChannelName.substring(0, parameters.getChannelLength());
        }
        return normalizedChannelName;
    }

    private void validateChannelName(String client, String channel) throws StateInvariantException {
        char prefix = channel.charAt(0);
        if (!parameters.getChannelTypes().contains(prefix)) {
            throw new StateInvariantException(
                    "Channel '%s' mask not valid".formatted(channel),
                    client,
                    channel,
                    "Channel name must start with one of '%s'".formatted(parameters.getChannelTypes()),
                    IRCMessage476::new);
        }
        String name = channel.substring(1);
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new StateInvariantException(
                    "Channel '%s' mask not valid".formatted(channel),
                    client,
                    channel,
                    "Channel name can only contain letters and numbers",
                    IRCMessage476::new);
        }
    }
}
