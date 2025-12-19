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

import com.jessegrabowski.irc.protocol.IRCMessageFactory0;
import com.jessegrabowski.irc.protocol.IRCMessageFactory1;
import com.jessegrabowski.irc.protocol.IRCMessageFactory2;
import com.jessegrabowski.irc.protocol.model.IRCMessage;

public class StateInvariantException extends Exception {

    private final IRCMessageFactory0<?> factory;

    public StateInvariantException(String message, IRCMessageFactory0<?> factory0) {
        super(message);
        this.factory = factory0;
    }

    public <A> StateInvariantException(String message, A arg0, IRCMessageFactory1<IRCMessage, A> factory1) {
        super(message);
        this.factory = (raw, tags, name, user, host) -> factory1.create(raw, tags, name, user, host, arg0);
    }

    public <A, B> StateInvariantException(
            String message, A arg0, B arg1, IRCMessageFactory2<IRCMessage, A, B> factory2) {
        super(message);
        this.factory = (raw, tags, name, user, host) -> factory2.create(raw, tags, name, user, host, arg0, arg1);
    }

    public IRCMessageFactory0<?> getFactory() {
        return factory;
    }
}
