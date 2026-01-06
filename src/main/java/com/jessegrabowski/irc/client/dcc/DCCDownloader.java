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

import com.jessegrabowski.irc.util.Directory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCCDownloader {

    private static final Logger LOG = Logger.getLogger(DCCDownloader.class.getName());
    private static final ThreadFactory THREAD_FACTORY =
            Thread.ofVirtual().name("IRCDCC-Ingress-", 0).factory();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicReference<Socket> socketHolder = new AtomicReference<>();

    private final String token;
    private final DCCClientEventListener listener;

    private final Directory downloadDirectory;
    private final String filename;
    private final String host;
    private final int port;
    private final Long length;

    public DCCDownloader(
            String token,
            DCCClientEventListener listener,
            Directory downloadDirectory,
            String filename,
            String host,
            int port,
            Long length) {
        this.token = token;
        this.listener = listener;
        this.downloadDirectory = downloadDirectory;
        this.filename = filename;
        this.host = host;
        this.port = port;
        this.length = length;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Download already in progress");
        }
        if (cancelled.get()) {
            return;
        }

        THREAD_FACTORY.newThread(this::download).start();
    }

    public void cancel() {
        cancelled.set(true);
        Socket socket = socketHolder.get();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error closing socket on download cancellation", e);
            }
        }
    }

    private void download() {
        long received = 0;
        Path outPath = null;

        try (Socket socket = new Socket()) {
            socketHolder.set(socket);
            socket.setSoTimeout(60000);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(host, port), 15000);

            outPath = downloadDirectory.createFile(filename);

            try (OutputStream fileOut =
                            new BufferedOutputStream(Files.newOutputStream(outPath, StandardOpenOption.WRITE));
                    InputStream socketIn = new BufferedInputStream(socket.getInputStream());
                    DataOutputStream socketOut =
                            new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

                byte[] buffer = new byte[16 * 1024];

                long lastProgressAlert = 0;
                while (length == null || received < length) {
                    if (cancelled.get()) {
                        throw new IOException("Download cancelled");
                    }

                    int n = socketIn.read(buffer);
                    if (n == -1) {
                        break;
                    }
                    if (length != null && received + n > length) {
                        n = (int) (length - received);
                    }

                    fileOut.write(buffer, 0, n);
                    fileOut.flush();
                    received += n;

                    // intentionally truncate to lower half
                    socketOut.writeInt((int) received);
                    socketOut.flush();

                    long now = System.currentTimeMillis();
                    if (lastProgressAlert + 500 < now) {
                        listener.onEvent(new DCCClientDownloadProgressEvent(token, received, length));
                        lastProgressAlert = now;
                    }
                }

                if (length != null && length >= 0 && received < length) {
                    throw new EOFException("Connection closed unexpectedly");
                }

                listener.onEvent(new DCCClientDownloadCompletedEvent(token, outPath));
            }
        } catch (SocketTimeoutException e) {
            LOG.log(Level.WARNING, "Download timed out", e);
            cleanupPartial(outPath);
            listener.onEvent(new DCCClientDownloadFailedEvent(token, "Download timed out"));
        } catch (EOFException e) {
            LOG.log(Level.WARNING, "Connection closed unexpectedly during download", e);
            cleanupPartial(outPath);
            listener.onEvent(new DCCClientDownloadFailedEvent(token, "Connection closed unexpectedly"));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error downloading file", e);
            cleanupPartial(outPath);
            listener.onEvent(new DCCClientDownloadFailedEvent(token, e.getMessage()));
        }
    }

    private static void cleanupPartial(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.log(Level.FINE, "Failed to delete partially downloaded file", e);
        }
    }
}
