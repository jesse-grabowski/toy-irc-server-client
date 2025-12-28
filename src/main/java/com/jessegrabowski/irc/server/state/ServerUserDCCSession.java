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

import com.jessegrabowski.irc.protocol.model.IRCMessage;
import java.util.UUID;

public class ServerUserDCCSession {
    private final UUID token;
    private final String filename;
    private final ServerUser target;
    private final Long fileSize;
    private final IRCMessage initiator;

    public ServerUserDCCSession(UUID token, String filename, ServerUser target, Long fileSize, IRCMessage initiator) {
        this.token = token;
        this.filename = filename;
        this.target = target;
        this.fileSize = fileSize;
        this.initiator = initiator;
    }

    public UUID getToken() {
        return token;
    }

    public String getFilename() {
        return filename;
    }

    public ServerUser getTarget() {
        return target;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public IRCMessage getInitiator() {
        return initiator;
    }
}
