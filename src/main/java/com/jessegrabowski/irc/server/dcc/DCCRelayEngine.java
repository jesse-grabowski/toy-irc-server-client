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

import com.jessegrabowski.irc.network.Acceptor;
import com.jessegrabowski.irc.server.IRCServerProperties;
import com.jessegrabowski.irc.util.StateGuard;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCCRelayEngine implements Closeable {

    private static final Logger LOG = Logger.getLogger(DCCRelayEngine.class.getName());

    private final List<DCCEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    private final StateGuard<Map<UUID, DCCPipeHolder>> pipesGuard = new StateGuard<>(new HashMap<>());
    private final ScheduledExecutorService executor;
    private final IRCServerProperties properties;

    public DCCRelayEngine(IRCServerProperties properties) {
        this.properties = properties;

        // using a single-threaded executor for engine tasks greatly simplifies our state management
        // without any real loss of performance (as acceptors spin up extra threads for connection
        // handling, we're just serializing access to the acceptors)
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                1,
                Thread.ofPlatform()
                        .name("DCCRelay-Engine") // name the thread so it shows up nicely in our logs
                        .daemon(false) // we don't want the JVM to terminate until this thread dies
                        .uncaughtExceptionHandler((t, e) -> LOG.log(Level.SEVERE, "Error executing task", e))
                        .factory(),
                // the more important bit is that we're overriding the default exception behavior, the
                // logging just makes it a bit easier for us to spot stray messages during shutdown (which are
                // harmless)
                (task, exec) -> LOG.log(
                        exec.isShutdown() ? Level.FINE : Level.SEVERE,
                        "Task {0} rejected from {1}",
                        new Object[] {task, exec}));
        // bind the state guard to the current (executor) thread
        // any access from other threads will raise an exception
        executor.execute(spy(pipesGuard::bindToCurrentThread));
        this.executor = executor;

        LOG.info("Started DCC Relay Engine");
    }

    public void addListener(DCCEventListener listener) {
        listeners.add(listener);
    }

    private void sendEvent(DCCEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }

    public CompletableFuture<Integer> openForReceiver(UUID token) {
        if (closed.get()) {
            throw new IllegalStateException("DCC Engine is closed");
        }

        return CompletableFuture.supplyAsync(
                () -> openPipe(
                        token,
                        DCCPipeHolder::getReceiverAcceptor,
                        DCCPipeHolder::setReceiverAcceptor,
                        DCCEventReceiverConnected::new,
                        DCCEventReceiverOpened::new),
                executor);
    }

    public CompletableFuture<Integer> openForSender(UUID token) {
        if (closed.get()) {
            throw new IllegalStateException("DCC Engine is closed");
        }

        return CompletableFuture.supplyAsync(
                () -> openPipe(
                        token,
                        DCCPipeHolder::getSenderAcceptor,
                        DCCPipeHolder::setSenderAcceptor,
                        DCCEventSenderConnected::new,
                        DCCEventSenderOpened::new),
                executor);
    }

    private int openPipe(
            UUID token,
            Function<DCCPipeHolder, Acceptor> accepterGetter,
            BiConsumer<DCCPipeHolder, Acceptor> accepterSetter,
            Function<UUID, DCCEvent> connectedEventFactory,
            BiFunction<UUID, Integer, DCCEvent> openedEventFactory) {
        Map<UUID, DCCPipeHolder> pipes = pipesGuard.getState();
        DCCPipeHolder holder = pipes.computeIfAbsent(token, this::createPipeHolder);
        if (accepterGetter.apply(holder) != null) {
            return -1;
        }

        Acceptor acceptor = new Acceptor(
                null,
                properties.getDccPortRange(),
                socket -> doPipe(socket, token, holder, connectedEventFactory),
                Thread::ofVirtual);

        accepterSetter.accept(holder, acceptor);

        int port;
        try {
            port = acceptor.start();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error starting acceptor", e);
            finalizePipe(token);
            return -1;
        }

        sendEvent(openedEventFactory.apply(token, port));
        return port;
    }

    private boolean doPipe(
            Socket socket, UUID token, DCCPipeHolder holder, Function<UUID, DCCEvent> connectedEventFactory) {
        try (socket) {
            holder.getSockets().add(socket);
            DCCRelayPipe pipe = holder.getPipe();
            executor.execute(() -> sendEvent(connectedEventFactory.apply(token)));
            pipe.join(socket, 3, TimeUnit.MINUTES);
            executor.execute(() -> finalizePipe(token));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error relaying DCC data", e);
            executor.execute(() -> finalizePipe(token));
        }
        return false;
    }

    public void cancel(UUID token) {
        executor.execute(() -> finalizePipe(token));
    }

    private void finalizePipe(UUID token) {
        Map<UUID, DCCPipeHolder> pipes = pipesGuard.getState();
        DCCPipeHolder holder = pipes.remove(token);
        if (holder == null) {
            return;
        }
        if (holder.getFinalizerTask() != null) {
            holder.getFinalizerTask().cancel(false);
        }
        for (Socket socket : Set.copyOf(holder.getSockets())) {
            if (!socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException _) {
                    // ignored
                }
            }
        }
        if (holder.senderAcceptor != null) {
            holder.senderAcceptor.close();
        }
        if (holder.receiverAcceptor != null) {
            holder.receiverAcceptor.close();
        }
        sendEvent(new DCCEventTransferClosed(token));
    }

    private DCCPipeHolder createPipeHolder(UUID token) {
        DCCPipeHolder holder = new DCCPipeHolder(new DCCRelayPipe());
        holder.setFinalizerTask(executor.schedule(() -> finalizePipe(token), 10, TimeUnit.MINUTES));
        return holder;
    }

    private Runnable spy(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error executing task", e);
                throw e;
            }
        };
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        try {
            executor.submit(() -> {
                        Map<UUID, DCCPipeHolder> pipes = pipesGuard.getState();
                        for (UUID token : Set.copyOf(pipes.keySet())) {
                            finalizePipe(token);
                        }
                    })
                    .get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Interrupted while waiting for engine shutdown", e);
        } catch (TimeoutException | ExecutionException e) {
            LOG.log(Level.WARNING, "Error waiting for engine shutdown", e);
        }

        executor.shutdownNow();
    }

    private class DCCPipeHolder {

        private final Set<Socket> sockets = ConcurrentHashMap.newKeySet(2);

        private final DCCRelayPipe pipe;

        private Acceptor senderAcceptor;
        private Acceptor receiverAcceptor;
        private ScheduledFuture<?> finalizerTask;

        public DCCPipeHolder(DCCRelayPipe pipe) {
            this.pipe = pipe;
        }

        public Acceptor getSenderAcceptor() {
            return senderAcceptor;
        }

        public void setSenderAcceptor(Acceptor senderAcceptor) {
            this.senderAcceptor = senderAcceptor;
        }

        public Acceptor getReceiverAcceptor() {
            return receiverAcceptor;
        }

        public void setReceiverAcceptor(Acceptor receiverAcceptor) {
            this.receiverAcceptor = receiverAcceptor;
        }

        public ScheduledFuture<?> getFinalizerTask() {
            return finalizerTask;
        }

        public void setFinalizerTask(ScheduledFuture<?> finalizerTask) {
            this.finalizerTask = finalizerTask;
        }

        public DCCRelayPipe getPipe() {
            return pipe;
        }

        public Set<Socket> getSockets() {
            return sockets;
        }
    }
}
