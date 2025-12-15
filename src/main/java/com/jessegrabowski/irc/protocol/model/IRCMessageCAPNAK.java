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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageCAPNAK extends IRCMessage {

    public static final String COMMAND = "CAP";

    private final String nick;
    private final List<String> enableCapabilities;
    private final List<String> disableCapabilities;

    public IRCMessageCAPNAK(
            String rawMessage,
            SequencedMap<String, String> tags,
            String prefixName,
            String prefixUser,
            String prefixHost,
            String nick,
            List<String> enableCapabilities,
            List<String> disableCapabilities) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.nick = nick;
        this.enableCapabilities = enableCapabilities;
        this.disableCapabilities = disableCapabilities;
    }

    public IRCMessageCAPNAK(String nick, List<String> enableCapabilities, List<String> disableCapabilities) {
        this(null, new LinkedHashMap<>(), null, null, null, nick, enableCapabilities, disableCapabilities);
    }

    public String getNick() {
        return nick;
    }

    public List<String> getEnableCapabilities() {
        return enableCapabilities;
    }

    public List<String> getDisableCapabilities() {
        return disableCapabilities;
    }
}
