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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// somewhat based on Tomcat's acceptor, but simpler
public class Acceptor implements Closeable {

    private static final int SOCKET_BACKLOG = 50;
    private static final Logger LOG = Logger.getLogger(Acceptor.class.getName());

    private final InetAddress host;
    private final Port port;
    private final Dispatcher dispatcher;
    private final Supplier<Thread.Builder> threadBuilder;

    private volatile State state = State.NEW;
    private volatile ServerSocket serverSocket;
    private volatile Thread thread;

    /**
     * Initializes our acceptor loop. After this, call {@code #start()}. By default, this
     * uses a non-daemon thread (blocking shutdown).
     *
     * @param host the {@link InetAddress} representing the host on which the server
     *             will listen for connections.
     * @param port the port on which the socket will listen, or a port range. See
     *             {@link Port} for more details.
     * @param dispatcher handles incoming {@link Socket} connections. This MUST be
     *                   non-blocking and should simply set appropriate socket options
     *                   based on the server configuration and then dispatch it to a
     *                   separate thread for processing.
     */
    public Acceptor(InetAddress host, Port port, Dispatcher dispatcher) {
        this(host, port, dispatcher, () -> Thread.ofPlatform().daemon(false));
    }

    /**
     * Initializes our acceptor loop. After this, call {@code #start()}
     *
     * @param host the {@link InetAddress} representing the host on which the server
     *             will listen for connections.
     * @param port the port on which the socket will listen, or a port range. See
     *             {@link Port} for more details.
     * @param dispatcher handles incoming {@link Socket} connections. This blocks
     *                   the thread created by {@code threadBuilder}.
     * @param threadBuilder builds the thread used to accept incoming connections.
     */
    public Acceptor(InetAddress host, Port port, Dispatcher dispatcher, Supplier<Thread.Builder> threadBuilder) {
        this.host = host;
        this.port = port;
        this.dispatcher = dispatcher;
        this.threadBuilder = threadBuilder;
    }

    public synchronized int start() throws IOException {
        if (state != State.NEW) {
            throw new IllegalStateException("server already started or closed");
        }

        try {
            serverSocket = new ServerSocket();
            // allow for quick restart
            serverSocket.setReuseAddress(true);
            switch (port) {
                case Port.FixedPort p -> serverSocket.bind(new InetSocketAddress(host, p.port()), SOCKET_BACKLOG);
                case Port.PortRange p -> bindInRange(serverSocket, p.start(), p.end());
            }
        } catch (IOException e) {
            close();
            throw e;
        }

        state = State.RUNNING;

        InetSocketAddress address = (InetSocketAddress) serverSocket.getLocalSocketAddress();
        LOG.info("Listening on " + address);
        thread = threadBuilder
                .get()
                .name("Acceptor-%s:%d".formatted(address.getHostString(), address.getPort()))
                .start(this::run);

        return serverSocket.getLocalPort();
    }

    // try random ports until we find one that is available
    private void bindInRange(ServerSocket socket, int startPort, int endPort) throws IOException {
        int range = endPort - startPort + 1;

        for (int i = 0; i < range; i++) {
            int port = startPort + ThreadLocalRandom.current().nextInt(range);
            try {
                socket.bind(new InetSocketAddress(host, port), SOCKET_BACKLOG);
                return;
            } catch (IOException e) {
                LOG.log(Level.FINEST, "Failed to bind to port " + port, e);
            }
        }

        throw new IOException("Failed to bind port in range " + startPort + "-" + endPort);
    }

    private void run() {
        try {
            int failures = 0;
            while (state == State.RUNNING) {
                try {
                    Socket socket = serverSocket.accept();
                    try {
                        if (!dispatcher.dispatch(socket)) {
                            LOG.info("Dispatcher returned false, closing server socket");
                            return;
                        }
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

        InetSocketAddress address = (InetSocketAddress) serverSocket.getLocalSocketAddress();
        LOG.info("No longer listening on " + address);

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
