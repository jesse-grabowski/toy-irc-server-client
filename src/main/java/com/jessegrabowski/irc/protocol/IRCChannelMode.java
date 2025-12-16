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

import com.jessegrabowski.irc.server.IRCServerParameters;
import java.util.Optional;

public enum IRCChannelMode {
    BAN,
    EXCEPTION,
    CLIENT_LIMIT,
    INVITE_ONLY,
    INVITE_EXCEPTION,
    KEY,
    MODERATED,
    SECRET,
    PROTECTED,
    NO_EXTERNAL_MESSAGES;

    public static Optional<IRCChannelMode> forName(IRCServerParameters parameters, char name) {
        return switch (name) {
            case 'b' -> Optional.of(BAN);
            case 'l' -> Optional.of(CLIENT_LIMIT);
            case 'i' -> Optional.of(INVITE_ONLY);
            case 'k' -> Optional.of(KEY);
            case 'm' -> Optional.of(MODERATED);
            case 's' -> Optional.of(SECRET);
            case 't' -> Optional.of(PROTECTED);
            case 'n' -> Optional.of(NO_EXTERNAL_MESSAGES);
            default -> {
                if (parameters.getExcepts() == name) {
                    yield Optional.of(EXCEPTION);
                } else if (parameters.getInviteExceptions() == name) {
                    yield Optional.of(INVITE_EXCEPTION);
                } else {
                    yield Optional.empty();
                }
            }
        };
    }
}
