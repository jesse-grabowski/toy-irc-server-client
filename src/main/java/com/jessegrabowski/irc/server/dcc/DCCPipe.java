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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DCCPipe implements Closeable {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<Socket> sender = new AtomicReference<>();
    private final AtomicReference<Socket> receiver = new AtomicReference<>();

    private final CyclicBarrier barrier = new CyclicBarrier(2);
    private final AtomicInteger shutdownCounter = new AtomicInteger(2);

    DCCPipe() {}

    boolean bindSender(Socket socket) {
        return sender.compareAndSet(null, socket);
    }

    boolean bindReceiver(Socket socket) {
        return receiver.compareAndSet(null, socket);
    }

    boolean waitForPartner(long timeout, TimeUnit unit) {
        if (closed.get()) {
            return false;
        }
        try {
            barrier.await(timeout, unit);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (BrokenBarrierException | TimeoutException e) {
            return false;
        }
    }

    boolean pipeSenderToReceiver() throws IOException {
        Socket source = sender.get();
        Socket dest = receiver.get();
        if (source == null || dest == null) {
            close();
            throw new IllegalStateException("DCCPipe not fully bound");
        }
        // large buffer since we're sending file data
        try {
            transmit(source, dest, 32 * 1024);
            return shutdownCounter.decrementAndGet() == 0;
        } catch (IOException | RuntimeException e) {
            shutdownCounter.decrementAndGet();
            throw e;
        }
    }

    boolean pipeReceiverToSender() throws IOException {
        Socket source = receiver.get();
        Socket dest = sender.get();
        if (source == null || dest == null) {
            close();
            throw new IllegalStateException("DCCPipe not fully bound");
        }
        // tiny buffer since acks are small
        try {
            transmit(source, dest, 1024);
            return shutdownCounter.decrementAndGet() == 0;
        } catch (IOException | RuntimeException e) {
            shutdownCounter.decrementAndGet();
            throw e;
        }
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
        barrier.reset();
        Socket senderSocket = sender.getAndSet(null);
        if (senderSocket != null) {
            try {
                senderSocket.close();
            } catch (IOException _) {
                // ignore
            }
        }
        Socket receiverSocket = receiver.getAndSet(null);
        if (receiverSocket != null) {
            try {
                receiverSocket.close();
            } catch (IOException _) {
                // ignore
            }
        }
    }
}
