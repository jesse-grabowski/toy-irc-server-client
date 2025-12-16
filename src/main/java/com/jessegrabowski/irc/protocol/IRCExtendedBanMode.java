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

public enum IRCExtendedBanMode {
    ALLOW_INVITE('A'),
    BLOCK_CAPS('B'),
    CTCP('C'),
    CHANNEL('j'),
    NICKS('N'),
    OPERS('o'),
    OPER_TYPES('O'),
    PART('p'),
    QUIET('q'),
    BLOCK_KICKS('Q'),
    REAL_NAME('r'),
    SERVER('s'),
    BLOCK_NOTICE('T'),
    UNREGISTERED('U'),
    TLS_CERT('z'),
    AND('&'),
    OR('|');

    private static final Map<Character, IRCExtendedBanMode> EXTENDED_BAN_MODES = new HashMap<>();

    static {
        for (IRCExtendedBanMode extendedBanModes : IRCExtendedBanMode.values()) {
            EXTENDED_BAN_MODES.put(extendedBanModes.character, extendedBanModes);
        }
    }

    private final char character;

    IRCExtendedBanMode(char character) {
        this.character = character;
    }

    public static Optional<IRCExtendedBanMode> forName(char name) {
        return Optional.ofNullable(EXTENDED_BAN_MODES.get(name));
    }
}
