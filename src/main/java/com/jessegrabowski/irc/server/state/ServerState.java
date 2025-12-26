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
import com.jessegrabowski.irc.protocol.IRCChannelFlag;
import com.jessegrabowski.irc.protocol.IRCChannelList;
import com.jessegrabowski.irc.protocol.IRCChannelMembershipMode;
import com.jessegrabowski.irc.protocol.IRCChannelSetting;
import com.jessegrabowski.irc.protocol.IRCUserMode;
import com.jessegrabowski.irc.protocol.model.IRCMessage401;
import com.jessegrabowski.irc.protocol.model.IRCMessage403;
import com.jessegrabowski.irc.protocol.model.IRCMessage405;
import com.jessegrabowski.irc.protocol.model.IRCMessage432;
import com.jessegrabowski.irc.protocol.model.IRCMessage433;
import com.jessegrabowski.irc.protocol.model.IRCMessage441;
import com.jessegrabowski.irc.protocol.model.IRCMessage442;
import com.jessegrabowski.irc.protocol.model.IRCMessage451;
import com.jessegrabowski.irc.protocol.model.IRCMessage462;
import com.jessegrabowski.irc.protocol.model.IRCMessage471;
import com.jessegrabowski.irc.protocol.model.IRCMessage473;
import com.jessegrabowski.irc.protocol.model.IRCMessage474;
import com.jessegrabowski.irc.protocol.model.IRCMessage475;
import com.jessegrabowski.irc.protocol.model.IRCMessage476;
import com.jessegrabowski.irc.protocol.model.IRCMessage482;
import com.jessegrabowski.irc.protocol.model.IRCMessage502;
import com.jessegrabowski.irc.protocol.model.IRCMessage696;
import com.jessegrabowski.irc.server.IRCServerParameters;
import com.jessegrabowski.irc.server.IRCServerProperties;
import com.jessegrabowski.irc.util.Glob;
import com.jessegrabowski.irc.util.Pair;
import com.jessegrabowski.irc.util.Transaction;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
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
    private final Deque<ServerUserWas> nicknameHistory = new ArrayDeque<>();

    private final Map<String, ServerChannel> channels = new HashMap<>();

    private final IRCServerProperties properties;

    private IRCServerParameters parameters;
    private int userCount = 0;
    private int maxUserCount = 0;

    public ServerState(IRCServerProperties properties, IRCServerParameters parameters) {
        this.properties = properties;
        this.parameters = parameters;
    }

    public Set<IRCConnection> getConnections() {
        return Set.copyOf(users.keySet());
    }

    public int getUserCount() {
        return userCount;
    }

    public int getMaxUserCount() {
        return maxUserCount;
    }

    public int getInvisibleUserCount() {
        return (int) users.values().stream()
                .filter(u -> u.getModes().contains(IRCUserMode.INVISIBLE))
                .count();
    }

    public int getOperatorCount() {
        return (int) users.values().stream()
                .filter(u -> u.getModes().contains(IRCUserMode.OPERATOR))
                .count();
    }

    public int getUnknownConnectionCount() {
        return (int) users.values().stream()
                .filter(u -> u.getState() == ServerConnectionState.NEW)
                .count();
    }

    public int getChannelCount() {
        return channels.size();
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
        if (user.getState() == ServerConnectionState.REGISTERED) {
            user.setState(ServerConnectionState.QUITTING);
        }
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

    public boolean userHasMode(String nickname, IRCUserMode mode) {
        ServerUser user = findUser(nickname);
        return user != null && user.getModes().contains(mode);
    }

    public void setChannelFlag(IRCConnection connection, String channelName, IRCChannelFlag flag)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannel channel = getExistingChannel(connection, channelName);
        ServerChannelMembership membership = channel.getMembership(user);
        if (membership == null || !membership.hasAtLeast(IRCChannelMembershipMode.OPERATOR)) {
            throw new StateInvariantException(
                    "Cannot set modes on channel",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot set modes on channel",
                    IRCMessage482::new);
        }

        channel.setFlag(flag);
    }

    public void clearChannelFlag(IRCConnection connection, String channelName, IRCChannelFlag flag)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannel channel = getExistingChannel(connection, channelName);
        ServerChannelMembership membership = channel.getMembership(user);
        if (membership == null || !membership.hasAtLeast(IRCChannelMembershipMode.OPERATOR)) {
            throw new StateInvariantException(
                    "Cannot set modes on channel",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot set modes on channel",
                    IRCMessage482::new);
        }

        channel.clearFlag(flag);
    }

    public void setChannelSetting(IRCConnection connection, String channelName, IRCChannelSetting setting, String value)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannel channel = getExistingChannel(connection, channelName);
        ServerChannelMembership membership = channel.getMembership(user);
        if (membership == null || !membership.hasAtLeast(IRCChannelMembershipMode.OPERATOR)) {
            throw new StateInvariantException(
                    "Cannot set modes on channel",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot set modes on channel",
                    IRCMessage482::new);
        }

        if (value == null || value.isBlank() || value.contains(" ") || value.length() > 50) {
            throw new StateInvariantException(
                    "Invalid mode param",
                    user.getNickname(),
                    channel.getName(),
                    setting.getMode(),
                    "*",
                    "Cannot set modes on channel",
                    IRCMessage696::new);
        }

        boolean valid =
                switch (setting) {
                    case CLIENT_LIMIT -> {
                        try {
                            int limit = Integer.parseInt(value);
                            yield limit > 0;
                        } catch (NumberFormatException e) {
                            yield false;
                        }
                    }
                    case KEY -> value.matches("[a-zA-Z0-9]{1,32}");
                };

        if (!valid) {
            throw new StateInvariantException(
                    "Invalid mode param",
                    user.getNickname(),
                    channel.getName(),
                    setting.getMode(),
                    value,
                    "Cannot set modes on channel",
                    IRCMessage696::new);
        }

        channel.setSetting(setting, value);
    }

    public void clearChannelSetting(
            IRCConnection connection, String channelName, IRCChannelSetting setting, String value)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannel channel = getExistingChannel(connection, channelName);
        ServerChannelMembership membership = channel.getMembership(user);
        if (membership == null || !membership.hasAtLeast(IRCChannelMembershipMode.OPERATOR)) {
            throw new StateInvariantException(
                    "Cannot clear modes on channel",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot clear modes on channel",
                    IRCMessage482::new);
        }

        boolean valid =
                switch (setting) {
                    case CLIENT_LIMIT -> true;
                    case KEY -> Objects.equals(value, channel.getSetting(IRCChannelSetting.KEY));
                };

        if (!valid) {
            throw new StateInvariantException(
                    "Invalid mode param",
                    user.getNickname(),
                    channel.getName(),
                    setting.getMode(),
                    value,
                    "Cannot clear modes on channel",
                    IRCMessage696::new);
        }

        channel.removeSetting(setting);
    }

    public void addToChannelList(IRCConnection connection, String channelName, IRCChannelList list, String value)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannel channel = getExistingChannel(connection, channelName);
        ServerChannelMembership membership = channel.getMembership(user);
        if (membership == null || !membership.hasAtLeast(IRCChannelMembershipMode.OPERATOR)) {
            throw new StateInvariantException(
                    "Cannot set modes on channel",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot set modes on channel",
                    IRCMessage482::new);
        }

        if (value == null || value.isBlank() || value.contains(" ") || value.length() > 50) {
            throw new StateInvariantException(
                    "Invalid mode param",
                    user.getNickname(),
                    channel.getName(),
                    list.getMode(),
                    "*",
                    "Cannot set modes on channel",
                    IRCMessage696::new);
        }

        Glob glob = Glob.of(value).casefold(parameters.getCaseMapping());
        if (!channel.getList(list).contains(glob)) {
            channel.addToList(list, glob);
        }
    }

    public void removeFromChannelList(IRCConnection connection, String channelName, IRCChannelList list, String value)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannel channel = getExistingChannel(connection, channelName);
        ServerChannelMembership membership = channel.getMembership(user);
        if (membership == null || !membership.hasAtLeast(IRCChannelMembershipMode.OPERATOR)) {
            throw new StateInvariantException(
                    "Cannot set modes on channel",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot set modes on channel",
                    IRCMessage482::new);
        }

        Glob glob = Glob.of(value).casefold(parameters.getCaseMapping());
        if (channel.getList(list).contains(glob)) {
            channel.removeFromList(list, glob);
        }
    }

    public void setUserMode(IRCConnection connection, String nickname, IRCUserMode mode)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        if (!Objects.equals(user.getNickname(), nickname)) {
            throw new StateInvariantException(
                    "Cannot set modes for other users",
                    user.getNickname(),
                    "Cannot change modes for other users",
                    IRCMessage502::new);
        }

        user.addMode(mode);
    }

    public void clearUserMode(IRCConnection connection, String nickname, IRCUserMode mode)
            throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        if (!Objects.equals(user.getNickname(), nickname)) {
            throw new StateInvariantException(
                    "Cannot set modes for other users",
                    user.getNickname(),
                    "Cannot change modes for other users",
                    IRCMessage502::new);
        }

        user.removeMode(mode);
    }

    public void setChannelMembershipMode(
            IRCConnection connection, String channelName, String nickname, IRCChannelMembershipMode mode)
            throws StateInvariantException {
        ServerUser me = findUser(connection);
        if (me == null || me.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannel channel = getExistingChannel(connection, channelName);
        ServerChannelMembership myMembership = channel.getMembership(me);
        if (myMembership == null || !myMembership.canGrant(mode)) {
            throw new StateInvariantException(
                    "Cannot set modes on channel",
                    me.getNickname(),
                    channel.getName(),
                    "Cannot set modes on channel",
                    IRCMessage482::new);
        }

        ServerUser target = findUser(nickname);
        if (target == null) {
            throw new StateInvariantException(
                    "User not found", me.getNickname(), nickname, "No such nick", IRCMessage401::new);
        }

        ServerChannelMembership membership = channel.getMembership(target);
        if (membership == null) {
            throw new StateInvariantException(
                    "User not in channel",
                    me.getNickname(),
                    target.getNickname(),
                    channel.getName(),
                    IRCMessage441::new);
        }

        membership.addMode(mode);
    }

    public void clearChannelMembershipMode(
            IRCConnection connection, String channelName, String nickname, IRCChannelMembershipMode mode)
            throws StateInvariantException {
        ServerUser me = findUser(connection);
        if (me == null || me.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannel channel = getExistingChannel(connection, channelName);
        ServerChannelMembership myMembership = channel.getMembership(me);
        if (myMembership == null || !myMembership.canGrant(mode)) {
            throw new StateInvariantException(
                    "Cannot set modes on channel",
                    me.getNickname(),
                    channel.getName(),
                    "Cannot set modes on channel",
                    IRCMessage482::new);
        }

        ServerUser target = findUser(nickname);
        if (target == null) {
            throw new StateInvariantException(
                    "User not found", me.getNickname(), nickname, "No such nick", IRCMessage401::new);
        }

        ServerChannelMembership membership = channel.getMembership(target);
        if (membership == null) {
            throw new StateInvariantException(
                    "User not in channel",
                    me.getNickname(),
                    target.getNickname(),
                    channel.getName(),
                    IRCMessage441::new);
        }

        membership.removeMode(mode);
    }

    public String getHost(IRCConnection connection, String nickname) throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        if (Objects.equals(user.getNickname(), normalizeNickname(nickname))) {
            return user.getHostAddress();
        } else {
            return properties.getHost();
        }
    }

    private ServerUser findUser(IRCConnection connection) {
        return users.get(connection);
    }

    public ServerUser findUser(String nickname) {
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

        ServerUser user = new ServerUser(connection.getHostAddress());
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
        for (ServerChannel channel : user.getInvitedChannels()) {
            clearChannelInvite(channel, user);
        }
        if (user.getNickname() != null && user.getUsername() != null && user.getRealName() != null) {
            Transaction.addFirstTransactionally(
                    nicknameHistory,
                    new ServerUserWas(
                            normalizeNickname(user.getNickname()),
                            user.getUsername(),
                            user.getHostAddress(),
                            user.getRealName()));
            while (nicknameHistory.size() > properties.getMaxNicknameHistory()) {
                Transaction.removeLastTransactionally(nicknameHistory);
            }
        }
        if (user.getState() != ServerConnectionState.NEW) {
            userCount--;
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
        String truncatedNickname = nickname;
        if (truncatedNickname.length() > parameters.getNickLength()) {
            truncatedNickname = truncatedNickname.substring(0, parameters.getNickLength());
        }
        String normalizedNickname = normalizeNickname(nickname);

        ServerUser user = findUser(connection);
        if (user == null) {
            throw new StateInvariantException(
                    "Not registered", normalizedNickname, "Not registered", IRCMessage451::new);
        }

        validateNickname(user.getNickname(), normalizedNickname);

        if (!user.isPasswordEntered()) {
            throw new InvalidPasswordException("PASS not yet entered");
        }

        String oldNickname = user.getNickname();

        if (Objects.equals(oldNickname, truncatedNickname)) {
            throw new NoOpException();
        }

        String normalizedOldNickname = normalizeNickname(oldNickname);

        if (usersByNickname.containsKey(normalizedNickname)
                && !Objects.equals(normalizedOldNickname, normalizedNickname)) {
            throw new StateInvariantException(
                    "NICK %s already in use".formatted(normalizedNickname),
                    oldNickname != null ? oldNickname : normalizedNickname,
                    normalizedNickname,
                    IRCMessage433::new);
        }

        if (normalizedOldNickname != null) {
            Transaction.removeTransactionally(usersByNickname, normalizedOldNickname);
        }
        user.setNickname(truncatedNickname);
        Transaction.putTransactionally(usersByNickname, normalizedNickname, user);

        if (normalizedOldNickname != null && !Objects.equals(normalizedOldNickname, normalizedNickname)) {
            Transaction.addFirstTransactionally(
                    nicknameHistory,
                    new ServerUserWas(
                            normalizedOldNickname, user.getUsername(), user.getHostAddress(), user.getRealName()));
            while (nicknameHistory.size() > properties.getMaxNicknameHistory()) {
                Transaction.removeLastTransactionally(nicknameHistory);
            }
        }

        return new Pair<>(oldNickname, truncatedNickname);
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

        userCount++;
        maxUserCount = Math.max(userCount, maxUserCount);
        user.setState(ServerConnectionState.REGISTERED);
        return true;
    }

    public MessageTarget resolveRequired(IRCConnection connection, String mask) throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        MessageTarget target = resolveOptional(connection, mask);
        if (!target.isEmpty()) {
            return target;
        }

        if (target.isChannel()) {
            throw new StateInvariantException(
                    "Could not find channel '%s'".formatted(mask),
                    user.getNickname(),
                    mask,
                    "No such channel '%s'".formatted(mask),
                    IRCMessage403::new);
        }

        throw new StateInvariantException(
                "Could not find user '%s'".formatted(mask),
                user.getNickname(),
                mask,
                "No such nick '%s'".formatted(mask),
                IRCMessage401::new);
    }

    public MessageTarget resolveOptional(IRCConnection connection, String mask) throws StateInvariantException {
        ServerUser user = findUser(connection);
        if (user == null || user.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        Glob glob = Glob.of(mask).casefold(parameters.getCaseMapping());

        if (parameters.getChannelTypes().stream().anyMatch(c -> mask.charAt(0) == c)) {
            return new MessageTarget(
                    mask,
                    true,
                    glob.isLiteral(),
                    Set.of(),
                    channels.values().stream()
                            .filter(c -> glob.matches(normalizeChannelName(c.getName())))
                            .collect(Collectors.toSet()));
        }

        return new MessageTarget(
                mask,
                false,
                glob.isLiteral(),
                users.values().stream()
                        .filter(u -> glob.matches(normalizeNickname(u.getNickname())))
                        .collect(Collectors.toSet()),
                Set.of());
    }

    public MessageTarget getWatchers(ServerChannel channel) {
        return new MessageTarget(channel.getName(), true, true, Set.of(), Set.of(channel));
    }

    public MessageTarget getWatchers(ServerUser user) {
        return new MessageTarget(user.getNickname(), false, true, Set.of(user), user.getChannels());
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

        ServerChannel channel = findChannel(channelName);
        if (channel == null) {
            throw new StateInvariantException(
                    "Channel '%s' does not exist".formatted(channelName),
                    user.getNickname(),
                    channelName,
                    "Channel '%s' does not exist".formatted(channelName),
                    IRCMessage403::new);
        }

        return channel;
    }

    public ServerUser getExistingUser(IRCConnection connection, String nickname) throws StateInvariantException {
        ServerUser me = findUser(connection);
        if (me == null || me.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerUser user = findUser(nickname);
        if (user == null) {
            throw new StateInvariantException(
                    "User '%s' does not exist".formatted(nickname),
                    me.getNickname(),
                    nickname,
                    "No such nickname",
                    IRCMessage401::new);
        }

        return user;
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

        if (channel.checkFlag(IRCChannelFlag.TOPIC)
                && !channel.getMembership(user).hasAtLeast(IRCChannelMembershipMode.HALFOP)) {
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
            user.addChannel(channel);
            return;
        }

        if (channel.isBanned(parameters, user)) {
            throw new StateInvariantException(
                    "User is banned from channel",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot join channel (banned +%s)".formatted(IRCChannelList.BAN.getMode()),
                    IRCMessage474::new);
        }

        if (!channel.isInvited(parameters, user)) {
            throw new StateInvariantException(
                    "Invite only channel",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot join channel (invite-only +%s)".formatted(IRCChannelFlag.INVITE_ONLY.getMode()),
                    IRCMessage473::new);
        }

        if (!channel.isKeyValid(key)) {
            throw new StateInvariantException(
                    "Invalid key",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot join channel (key +%s)".formatted(IRCChannelSetting.KEY.getMode()),
                    IRCMessage475::new);
        }

        if (channel.isFull()) {
            throw new StateInvariantException(
                    "Channel is full",
                    user.getNickname(),
                    channel.getName(),
                    "Cannot join channel (client limit +%s)".formatted(IRCChannelSetting.CLIENT_LIMIT.getMode()),
                    IRCMessage471::new);
        }

        channel.join(user, key);
        user.addChannel(channel);
        clearChannelInvite(channel, user);
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

    public void kickFromChannel(IRCConnection connection, ServerChannel channel, ServerUser target)
            throws StateInvariantException {
        ServerUser me = findUser(connection);
        if (me == null || me.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        ServerChannelMembership myMembership = channel.getMembership(me);
        if (myMembership == null) {
            throw new StateInvariantException(
                    "Not on channel", me.getNickname(), channel.getName(), IRCMessage442::new);
        }

        if (!myMembership.hasAtLeast(IRCChannelMembershipMode.OPERATOR)) {
            throw new StateInvariantException(
                    "Cannot kick in channel",
                    me.getNickname(),
                    channel.getName(),
                    "Cannot kick in channel (requires operator)",
                    IRCMessage482::new);
        }

        ServerChannelMembership targetMembership = channel.getMembership(target);
        if (targetMembership == null) {
            throw new StateInvariantException(
                    "Cannot kick user from channel",
                    me.getNickname(),
                    channel.getName(),
                    "Cannot kick user from channel (not in channel)",
                    IRCMessage441::new);
        }

        if (!myMembership.getHighestPowerMode().isAtLeast(targetMembership.getHighestPowerMode())) {
            throw new StateInvariantException(
                    "Cannot kick in channel",
                    me.getNickname(),
                    channel.getName(),
                    "Cannot kick in channel (requires operator)",
                    IRCMessage482::new);
        }

        channel.part(target);
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

    public Set<ServerChannel> getChannels() {
        return Set.copyOf(channels.values());
    }

    public Set<ServerChannel> getChannelsForUser(IRCConnection connection, ServerUser user) {
        // todo respect +s +i
        return user.getChannels();
    }

    public Set<ServerUser> getMembersForChannel(IRCConnection connection, ServerChannel channel) {
        // todo respect +s +i
        return channel.getMembers();
    }

    public void markActivity(IRCConnection connection) {
        ServerUser user = findUser(connection);
        user.setLastActive(System.currentTimeMillis());
    }

    public List<ServerUserWas> getNicknameHistory(String nickname, int limit) {
        return nicknameHistory.stream()
                .filter(u -> u.getNickname().equals(normalizeNickname(nickname)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean isChannelLike(String name) {
        return parameters.getChannelTypes().stream().anyMatch(c -> name.charAt(0) == c);
    }

    public void setChannelInvite(IRCConnection connection, ServerChannel channel, ServerUser user)
            throws StateInvariantException {
        ServerUser me = findUser(connection);
        if (me == null || me.getState() != ServerConnectionState.REGISTERED) {
            throw new StateInvariantException("Not registered", "*", "Not registered", IRCMessage451::new);
        }

        if (!channel.getMembers().contains(me)) {
            throw new StateInvariantException(
                    "Not a member of channel '%s'".formatted(channel.getName()),
                    me.getNickname(),
                    channel.getName(),
                    IRCMessage442::new);
        }

        if (channel.checkFlag(IRCChannelFlag.INVITE_ONLY)
                && !channel.getMembership(me).hasAtLeast(IRCChannelMembershipMode.HALFOP)) {
            throw new StateInvariantException(
                    "User does not have permission to invite members",
                    me.getNickname(),
                    channel.getName(),
                    "Invite-only mode enabled, halfop required to send invites",
                    IRCMessage482::new);
        }

        channel.setInvited(user);
        user.setInvited(channel);
    }

    void clearChannelInvite(ServerChannel channel, ServerUser user) {
        channel.clearInvited(user);
        user.clearInvited(channel);
    }
}
