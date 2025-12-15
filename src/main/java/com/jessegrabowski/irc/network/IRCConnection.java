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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IRCConnection implements Closeable {

    private static final Logger LOG = Logger.getLogger(IRCConnection.class.getName());

    private static final ThreadFactory INGRESS_THREAD_FACTORY =
            Thread.ofVirtual().name("irc-ingress-", 0).factory();
    private static final ThreadFactory EGRESS_THREAD_FACTORY =
            Thread.ofVirtual().name("irc-egress-", 0).factory();

    // we'll use an arbitrarily high value of 100 here; in practice we really won't have more than ~10
    // messages in this queue at any given time
    private final BlockingQueue<String> egressQueue = new ArrayBlockingQueue<>(100);

    private final List<Consumer<String>> ingressHandlers = new CopyOnWriteArrayList<>();
    private final List<Runnable> shutdownHandlers = new CopyOnWriteArrayList<>();

    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.NEW);

    private final Socket socket;
    private final Charset charset;
    private final Thread ingressThread;
    private final Thread egressThread;

    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    /**
     * Create a new full-duplex IRC connection. Assumes the use of UTF-8 encoding.
     *
     * @param socket a preconfigured socket
     */
    public IRCConnection(Socket socket) {
        this(socket, StandardCharsets.UTF_8);
    }

    /**
     * Create a new full-duplex IRC connection.
     *
     * @param socket a preconfigured socket
     * @param charset the encoding used for communication
     */
    public IRCConnection(Socket socket, Charset charset) {
        this.socket = Objects.requireNonNull(socket);
        this.charset = Objects.requireNonNull(charset);

        ingressThread = INGRESS_THREAD_FACTORY.newThread(this::doIngress);
        egressThread = EGRESS_THREAD_FACTORY.newThread(this::doEgress);
    }

    /**
     * Add a handler to be called each time a new message is received
     *
     * @param handler handler to be called
     */
    public void addIngressHandler(Consumer<String> handler) {
        ingressHandlers.add(Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Add a handler to be called the first time the connection is closed (either explicity, or
     * implicitly by a startup failure)
     *
     * @param handler handler to be called
     */
    public void addShutdownHandler(Runnable handler) {
        shutdownHandlers.add(Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Make a best-effort attempt to enqueue a line for egress. This method does not guarantee that
     * the message will be sent (e.g. if the connection closes while it is enqueued the message will
     * be lost).
     *
     * @param line a well-formed IRC message line
     * @return true if the message was successfully enqueued, false otherwise.
     */
    public boolean offer(String line) {
        if (state.get() != ConnectionState.ACTIVE) {
            return false;
        }
        return egressQueue.offer(Objects.requireNonNull(line));
    }

    public void start() throws IOException {
        if (!state.compareAndSet(ConnectionState.NEW, ConnectionState.INITIALIZING)) {
            throw new IllegalStateException("connection already started or closed");
        }

        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset));

            ingressThread.start();
            egressThread.start();

            if (!state.compareAndSet(ConnectionState.INITIALIZING, ConnectionState.ACTIVE)) {
                throw new IllegalStateException("inconsistent state in initialization");
            }

            LOG.fine("Started IRC connection");
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Failed to start IRC connection", e);
            close();
            throw e;
        }
    }

    private void doIngress() {
        try {
            String line;
            while (state.get() != ConnectionState.CLOSED && (line = bufferedReader.readLine()) != null) {
                LOG.log(Level.FINE, "Received IRC line: {0}", line);
                for (Consumer<String> handler : ingressHandlers) {
                    try {
                        handler.accept(line);
                    } catch (Exception e) {
                        // we control all the handler implementations, and they should be incapable of throwing
                        // under normal operation, so we consider any error here to be fatal and kill the
                        // connection
                        LOG.log(Level.SEVERE, "Error in IRC line consumer", e);
                        return;
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            LOG.log(Level.WARNING, "read timeout", e);
        } catch (IOException e) {
            if (state.get() != ConnectionState.CLOSED) {
                LOG.log(Level.WARNING, "ingress socket exception", e);
            }
        } finally {
            close();
        }
    }

    private void doEgress() {
        try {
            while (state.get() != ConnectionState.CLOSED
                    && !Thread.currentThread().isInterrupted()) {
                String line = egressQueue.take();

                if (state.get() == ConnectionState.CLOSED) {
                    break;
                }

                bufferedWriter.write(line);
                bufferedWriter.write("\r\n");
                bufferedWriter.flush();

                LOG.log(Level.FINE, "Sent IRC line: {0}", line);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (state.get() != ConnectionState.CLOSED) {
                LOG.log(Level.WARNING, "egress socket exception", e);
            }
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        ConnectionState oldState = state.getAndSet(ConnectionState.CLOSED);
        if (oldState == ConnectionState.CLOSED) {
            return;
        }

        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "failed to close socket", e);
        }

        Thread current = Thread.currentThread();
        if (ingressThread != current) {
            ingressThread.interrupt();
        }
        if (egressThread != current) {
            egressThread.interrupt();
        }

        for (Runnable handler : shutdownHandlers) {
            try {
                handler.run();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error in IRC shutdown handler", e);
            }
        }
    }

    private enum ConnectionState {
        NEW,
        INITIALIZING,
        ACTIVE,
        CLOSED
    }
}
