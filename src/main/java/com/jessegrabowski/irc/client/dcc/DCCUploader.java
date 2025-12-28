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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCCUploader {

    private static final Logger LOG = Logger.getLogger(DCCUploader.class.getName());
    private static final Executor EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Resource file;
    private final String filename;
    private final String host;
    private final int port;
    private final Long length;

    public DCCUploader(Resource file, String filename, String host, int port, Long length) {
        this.file = file;
        this.filename = filename;
        this.host = host;
        this.port = port;
        this.length = length;
    }

    public void pushAsync() {
        EXECUTOR.execute(this::upload);
    }

    private void upload() {
        long totalLength = resolveLength();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 15000);
            socket.setSoTimeout(60000);

            try (InputStream fileIn = new BufferedInputStream(file.getInputStream());
                    OutputStream socketOut = new BufferedOutputStream(socket.getOutputStream());
                    DataInputStream socketIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

                byte[] buf = new byte[64 * 1024];
                long sent = 0L;
                long lastAck = 0L;

                while (true) {
                    int n = fileIn.read(buf);
                    if (n == -1) break;

                    socketOut.write(buf, 0, n);
                    sent += n;

                    if ((sent & ((256L * 1024L) - 1L)) == 0L) {
                        socketOut.flush();
                    }

                    lastAck = drainAvailableAcks(socketIn, lastAck);
                }

                socketOut.flush();

                if (totalLength > 0 && totalLength <= 0xFFFF_FFFFL) {
                    long expected = totalLength;

                    long deadlineMs = System.currentTimeMillis() + 10_000;
                    while (lastAck < expected && System.currentTimeMillis() < deadlineMs) {
                        if (socketIn.available() >= 4) {
                            lastAck = readAck(socketIn);
                        } else {
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }

                    if (lastAck < expected) {
                        LOG.warning("Upload finished but final ACK is short: ack=" + lastAck + " expected=" + expected);
                    }
                }

                LOG.info("Successfully uploaded " + filename + " (" + sent + " bytes) to " + host + ":" + port);
            }
        } catch (SocketTimeoutException e) {
            LOG.log(Level.WARNING, "Upload timed out", e);
        } catch (EOFException e) {
            LOG.log(Level.WARNING, "Connection closed unexpectedly during upload", e);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error uploading file", e);
        }
    }

    private static long drainAvailableAcks(DataInputStream in, long lastAck) throws IOException {
        while (in.available() >= 4) {
            lastAck = readAck(in);
        }
        return lastAck;
    }

    private static long readAck(DataInputStream in) throws IOException {
        int ack32 = in.readInt();
        return ack32 & 0xFFFF_FFFFL;
    }

    private long resolveLength() {
        if (length != null && length > 0) return length;

        try {
            Long s = file.getFileSize();
            if (s != null && s > 0) return s;
        } catch (IOException ignored) {
        }

        return -1L;
    }
}
