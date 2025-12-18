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
import com.jessegrabowski.irc.protocol.IRCMessageFactory0;
import com.jessegrabowski.irc.protocol.IRCMessageFactory1;
import com.jessegrabowski.irc.protocol.IRCMessageFactory2;
import com.jessegrabowski.irc.protocol.IRCMessageMarshaller;
import com.jessegrabowski.irc.protocol.IRCMessageUnmarshaller;
import com.jessegrabowski.irc.protocol.model.*;
import com.jessegrabowski.irc.util.StateGuard;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IRCServerEngine implements Closeable {

    private static final Logger LOG = Logger.getLogger(IRCServerEngine.class.getName());

    private static final IRCMessageUnmarshaller UNMARSHALLER = new IRCMessageUnmarshaller();
    private static final IRCMessageMarshaller MARSHALLER = new IRCMessageMarshaller();

    private final AtomicReference<IRCServerEngineState> engineState =
            new AtomicReference<>(IRCServerEngineState.ACTIVE);

    private final StateGuard<IRCServerState> serverStateGuard;
    private final ScheduledExecutorService executor;
    private final IRCServerProperties properties;

    // there's a lot going on here but I'll try to call out the important bits
    public IRCServerEngine(IRCServerProperties properties) throws IOException {
        this.properties = properties;

        IRCServerParameters parameters = IRCServerParametersLoader.load(properties.getIsupportProperties());

        IRCServerState state = new IRCServerState();
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
        executor.execute(spy(serverStateGuard::bindToCurrentThread));
        this.executor = executor;
    }

    public void accept(Socket socket) {
        executor.execute(spy(() -> {
            IRCConnection connection = new IRCConnection(socket);
            connection.addIngressHandler(line -> executor.execute(() -> handle(connection, line)));
            connection.addShutdownHandler(() -> executor.execute(() -> handleDisconnect(connection)));

            IRCServerState state = serverStateGuard.getState();
            state.addConnection(connection);

            try {
                connection.start();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error starting connection", e);
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
        IRCServerState state = serverStateGuard.getState();
        state.removeConnection(connection);
    }

    private void handle(IRCConnection client, String line) {
        IRCServerState state = serverStateGuard.getState();
        IRCMessage message = UNMARSHALLER.unmarshal(state.getParameters(), StandardCharsets.UTF_8, line);
        handle(client, message);
    }

    private void handle(IRCConnection client, IRCMessage message) {
        switch (message) {
            case IRCMessageAWAY m -> {}
            case IRCMessageCAPEND m -> {}
            case IRCMessageCAPLISTRequest m -> {}
            case IRCMessageCAPLSRequest m -> {}
            case IRCMessageCAPREQ m -> {}
            case IRCMessageJOIN0 m -> {}
            case IRCMessageJOINNormal m -> {}
            case IRCMessageKICK m -> {}
            case IRCMessageKILL m -> {}
            case IRCMessageMODE m -> {}
            case IRCMessageNAMES m -> {}
            case IRCMessageNICK m -> handle(client, m);
            case IRCMessageNOTICE m -> {}
            case IRCMessageOPER m -> {}
            case IRCMessagePART m -> {}
            case IRCMessagePASS m -> handle(client, m);
            case IRCMessagePONG m -> {}
            case IRCMessagePRIVMSG m -> {}
            case IRCMessageQUIT m -> {}
            case IRCMessageTOPIC m -> {}
            case IRCMessageUSER m -> handle(client, m);
            default -> {
                send(client, message, "NOT IMPLEMENTED", IRCMessageERROR::new);
            }
        }
    }

    private void handle(IRCConnection client, IRCMessageNICK message) {
        IRCServerState state = serverStateGuard.getState();
        String nickname = message.getNick();
        if (nickname.charAt(0) == ':'
                || nickname.contains(" ")
                || state.getParameters().getChannelTypes().stream().anyMatch(c -> nickname.charAt(0) == c)) {
            send(client, message, "*", nickname, IRCMessage432::new);
        } else if (!state.isNicknameAvailable(nickname)) {
            send(client, message, "*", nickname, IRCMessage433::new);
        } else {
            send(client, message, nickname, IRCMessageNICK::new);
            boolean registered = state.isConnectionRegistered(client, properties.getPassword() != null);
            state.setConnectionNickname(client, nickname);
            if (!registered && state.isConnectionRegistered(client, properties.getPassword() != null)) {
                sendWelcome(client, message);
            }
        }
    }

    private void handle(IRCConnection client, IRCMessagePASS message) {
        IRCServerState state = serverStateGuard.getState();
        if (!Objects.equals(message.getPass(), properties.getPassword())) {
            send(client, null, "Incorrect password", IRCMessage464::new);
            send(client, null, "Incorrect password", IRCMessageERROR::new);
            client.closeDeferred();
        } else if (state.isConnectionRegistered(client, properties.getPassword() != null)) {
            send(client, null, state.getConnectionNickname(client), IRCMessage462::new);
        } else {
            state.setConnectionPassword(client, true);
            if (state.isConnectionRegistered(client, properties.getPassword() != null)) {
                sendWelcome(client, message);
            }
        }
    }

    private void handle(IRCConnection client, IRCMessageUSER message) {
        IRCServerState state = serverStateGuard.getState();
        String truncatedUser = message.getUser()
                .substring(
                        0,
                        Math.min(
                                message.getUser().length(),
                                state.getParameters().getUserLength()));
        if (state.isConnectionRegistered(client, properties.getPassword() != null)) {
            send(client, null, state.getConnectionNickname(client), IRCMessage462::new);
        } else {
            state.setConnectionUser(client, truncatedUser);
            state.setConnectionRealName(client, message.getRealName());
            if (state.isConnectionRegistered(client, properties.getPassword() != null)) {
                sendWelcome(client, message);
            }
        }
    }

    private void sendWelcome(IRCConnection client, IRCMessage initiator) {
        IRCServerState state = serverStateGuard.getState();
        send(client, null, state.getConnectionNickname(client), "Welcome to RitsIRC", IRCMessage001::new);
        send(
                client,
                null,
                state.getConnectionNickname(client),
                "Your host is RitsIRC, running version 1.0,0",
                IRCMessage002::new);
        send(
                client,
                null,
                state.getConnectionNickname(client),
                "This server was created Wed Jan 01 00:00:00 1970",
                IRCMessage003::new);
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
