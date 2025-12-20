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
package com.jessegrabowski.irc.server;

import com.jessegrabowski.irc.network.IRCConnection;
import com.jessegrabowski.irc.protocol.IRCCapability;
import com.jessegrabowski.irc.protocol.IRCMessageFactory0;
import com.jessegrabowski.irc.protocol.IRCMessageFactory1;
import com.jessegrabowski.irc.protocol.IRCMessageFactory2;
import com.jessegrabowski.irc.protocol.IRCMessageFactory3;
import com.jessegrabowski.irc.protocol.IRCMessageFactory6;
import com.jessegrabowski.irc.protocol.IRCMessageMarshaller;
import com.jessegrabowski.irc.protocol.IRCMessageUnmarshaller;
import com.jessegrabowski.irc.protocol.model.*;
import com.jessegrabowski.irc.server.state.InvalidPasswordException;
import com.jessegrabowski.irc.server.state.ServerState;
import com.jessegrabowski.irc.server.state.StateInvariantException;
import com.jessegrabowski.irc.util.Pair;
import com.jessegrabowski.irc.util.StateGuard;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Gatherers;

public class IRCServerEngine implements Closeable {

    private static final Instant START_TIME = Instant.now();

    private static final Logger LOG = Logger.getLogger(IRCServerEngine.class.getName());

    private static final IRCMessageUnmarshaller UNMARSHALLER = new IRCMessageUnmarshaller();
    private static final IRCMessageMarshaller MARSHALLER = new IRCMessageMarshaller();

    private final AtomicReference<IRCServerEngineState> engineState =
            new AtomicReference<>(IRCServerEngineState.ACTIVE);

    private final StateGuard<ServerState> serverStateGuard;
    private final ScheduledExecutorService executor;
    private final IRCServerProperties properties;

    // there's a lot going on here but I'll try to call out the important bits
    public IRCServerEngine(IRCServerProperties properties) throws IOException {
        this.properties = properties;

        IRCServerParameters parameters = IRCServerParametersLoader.load(properties.getIsupportProperties());

        ServerState state = new ServerState(properties);
        state.setParameters(parameters);

        this.serverStateGuard = new StateGuard<>(state);

        // using a single-threaded executor for engine tasks greatly simplifies our state management
        // without any real loss of performance (as the IRCConnection class handles additional threads for
        // client communication, we're just serializing state access)
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                1,
                Thread.ofPlatform()
                        .name("IRCClient-Server") // name the thread so it shows up nicely in our logs
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
        executor.execute(spy(() -> {
            serverStateGuard.bindToCurrentThread();
            executor.scheduleAtFixedRate(
                    this::ping, 0, properties.getPingFrequencyMilliseconds(), TimeUnit.MILLISECONDS);
        }));
        this.executor = executor;
    }

    public void accept(Socket socket) {
        executor.execute(spy(() -> {
            IRCConnection connection = new IRCConnection(socket);
            connection.addIngressHandler(line -> executor.execute(() -> handle(connection, line)));
            connection.addShutdownHandler(() -> executor.execute(() -> handleDisconnect(connection)));

            ServerState state = serverStateGuard.getState();
            state.connect(connection);

            try {
                connection.start();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error starting connection", e);
                connection.close();
            }
        }));
    }

    private <T extends IRCMessage> void send(
            IRCConnection connection, IRCMessage initiator, IRCMessageFactory0<T> factory) {
        T message = factory.create(
                null,
                makeTags(connection, initiator),
                makePrefixName(connection, initiator),
                makePrefixUser(connection, initiator),
                makePrefixHost(connection, initiator));
        connection.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A> void send(
            IRCConnection connection, IRCMessage initiator, A arg0, IRCMessageFactory1<T, A> factory) {
        T message = factory.create(
                null,
                makeTags(connection, initiator),
                makePrefixName(connection, initiator),
                makePrefixUser(connection, initiator),
                makePrefixHost(connection, initiator),
                arg0);
        connection.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B> void send(
            IRCConnection connection, IRCMessage initiator, A arg0, B arg1, IRCMessageFactory2<T, A, B> factory) {
        T message = factory.create(
                null,
                makeTags(connection, initiator),
                makePrefixName(connection, initiator),
                makePrefixUser(connection, initiator),
                makePrefixHost(connection, initiator),
                arg0,
                arg1);
        connection.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B, C> void send(
            IRCConnection connection,
            IRCMessage initiator,
            A arg0,
            B arg1,
            C arg2,
            IRCMessageFactory3<T, A, B, C> factory) {
        T message = factory.create(
                null,
                makeTags(connection, initiator),
                makePrefixName(connection, initiator),
                makePrefixUser(connection, initiator),
                makePrefixHost(connection, initiator),
                arg0,
                arg1,
                arg2);
        connection.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B, C, D, E, F> void send(
            IRCConnection connection,
            IRCMessage initiator,
            A arg0,
            B arg1,
            C arg2,
            D arg3,
            E arg4,
            F arg5,
            IRCMessageFactory6<T, A, B, C, D, E, F> factory) {
        T message = factory.create(
                null,
                makeTags(connection, initiator),
                makePrefixName(connection, initiator),
                makePrefixUser(connection, initiator),
                makePrefixHost(connection, initiator),
                arg0,
                arg1,
                arg2,
                arg3,
                arg4,
                arg5);
        connection.offer(MARSHALLER.marshal(message));
    }

    private SequencedMap<String, String> makeTags(IRCConnection connection, IRCMessage initiator) {
        return new LinkedHashMap<>();
    }

    private String makePrefixName(IRCConnection connection, IRCMessage initiator) {
        return null;
    }

    private String makePrefixUser(IRCConnection connection, IRCMessage initiator) {
        return null;
    }

    private String makePrefixHost(IRCConnection connection, IRCMessage initiator) {
        return null;
    }

    private void handleDisconnect(IRCConnection connection) {
        ServerState state = serverStateGuard.getState();
        state.disconnect(connection);
    }

    private void handle(IRCConnection client, String line) {
        ServerState state = serverStateGuard.getState();
        IRCMessage message = UNMARSHALLER.unmarshal(state.getParameters(), StandardCharsets.UTF_8, line);
        handle(client, message);
    }

    private void handle(IRCConnection connection, IRCMessage message) {
        try {
            switch (message) {
                case IRCMessageAWAY m -> {}
                case IRCMessageCAPEND m -> handle(connection, m);
                case IRCMessageCAPLISTRequest m -> {}
                case IRCMessageCAPLSRequest m -> handle(connection, m);
                case IRCMessageCAPREQ m -> handle(connection, m);
                case IRCMessageJOIN0 m -> {}
                case IRCMessageJOINNormal m -> {}
                case IRCMessageKICK m -> {}
                case IRCMessageKILL m -> {}
                case IRCMessageMODE m -> {}
                case IRCMessageNAMES m -> {}
                case IRCMessageNICK m -> handle(connection, m);
                case IRCMessageNOTICE m -> {}
                case IRCMessageOPER m -> {}
                case IRCMessagePART m -> {}
                case IRCMessagePASS m -> handle(connection, m);
                case IRCMessagePING m -> handle(connection, m);
                case IRCMessagePONG m -> handle(connection, m);
                case IRCMessagePRIVMSG m -> {}
                case IRCMessageQUIT m -> {}
                case IRCMessageTOPIC m -> {}
                case IRCMessageUSER m -> handle(connection, m);
                default -> {
                    send(connection, message, "NOT IMPLEMENTED", IRCMessageERROR::new);
                }
            }
        } catch (InvalidPasswordException e) {
            send(connection, message, "*", IRCMessage464::new);
            send(connection, message, "Invalid password", IRCMessageERROR::new);
            connection.closeDeferred();
        } catch (StateInvariantException e) {
            send(connection, message, e.getFactory());
        }
    }

    private void handle(IRCConnection connection, IRCMessageCAPEND message) {
        ServerState state = serverStateGuard.getState();
        state.endCapabilityNegotiation(connection);
        if (state.tryFinishRegistration(connection)) {
            sendWelcome(connection, message);
        }
    }

    private void handle(IRCConnection connection, IRCMessageCAPLSRequest message) {
        ServerState state = serverStateGuard.getState();
        if (!state.isRegistered(connection)) {
            state.startCapabilityNegotiation(connection);
        }
        LinkedHashMap<String, String> capabilities = new LinkedHashMap<>();
        for (IRCCapability capability : IRCCapability.values()) {
            capabilities.put(capability.getCapabilityName(), null);
        }
        send(connection, message, state.getNickname(connection), false, capabilities, IRCMessageCAPLSResponse::new);
    }

    private void handle(IRCConnection connection, IRCMessageCAPREQ message) {
        ServerState state = serverStateGuard.getState();
        List<IRCCapability> knownEnableCapabilities = new ArrayList<>();
        List<IRCCapability> knownDisableCapabilities = new ArrayList<>();
        List<String> unknownEnableCapabilities = new ArrayList<>();
        List<String> unknownDisableCapabilities = new ArrayList<>();
        for (String capability : message.getEnableCapabilities()) {
            IRCCapability.forName(capability)
                    .ifPresentOrElse(knownEnableCapabilities::add, () -> unknownEnableCapabilities.add(capability));
        }
        for (String capability : message.getDisableCapabilities()) {
            IRCCapability.forName(capability)
                    .ifPresentOrElse(knownDisableCapabilities::add, () -> unknownDisableCapabilities.add(capability));
        }
        knownEnableCapabilities.forEach(cap -> state.addCapability(connection, cap));
        knownDisableCapabilities.forEach(cap -> state.removeCapability(connection, cap));
        if (!knownEnableCapabilities.isEmpty() || !knownDisableCapabilities.isEmpty()) {
            send(
                    connection,
                    message,
                    state.getNickname(connection),
                    knownEnableCapabilities.stream()
                            .map(IRCCapability::getCapabilityName)
                            .toList(),
                    knownDisableCapabilities.stream()
                            .map(IRCCapability::getCapabilityName)
                            .toList(),
                    IRCMessageCAPACK::new);
        }
        if (!unknownEnableCapabilities.isEmpty() || !unknownDisableCapabilities.isEmpty()) {
            send(
                    connection,
                    message,
                    state.getNickname(connection),
                    unknownEnableCapabilities,
                    unknownDisableCapabilities,
                    IRCMessageCAPNAK::new);
        }
    }

    private void handle(IRCConnection connection, IRCMessageNICK message)
            throws StateInvariantException, InvalidPasswordException {
        ServerState state = serverStateGuard.getState();
        Pair<String, String> result = state.setNickname(connection, message.getNick());
        send(connection, message, result.right(), IRCMessageNICK::new);
        if (state.tryFinishRegistration(connection)) {
            sendWelcome(connection, message);
        }
    }

    private void handle(IRCConnection connection, IRCMessagePASS message)
            throws StateInvariantException, InvalidPasswordException {
        ServerState state = serverStateGuard.getState();
        state.checkPassword(connection, message.getPass());
    }

    private void handle(IRCConnection connection, IRCMessagePING message) {
        send(connection, message, (String) null, message.getToken(), IRCMessagePONG::new);
    }

    private void handle(IRCConnection connection, IRCMessagePONG message) {
        ServerState state = serverStateGuard.getState();
        try {
            long token = Long.parseLong(message.getToken());
            long lastPing = state.getLastPing(connection);
            if (token <= lastPing) {
                state.setLastPong(connection, token);
            }
        } catch (NumberFormatException e) {
            LOG.log(Level.FINE, "Invalid PONG token", e);
        }
    }

    private void handle(IRCConnection connection, IRCMessageUSER message)
            throws StateInvariantException, InvalidPasswordException {
        ServerState state = serverStateGuard.getState();
        state.setUserInfo(connection, message.getUser(), message.getRealName());
        if (state.tryFinishRegistration(connection)) {
            sendWelcome(connection, message);
        }
    }

    private void sendWelcome(IRCConnection connection, IRCMessage initiator) {
        ServerState state = serverStateGuard.getState();
        IRCServerParameters parameters = state.getParameters();
        send(
                connection,
                initiator,
                state.getNickname(connection),
                "Welcome to the %s Network, %s".formatted(parameters.getNetwork(), state.getNickname(connection)),
                IRCMessage001::new);
        send(
                connection,
                initiator,
                state.getNickname(connection),
                "Your host is RitsIRC, running version 1.0.0",
                IRCMessage002::new);
        send(
                connection,
                initiator,
                state.getNickname(connection),
                "This server was created %s".formatted(DateTimeFormatter.ISO_INSTANT.format(START_TIME)),
                IRCMessage003::new);
        String availableUserModes = "io";
        String availableChannelModes = "biklmnoprst";
        send(
                connection,
                initiator,
                state.getNickname(connection),
                "RitsIRC",
                "1.0.0",
                availableUserModes,
                availableChannelModes,
                null,
                IRCMessage004::new);
        send005(connection, initiator);
    }

    private void send005(IRCConnection connection, IRCMessage initiator) {
        ServerState state = serverStateGuard.getState();
        var chunkedEntries = IRCServerParametersMarshaller.marshal(state.getParameters()).sequencedEntrySet().stream()
                .gather(Gatherers.windowFixed(13))
                .toList();
        for (List<Map.Entry<String, String>> chunk : chunkedEntries) {
            SequencedMap<String, String> map =
                    chunk.stream().collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
            send(
                    connection,
                    initiator,
                    state.getNickname(connection),
                    map,
                    "are supported by this server",
                    IRCMessage005::new);
        }
    }

    private void ping() {
        ServerState state = serverStateGuard.getState();
        Set<IRCConnection> connections = state.getConnections();
        long now = System.currentTimeMillis();
        for (IRCConnection connection : connections) {
            long lastPong = state.getLastPong(connection);
            long lastPing = state.getLastPing(connection);
            if (lastPing - lastPong > properties.getMaxIdleMilliseconds()) {
                send(
                        connection,
                        null,
                        "No PONG in %dms".formatted(properties.getMaxIdleMilliseconds()),
                        IRCMessageERROR::new);
                connection.closeDeferred();
                continue;
            }
            state.setLastPing(connection, now);
            send(connection, null, String.valueOf(now), IRCMessagePING::new);
        }
    }

    @Override
    public void close() {
        IRCServerEngineState oldState = engineState.getAndSet(IRCServerEngineState.CLOSED);
        if (oldState == IRCServerEngineState.CLOSED) {
            return;
        }

        LOG.info("IRCClientEngine shutting down...");

        executor.shutdownNow();
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

    private enum IRCServerEngineState {
        ACTIVE,
        CLOSED
    }
}
