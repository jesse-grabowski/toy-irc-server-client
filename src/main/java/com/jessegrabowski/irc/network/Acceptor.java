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
package com.jessegrabowski.irc.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// somewhat based on Tomcat's acceptor, but simpler
public class Acceptor implements Closeable {

    private final Logger LOG = Logger.getLogger(Acceptor.class.getName());

    private final InetSocketAddress address;
    private final Consumer<Socket> dispatcher;

    private volatile State state = State.NEW;
    private volatile ServerSocket serverSocket;
    private volatile Thread thread;

    /**
     * Initializes our acceptor loop. After this, call {@code #start()}
     *
     * @param host the {@link InetAddress} representing the host on which the server
     *             will listen for connections.
     * @param port the port number on which the server will accept client connections.
     *             Must be between 0 and 65535.
     * @param dispatcher handles incoming {@link Socket} connections. This MUST be
     *                   non-blocking and should simply set appropriate socket options
     *                   based on the server configuration and then dispatch it to a
     *                   separate thread for processing.
     */
    public Acceptor(InetAddress host, int port, Consumer<Socket> dispatcher) {
        this.address = new InetSocketAddress(host, port);
        this.dispatcher = dispatcher;
    }

    public synchronized int start() throws IOException {
        if (state != State.NEW) {
            throw new IllegalStateException("server already started or closed");
        }

        try {
            serverSocket = new ServerSocket();
            // allow for quick restart
            serverSocket.setReuseAddress(true);
            // relatively low backlog since we expect few connections
            serverSocket.bind(address, 50);
        } catch (IOException e) {
            close();
            throw e;
        }

        state = State.RUNNING;

        thread = new Thread(this::run, "Acceptor-%s:%d".formatted(address.getHostString(), address.getPort()));
        thread.setDaemon(false); // block shutdown until fully closed
        thread.start();

        return serverSocket.getLocalPort();
    }

    private void run() {
        try {
            int failures = 0;
            while (state == State.RUNNING) {
                try {
                    Socket socket = serverSocket.accept();
                    try {
                        dispatcher.accept(socket);
                    } catch (Exception e) {
                        LogRecord record =
                                new LogRecord(Level.WARNING, "Error in socket dispatch with remote address {0}");
                        record.setParameters(new Object[] {socket.getRemoteSocketAddress()});
                        record.setThrown(e);
                        LOG.log(record);
                        try {
                            socket.close();
                        } catch (IOException ex) {
                            LOG.log(Level.FINE, "Error closing client socket", ex);
                        }
                    }
                    failures = 0;
                } catch (IOException e) {
                    if (state != State.RUNNING || serverSocket.isClosed()) {
                        break;
                    }
                    failures++;
                    if (failures == 1) {
                        LOG.log(Level.WARNING, "Failed to accept() on server socket", e);
                    } else if (failures % 10 == 0) {
                        LOG.log(Level.WARNING, "Repeated failure to accept() on server socket", e);
                    } else {
                        LOG.log(Level.FINE, "Failed to accept() on server socket", e);
                    }
                }
                // don't sleep for single failure, probably intermittent
                if (failures > 1) {
                    try {
                        Thread.sleep(calculateBackoff(failures));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            close();
        }
    }

    // exponential backoff, up to 1.6 seconds on repeated errors
    private long calculateBackoff(int failures) {
        return Math.min(1600, 50L * (1L << Math.min(failures - 2, 5)));
    }

    @Override
    public synchronized void close() {
        if (state == State.CLOSED) {
            return;
        }

        state = State.CLOSED;

        // interrupt serverSocket.accept()
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error closing server socket", e);
        }

        // break out of failure retry backoff if necessary
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }

    private enum State {
        NEW,
        RUNNING,
        CLOSED
    }
}
