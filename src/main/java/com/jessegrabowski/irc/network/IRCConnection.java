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
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IRCConnection implements Closeable {

    private static final Logger LOG = Logger.getLogger(IRCConnection.class.getName());

    // token to force our egress thread to wake up during shutdown if we call close() without
    // any messages enqueued. Value doesn't really matter as long as it's invalid by IRC.
    private static final String WAKE_UP = "\u0000grababrushandputalittlemakeup\u0000";

    // arbitrary cap on line length; this is a safeguard against malicious clients sending us
    // a single line that's too long to parse.
    private static final int MAX_LINE_LENGTH = 10000;

    private static final ThreadFactory INGRESS_THREAD_FACTORY =
            Thread.ofVirtual().name("IRCConnection-Ingress-", 0).factory();
    private static final ThreadFactory EGRESS_THREAD_FACTORY =
            Thread.ofVirtual().name("IRCConnection-Egress-", 0).factory();
    private static final ThreadFactory FINALIZER_THREAD_FACTORY =
            Thread.ofVirtual().name("IRCConnection-Finalizer-", 0).factory();

    // arbitrary cap; in practice this should almost always be empty unless there's
    // an issue with the connection or we're under very high load
    private final BlockingQueue<String> egressQueue = new ArrayBlockingQueue<>(200);

    private final List<IRCIngressHandler> ingressHandlers = new CopyOnWriteArrayList<>();
    private final List<IRCDisconnectHandler> shutdownHandlers = new CopyOnWriteArrayList<>();

    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.NEW);
    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

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
    public void addIngressHandler(IRCIngressHandler handler) {
        ingressHandlers.add(Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Add a handler to be called the first time the connection is closed (either explicity, or
     * implicitly by a startup failure)
     *
     * @param handler handler to be called
     */
    public void addShutdownHandler(IRCDisconnectHandler handler) {
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
            StringBuilder lineBuilder = new StringBuilder(MAX_LINE_LENGTH);
            while (state.get() != ConnectionState.CLOSED && readLine(bufferedReader, lineBuilder)) {
                line = lineBuilder.toString();
                lineBuilder.setLength(0);
                if (state.get() == ConnectionState.CLOSING) {
                    LOG.log(Level.FINE, "Received IRC line in closing: {0}", line);
                    continue;
                } else {
                    LOG.log(Level.FINE, "Received IRC line: {0}", line);
                }

                for (IRCIngressHandler handler : ingressHandlers) {
                    try {
                        handler.receive(this, line);
                    } catch (Exception e) {
                        // We don't have a meaningful way to deal with handler failures safely here
                        // so we kill the connection and let the higher-level application decide what
                        // to do (i.e., client prompts for reconnection, server just drops the client)
                        // via a registered shutdownHandler
                        LOG.log(Level.SEVERE, "Error in IRC line consumer", e);
                        return;
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            LOG.log(Level.WARNING, "read timeout", e);
        } catch (IOException e) {
            if (state.get() != ConnectionState.CLOSED && state.get() != ConnectionState.CLOSING) {
                LOG.log(Level.WARNING, "ingress socket exception", e);
            }
        } finally {
            closeDeferred();
        }
    }

    // We can't use reader.readLine() or we'll risk a malicious infinite length line running us out
    // of memory. Instead we read character by character, considering \r\n as the only line delimiter
    // and retaining solo \r and \n characters. If the line length limit is exceeded, we silently
    // truncate the message and let the unmarshaller flag the error.
    private boolean readLine(BufferedReader reader, StringBuilder lineBuilder) throws IOException {
        int next;
        boolean cr = false;
        while (state.get() != ConnectionState.CLOSED && (next = reader.read()) != -1) {
            char nextChar = (char) next;
            if (cr && nextChar == '\r') {
                lineBuilder.append('\r');
                continue;
            } else if (nextChar == '\r') {
                cr = true;
                continue;
            } else if (nextChar == '\n' && cr) {
                return true;
            }
            if (cr && lineBuilder.length() < MAX_LINE_LENGTH) {
                lineBuilder.append('\r');
            }
            cr = false;
            if (lineBuilder.length() < MAX_LINE_LENGTH) {
                lineBuilder.append(nextChar);
            }
        }
        if (cr) {
            lineBuilder.append('\r');
        }
        if (!lineBuilder.isEmpty()) {
            LOG.log(Level.FINE, "IRCConnection ingress closed with partial line: {0}", lineBuilder);
        }
        return false;
    }

    private void doEgress() {
        try {
            while (state.get() != ConnectionState.CLOSED
                    && !Thread.currentThread().isInterrupted()) {
                if (state.get() == ConnectionState.CLOSING && egressQueue.isEmpty()) {
                    break;
                }
                String line = egressQueue.poll(250, TimeUnit.MILLISECONDS);
                if (line == null || WAKE_UP.equals(line)) {
                    continue;
                }

                bufferedWriter.write(line);
                bufferedWriter.write("\r\n");
                bufferedWriter.flush();

                LOG.log(Level.FINE, "Sent IRC line: {0}", line);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (state.get() != ConnectionState.CLOSED && state.get() != ConnectionState.CLOSING) {
                LOG.log(Level.WARNING, "egress socket exception", e);
            }
        } finally {
            closeDeferred();
        }
    }

    /**
     * Close the connection. The connection makes a best-effort attempt to drain the egress queue
     * before closing - this method will wait until that completes. See {@link #closeDeferred()} for
     * a non-blocking alternative.
     */
    @Override
    public void close() {
        try {
            closeDeferred().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error closing IRC connection", e);
        }
    }

    /**
     * Close the connection asynchronously.
     *
     * @return a {@code java.util.concurrent.CompletableFuture} that will complete when the connection is fully closed.
     */
    public CompletableFuture<Void> closeDeferred() {
        ConnectionState oldState = state.getAndUpdate(s -> switch (s) {
            case CLOSED, CLOSING -> s;
            default -> ConnectionState.CLOSING;
        });
        if (oldState == ConnectionState.CLOSED || oldState == ConnectionState.CLOSING) {
            return closeFuture;
        }

        // force egress to wake up if it's stuck polling
        egressQueue.offer(WAKE_UP);

        FINALIZER_THREAD_FACTORY
                .newThread(() -> {
                    try {
                        finalizeClose();
                        closeFuture.complete(null);
                    } catch (Throwable t) {
                        closeFuture.completeExceptionally(t);
                    }
                })
                .start();
        return closeFuture;
    }

    // wait up to 5 seconds to give the egress queue time to
    // drain, then interrupt the thread and shut down
    private void finalizeClose() {
        try {
            egressThread.join(Duration.ofSeconds(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error closing socket", e);
        }

        Thread current = Thread.currentThread();
        if (ingressThread != current && ingressThread.isAlive()) {
            ingressThread.interrupt();
        }
        if (egressThread != current && egressThread.isAlive()) {
            egressThread.interrupt();
        }

        state.set(ConnectionState.CLOSED);

        for (IRCDisconnectHandler handler : shutdownHandlers) {
            try {
                handler.onDisconnect(this);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error in IRC shutdown handler", e);
            }
        }
    }

    public String getHostAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    public boolean isClosed() {
        ConnectionState currentState = state.get();
        return currentState == ConnectionState.CLOSED || currentState == ConnectionState.CLOSING;
    }

    private enum ConnectionState {
        NEW,
        INITIALIZING,
        ACTIVE,
        CLOSING,
        CLOSED
    }
}
