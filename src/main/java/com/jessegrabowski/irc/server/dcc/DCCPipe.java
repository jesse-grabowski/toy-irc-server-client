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
package com.jessegrabowski.irc.server.dcc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class DCCPipe implements Closeable {
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile Socket sender;
    private volatile Socket receiver;

    DCCPipe() {}

    void bindSource(Socket socket) {
        this.sender = socket;
    }

    void bindDestination(Socket socket) {
        this.receiver = socket;
    }

    boolean isBound() {
        return sender != null && receiver != null;
    }

    void pipeSenderToReceiver() throws IOException {
        // large buffer since we're sending file data
        transmit(sender, receiver, 32 * 1024);
    }

    void pipeReceiverToSender() throws IOException {
        // tiny buffer since acks are small
        transmit(receiver, sender, 1024);
    }

    private void transmit(Socket src, Socket dest, int bufferLength) throws IOException {
        byte[] buffer = new byte[bufferLength];
        try {
            InputStream is = src.getInputStream();
            OutputStream os = dest.getOutputStream();

            while (!closed.get()) {
                int n = is.read(buffer);
                if (n > 0) {
                    os.write(buffer, 0, n);
                } else if (n == -1) {
                    try {
                        os.flush();
                    } catch (IOException _) {
                        // ignore
                    }
                    try {
                        src.shutdownInput();
                    } catch (IOException _) {
                        // ignore
                    }
                    try {
                        dest.shutdownOutput();
                    } catch (IOException _) {
                        // ignore
                    }
                    return;
                }
            }
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (sender != null) {
            try {
                sender.close();
            } catch (IOException _) {
                // ignore
            }
        }
        if (receiver != null) {
            try {
                receiver.close();
            } catch (IOException _) {
                // ignore
            }
        }
    }
}
