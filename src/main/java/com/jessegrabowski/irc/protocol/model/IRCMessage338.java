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
package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage338 extends IRCMessage {
    public static final String COMMAND = "338";
    private final String client;
    private final String nick;
    private final String username;
    private final String hostname;
    private final String ip;
    private final String text;

    public IRCMessage338(
            String rawMessage,
            SequencedMap<String, String> tags,
            String prefixName,
            String prefixUser,
            String prefixHost,
            String client,
            String nick,
            String username,
            String hostname,
            String ip,
            String text) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.client = client;
        this.nick = nick;
        this.username = username;
        this.hostname = hostname;
        this.ip = ip;
        this.text = text;
    }

    public static IRCMessage338 forClientNick(
            String rawMessage,
            SequencedMap<String, String> tags,
            String prefixName,
            String prefixUser,
            String prefixHost,
            String client,
            String nick,
            String text) {
        return new IRCMessage338(
                rawMessage, tags, prefixName, prefixUser, prefixHost, client, nick, null, null, null, text);
    }

    public static IRCMessage338 forHost(
            String rawMessage,
            SequencedMap<String, String> tags,
            String prefixName,
            String prefixUser,
            String prefixHost,
            String client,
            String nick,
            String hostname,
            String text) {
        return new IRCMessage338(
                rawMessage, tags, prefixName, prefixUser, prefixHost, client, nick, null, hostname, null, text);
    }

    public static IRCMessage338 forIp(
            String rawMessage,
            SequencedMap<String, String> tags,
            String prefixName,
            String prefixUser,
            String prefixHost,
            String client,
            String nick,
            String ip,
            String text) {
        return new IRCMessage338(
                rawMessage, tags, prefixName, prefixUser, prefixHost, client, nick, null, null, ip, text);
    }

    public String getClient() {
        return client;
    }

    public String getNick() {
        return nick;
    }

    public String getUsername() {
        return username;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public String getText() {
        return text;
    }
}
