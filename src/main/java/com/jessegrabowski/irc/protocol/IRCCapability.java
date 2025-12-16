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
package com.jessegrabowski.irc.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum IRCCapability {
    AWAY_NOTIFY("away-notify"),
    CAP_NOTIFY("cap-notify"),
    ECHO_MESSAGE("echo-message"),
    MESSAGE_TAGS("message-tags"),
    SERVER_TIME("server-time");

    private static final Map<String, IRCCapability> CAPABILITY_LOOKUP = new HashMap<>();

    static {
        for (IRCCapability capability : IRCCapability.values()) {
            CAPABILITY_LOOKUP.put(capability.getCapabilityName(), capability);
        }
    }

    private final String capabilityName;

    IRCCapability(String capabilityName) {
        this.capabilityName = capabilityName;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public static Optional<IRCCapability> forName(String name) {
        return Optional.ofNullable(CAPABILITY_LOOKUP.get(name));
    }
}
