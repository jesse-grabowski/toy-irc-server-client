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
package com.jessegrabowski.irc.server;

import com.jessegrabowski.irc.network.IRCConnection;
import java.util.HashMap;
import java.util.Map;

public class IRCServerState {

    private final Map<IRCConnection, Connection> connections = new HashMap<>();
    private IRCServerParameters parameters;

    public void addConnection(IRCConnection connection) {
        connections.put(connection, new Connection(connection));
    }

    public void removeConnection(IRCConnection connection) {
        connections.remove(connection);
    }

    public boolean isNicknameAvailable(String nickname) {
        return connections.values().stream().noneMatch(c -> nickname.equals(c.getNickname()));
    }

    public void setConnectionNickname(IRCConnection connection, String nickname) {
        connections.get(connection).setNickname(nickname);
    }

    public void setConnectionUser(IRCConnection connection, String user) {
        connections.get(connection).setUser(user);
    }

    public void setConnectionRealName(IRCConnection connection, String realName) {
        connections.get(connection).setRealName(realName);
    }

    public String getConnectionNickname(IRCConnection connection) {
        return connections.get(connection).getNickname();
    }

    public void setConnectionPassword(IRCConnection connection, boolean password) {
        connections.get(connection).setPassword(password);
    }

    public boolean isConnectionRegistered(IRCConnection connection, boolean expectsPassword) {
        Connection c = connections.get(connection);
        return c.getNickname() != null
                && c.getUser() != null
                && c.getRealName() != null
                && (!expectsPassword || c.isPassword());
    }

    public IRCServerParameters getParameters() {
        return parameters;
    }

    public void setParameters(IRCServerParameters parameters) {
        this.parameters = parameters;
    }

    private static final class Connection {
        private final IRCConnection connection;
        private String nickname;
        private boolean password;
        private String user;
        private String realName;

        private Connection(IRCConnection connection) {
            this.connection = connection;
        }

        private String getNickname() {
            return nickname;
        }

        private void setNickname(String nickname) {
            this.nickname = nickname;
        }

        private boolean isPassword() {
            return password;
        }

        private void setPassword(boolean password) {
            this.password = password;
        }

        private String getUser() {
            return user;
        }

        private void setUser(String user) {
            this.user = user;
        }

        private String getRealName() {
            return realName;
        }

        private void setRealName(String realName) {
            this.realName = realName;
        }
    }
}
