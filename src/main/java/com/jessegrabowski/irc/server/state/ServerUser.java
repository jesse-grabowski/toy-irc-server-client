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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;

public final class ServerUser {
    private final Set<IRCCapability> capabilities = new HashSet<>();
    private final SequencedSet<ServerChannel> channels = new LinkedHashSet<>();
    private final Set<Character> flags = new HashSet<>();

    private ServerConnectionState state = ServerConnectionState.NEW;
    private long lastPinged = System.currentTimeMillis();
    private long lastPonged = System.currentTimeMillis();
    private boolean passwordEntered;
    private boolean negotiatingCapabilities;
    private String nickname;
    private String username;
    private String realName;
    private String awayStatus;
    private boolean operator;

    public ServerConnectionState getState() {
        return state;
    }

    public void setState(ServerConnectionState state) {
        this.state = state;
    }

    public long getLastPinged() {
        return lastPinged;
    }

    public void setLastPinged(long lastPinged) {
        this.lastPinged = lastPinged;
    }

    public long getLastPonged() {
        return lastPonged;
    }

    public void setLastPonged(long lastPonged) {
        this.lastPonged = lastPonged;
    }

    public boolean isPasswordEntered() {
        return passwordEntered;
    }

    public void setPasswordEntered(boolean passwordEntered) {
        this.passwordEntered = passwordEntered;
    }

    public boolean isNegotiatingCapabilities() {
        return negotiatingCapabilities;
    }

    public void setNegotiatingCapabilities(boolean negotiatingCapabilities) {
        this.negotiatingCapabilities = negotiatingCapabilities;
    }

    public String getNickname() {
        return nickname;
    }

    void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getAwayStatus() {
        return awayStatus;
    }

    public void setAwayStatus(String awayStatus) {
        this.awayStatus = awayStatus;
    }

    public boolean isOperator() {
        return operator;
    }

    public void setOperator(boolean operator) {
        this.operator = operator;
    }

    public void addCapability(IRCCapability capability) {
        capabilities.add(capability);
    }

    public void removeCapability(IRCCapability capability) {
        capabilities.remove(capability);
    }

    public boolean hasCapability(IRCCapability capability) {
        return capabilities.contains(capability);
    }

    public Set<IRCCapability> getCapabilities() {
        return Collections.unmodifiableSet(capabilities);
    }
}
