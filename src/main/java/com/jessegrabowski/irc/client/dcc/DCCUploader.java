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
package com.jessegrabowski.irc.client.dcc;

import com.jessegrabowski.irc.util.Resource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCCUploader {

    private static final Logger LOG = Logger.getLogger(DCCUploader.class.getName());
    private static final ThreadFactory THREAD_FACTORY =
            Thread.ofVirtual().name("IRCClient-DCCUploader-", 0).factory();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicReference<Socket> socketHolder = new AtomicReference<>();

    private final String token;
    private final DCCClientEventListener listener;

    private final Resource file;
    private final String host;
    private final int port;
    private final Long length;

    public DCCUploader(
            String token, DCCClientEventListener listener, Resource file, String host, int port, Long length) {
        this.token = token;
        this.listener = listener;
        this.file = file;
        this.host = host;
        this.port = port;
        this.length = length;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Upload already in progress");
        }
        if (cancelled.get()) {
            return;
        }

        THREAD_FACTORY.newThread(this::upload).start();
    }

    public void cancel() {
        cancelled.set(true);
        Socket socket = socketHolder.get();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error closing socket on upload cancellation", e);
            }
        }
    }

    private void upload() {
        Long resolvedLength = resolveLength();
        long sent = 0;
        AckState ack = new AckState();

        try (Socket socket = new Socket()) {
            socketHolder.set(socket);
            socket.setSoTimeout(60000);
            socket.setKeepAlive(true);
            socket.connect(new InetSocketAddress(host, port), 15000);

            try (InputStream fileIn = new BufferedInputStream(file.getInputStream());
                    OutputStream socketOut = new BufferedOutputStream(socket.getOutputStream());
                    DataInputStream socketIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

                byte[] buffer = new byte[16 * 1024];

                long lastProgressAlert = 0;
                while (true) {
                    if (cancelled.get()) {
                        throw new IOException("Upload cancelled");
                    }
                    int n = fileIn.read(buffer);
                    if (n == -1) {
                        break;
                    }

                    socketOut.write(buffer, 0, n);
                    socketOut.flush();
                    sent += n;

                    drainAvailableAcks(socketIn, ack);

                    long now = System.currentTimeMillis();
                    if (lastProgressAlert + 500 < now) {
                        listener.onEvent(new DCCClientUploadProgressEvent(token, sent, ack.total, resolvedLength));
                        lastProgressAlert = now;
                    }
                }

                socket.shutdownOutput();

                while (ack.total < sent) {
                    if (cancelled.get()) {
                        throw new IOException("Upload cancelled");
                    }
                    consumeAck(socketIn, ack);
                }

                listener.onEvent(new DCCClientUploadCompletedEvent(token));
            }
        } catch (SocketTimeoutException e) {
            LOG.log(Level.WARNING, "Upload timed out", e);
            listener.onEvent(new DCCClientUploadFailedEvent(token, "Upload timed out"));
        } catch (EOFException e) {
            LOG.log(Level.WARNING, "Connection closed unexpectedly during upload", e);
            listener.onEvent(new DCCClientUploadFailedEvent(token, "Connection closed unexpectedly"));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error uploading file", e);
            listener.onEvent(new DCCClientUploadFailedEvent(token, e.getMessage()));
        }
    }

    private static void drainAvailableAcks(DataInputStream in, AckState ack) throws IOException {
        while (in.available() >= 4) {
            consumeAck(in, ack);
        }
    }

    // this is a bit convoluted but DCC acks are 32 bit and we need to handle wraparound
    // on the off chance someone tries to send a larger file
    private static void consumeAck(DataInputStream in, AckState s) throws IOException {
        long ack32 = Integer.toUnsignedLong(in.readInt());
        if (ack32 < s.last32) {
            s.base += (1L << Integer.SIZE);
        }
        s.last32 = ack32;
        s.total = s.base + ack32;
    }

    private Long resolveLength() {
        if (length != null && length >= 0) {
            return length;
        }

        try {
            return file.getFileSize();
        } catch (IOException _) {
            return null;
        }
    }

    private static final class AckState {
        long base;
        long last32;
        long total;
    }
}
