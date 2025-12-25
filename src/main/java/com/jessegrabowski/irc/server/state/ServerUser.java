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

import com.jessegrabowski.irc.protocol.IRCCapability;
import com.jessegrabowski.irc.protocol.IRCCaseMapping;
import com.jessegrabowski.irc.protocol.IRCUserMode;
import com.jessegrabowski.irc.server.IRCServerParameters;
import com.jessegrabowski.irc.util.Transaction;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;

public final class ServerUser implements MessageSource {
    private final Set<IRCCapability> capabilities = new HashSet<>();
    private final SequencedSet<ServerChannel> channels = new LinkedHashSet<>();
    private final Set<ServerChannel> invitedChannels = new HashSet<>();
    private final Set<IRCUserMode> modes = new HashSet<>();
    private final String hostAddress;
    private final long signOnTime = System.currentTimeMillis();

    private ServerConnectionState state = ServerConnectionState.NEW;
    private long lastPinged = System.currentTimeMillis();
    private long lastPonged = System.currentTimeMillis();
    private long lastActive = System.currentTimeMillis();
    private boolean passwordEntered;
    private boolean negotiatingCapabilities;
    private String nickname;
    private String username;
    private String realName;
    private String awayStatus;
    private String quitMessage;

    ServerUser(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public ServerConnectionState getState() {
        return state;
    }

    void setState(ServerConnectionState state) {
        ServerConnectionState oldState = this.state;
        Transaction.addCompensation(() -> this.state = oldState);
        this.state = state;
    }

    public long getLastPinged() {
        return lastPinged;
    }

    void setLastPinged(long lastPinged) {
        long oldLastPinged = this.lastPinged;
        Transaction.addCompensation(() -> this.lastPinged = oldLastPinged);
        this.lastPinged = lastPinged;
    }

    public long getLastPonged() {
        return lastPonged;
    }

    void setLastPonged(long lastPonged) {
        long oldLastPonged = this.lastPonged;
        Transaction.addCompensation(() -> this.lastPonged = oldLastPonged);
        this.lastPonged = lastPonged;
    }

    public long getSignOnTime() {
        return signOnTime;
    }

    public long getLastActive() {
        return lastActive;
    }

    void setLastActive(long lastActive) {
        long oldLastActive = this.lastActive;
        Transaction.addCompensation(() -> this.lastActive = oldLastActive);
        this.lastActive = lastActive;
    }

    public boolean isPasswordEntered() {
        return passwordEntered;
    }

    void setPasswordEntered(boolean passwordEntered) {
        boolean oldPasswordEntered = this.passwordEntered;
        Transaction.addCompensation(() -> this.passwordEntered = oldPasswordEntered);
        this.passwordEntered = passwordEntered;
    }

    public boolean isNegotiatingCapabilities() {
        return negotiatingCapabilities;
    }

    void setNegotiatingCapabilities(boolean negotiatingCapabilities) {
        boolean oldNegotiatingCapabilities = this.negotiatingCapabilities;
        Transaction.addCompensation(() -> this.negotiatingCapabilities = oldNegotiatingCapabilities);
        this.negotiatingCapabilities = negotiatingCapabilities;
    }

    public String getNickname() {
        return nickname;
    }

    void setNickname(String nickname) {
        String oldNickname = this.nickname;
        Transaction.addCompensation(() -> this.nickname = oldNickname);
        this.nickname = nickname;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String getHostAddress() {
        return hostAddress;
    }

    void setUsername(String username) {
        String oldUsername = this.username;
        Transaction.addCompensation(() -> this.username = oldUsername);
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    void setRealName(String realName) {
        String oldRealName = this.realName;
        Transaction.addCompensation(() -> this.realName = oldRealName);
        this.realName = realName;
    }

    public String getAwayStatus() {
        return awayStatus;
    }

    void setAwayStatus(String awayStatus) {
        String oldAwayStatus = this.awayStatus;
        Transaction.addCompensation(() -> this.awayStatus = oldAwayStatus);
        this.awayStatus = awayStatus;
    }

    void addCapability(IRCCapability capability) {
        Transaction.addTransactionally(capabilities, capability);
    }

    void removeCapability(IRCCapability capability) {
        Transaction.removeTransactionally(capabilities, capability);
    }

    public boolean hasCapability(IRCCapability capability) {
        return capabilities.contains(capability);
    }

    public Set<IRCCapability> getCapabilities() {
        return Collections.unmodifiableSet(capabilities);
    }

    void addChannel(ServerChannel channel) {
        Transaction.addTransactionally(channels, channel);
    }

    void removeChannel(ServerChannel channel) {
        Transaction.removeTransactionally(channels, channel);
    }

    public Set<ServerChannel> getChannels() {
        return Set.copyOf(channels);
    }

    void setQuitMessage(String value) {
        String oldQuitMessage = this.quitMessage;
        Transaction.addCompensation(() -> this.quitMessage = oldQuitMessage);
        this.quitMessage = value;
    }

    public String getQuitMessage() {
        return quitMessage;
    }

    public Set<IRCUserMode> getModes() {
        return Set.copyOf(modes);
    }

    void addMode(IRCUserMode mode) {
        Transaction.addTransactionally(modes, mode);
    }

    void removeMode(IRCUserMode mode) {
        Transaction.removeTransactionally(modes, mode);
    }

    String getNickmask(IRCServerParameters parameters) {
        IRCCaseMapping mapping = parameters.getCaseMapping();
        return mapping.normalizeNickname(nickname)
                + '!'
                + mapping.normalizeNickname(username)
                + '@'
                + mapping.normalizeNickname(hostAddress);
    }

    public Set<ServerChannel> getInvitedChannels() {
        return Set.copyOf(invitedChannels);
    }

    void setInvited(ServerChannel channel) {
        Transaction.addTransactionally(invitedChannels, channel);
    }

    void clearInvited(ServerChannel channel) {
        Transaction.removeTransactionally(invitedChannels, channel);
    }
}
