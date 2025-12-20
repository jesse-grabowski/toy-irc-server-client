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
import com.jessegrabowski.irc.protocol.IRCMessageFactory4;
import com.jessegrabowski.irc.protocol.IRCMessageFactory5;
import com.jessegrabowski.irc.protocol.IRCMessageFactory6;
import com.jessegrabowski.irc.protocol.IRCMessageMarshaller;
import com.jessegrabowski.irc.protocol.IRCMessageUnmarshaller;
import com.jessegrabowski.irc.protocol.model.*;
import com.jessegrabowski.irc.server.state.InvalidPasswordException;
import com.jessegrabowski.irc.server.state.MessageSource;
import com.jessegrabowski.irc.server.state.MessageTarget;
import com.jessegrabowski.irc.server.state.ServerChannel;
import com.jessegrabowski.irc.server.state.ServerChannelMembership;
import com.jessegrabowski.irc.server.state.ServerState;
import com.jessegrabowski.irc.server.state.ServerUser;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        ServerState state = new ServerState(properties, parameters);

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
            IRCConnection receiver, MessageSource sender, IRCMessage initiator, IRCMessageFactory0<T> factory) {
        T message = factory.create(
                null,
                makeTags(receiver, false, initiator),
                makePrefixName(sender),
                makePrefixUser(sender),
                makePrefixHost(sender));
        receiver.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A> void send(
            IRCConnection receiver,
            MessageSource sender,
            IRCMessage initiator,
            A arg0,
            IRCMessageFactory1<T, A> factory) {
        T message = factory.create(
                null,
                makeTags(receiver, false, initiator),
                makePrefixName(sender),
                makePrefixUser(sender),
                makePrefixHost(sender),
                arg0);
        receiver.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B> void send(
            IRCConnection receiver,
            MessageSource sender,
            IRCMessage initiator,
            A arg0,
            B arg1,
            IRCMessageFactory2<T, A, B> factory) {
        T message = factory.create(
                null,
                makeTags(receiver, false, initiator),
                makePrefixName(sender),
                makePrefixUser(sender),
                makePrefixHost(sender),
                arg0,
                arg1);
        receiver.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B> void echo(
            IRCConnection receiver,
            MessageSource sender,
            IRCMessage initiator,
            A arg0,
            B arg1,
            IRCMessageFactory2<T, A, B> factory) {
        T message = factory.create(
                null,
                makeTags(receiver, true, initiator),
                makePrefixName(sender),
                makePrefixUser(sender),
                makePrefixHost(sender),
                arg0,
                arg1);
        receiver.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B, C> void send(
            IRCConnection receiver,
            MessageSource sender,
            IRCMessage initiator,
            A arg0,
            B arg1,
            C arg2,
            IRCMessageFactory3<T, A, B, C> factory) {
        T message = factory.create(
                null,
                makeTags(receiver, false, initiator),
                makePrefixName(sender),
                makePrefixUser(sender),
                makePrefixHost(sender),
                arg0,
                arg1,
                arg2);
        receiver.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B, C, D> void send(
            IRCConnection receiver,
            MessageSource sender,
            IRCMessage initiator,
            A arg0,
            B arg1,
            C arg2,
            D arg3,
            IRCMessageFactory4<T, A, B, C, D> factory) {
        T message = factory.create(
                null,
                makeTags(receiver, false, initiator),
                makePrefixName(sender),
                makePrefixUser(sender),
                makePrefixHost(sender),
                arg0,
                arg1,
                arg2,
                arg3);
        receiver.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B, C, D, E> void send(
            IRCConnection receiver,
            MessageSource sender,
            IRCMessage initiator,
            A arg0,
            B arg1,
            C arg2,
            D arg3,
            E arg4,
            IRCMessageFactory5<T, A, B, C, D, E> factory) {
        T message = factory.create(
                null,
                makeTags(receiver, false, initiator),
                makePrefixName(sender),
                makePrefixUser(sender),
                makePrefixHost(sender),
                arg0,
                arg1,
                arg2,
                arg3,
                arg4);
        receiver.offer(MARSHALLER.marshal(message));
    }

    private <T extends IRCMessage, A, B, C, D, E, F> void send(
            IRCConnection receiver,
            MessageSource sender,
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
                makeTags(receiver, false, initiator),
                makePrefixName(sender),
                makePrefixUser(sender),
                makePrefixHost(sender),
                arg0,
                arg1,
                arg2,
                arg3,
                arg4,
                arg5);
        receiver.offer(MARSHALLER.marshal(message));
    }

    private SequencedMap<String, String> makeTags(IRCConnection connection, boolean isEcho, IRCMessage initiator) {
        ServerState state = serverStateGuard.getState();
        if (!state.hasCapability(connection, IRCCapability.MESSAGE_TAGS)
                && !state.hasCapability(connection, IRCCapability.ECHO_MESSAGE)) {
            return new LinkedHashMap<>();
        }

        SequencedMap<String, String> tags = new LinkedHashMap<>();
        if (initiator != null) {
            for (Map.Entry<String, String> entry : initiator.getTags().entrySet()) {
                if (isEcho || entry.getKey().startsWith("+")) {
                    tags.put(entry.getKey(), entry.getValue());
                }
            }
        }

        if (state.hasCapability(connection, IRCCapability.SERVER_TIME)) {
            tags.put("time", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        }

        return tags;
    }

    private String makePrefixName(MessageSource sender) {
        return sender.getNickname();
    }

    private String makePrefixUser(MessageSource sender) {
        return sender.getUsername();
    }

    private String makePrefixHost(MessageSource sender) {
        return sender.includeHostname() ? properties.getHost() : null;
    }

    private MessageSource server() {
        return new MessageSource.ServerMessageSource(properties.getHost());
    }

    private void handleDisconnect(IRCConnection connection) {
        ServerState state = serverStateGuard.getState();
        try {
            ServerUser me = state.getUserForConnection(connection);
            MessageTarget target = state.getWatchers(me, false);
            sendToTarget(
                    me,
                    null,
                    target,
                    (raw, tags, nick, user, host) ->
                            new IRCMessageQUIT(raw, tags, nick, user, host, "user disconnected"));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error sending QUIT message during disconnect", e);
        }
        state.disconnect(connection);
    }

    private void handle(IRCConnection client, String line) {
        ServerState state = serverStateGuard.getState();
        IRCMessage message = UNMARSHALLER.unmarshal(state.getParameters(), StandardCharsets.UTF_8, line);
        handle(client, message);
    }

    private void handle(IRCConnection connection, IRCMessage message) {
        try {
            ServerState state = serverStateGuard.getState();
            ServerUser me = state.getUserForConnection(connection);
            switch (message) {
                case IRCMessageAWAY m -> {}
                case IRCMessageCAPEND m -> handle(connection, m);
                case IRCMessageCAPLISTRequest m -> {}
                case IRCMessageCAPLSRequest m -> handle(connection, m);
                case IRCMessageCAPREQ m -> handle(connection, m);
                case IRCMessageJOIN0 m -> {}
                case IRCMessageJOINNormal m -> handle(connection, m);
                case IRCMessageKICK m -> {}
                case IRCMessageKILL m -> {}
                case IRCMessageMODE m -> {}
                case IRCMessageNAMES m -> {}
                case IRCMessageNICK m -> handle(connection, m);
                case IRCMessageNOTICE m -> handle(connection, m);
                case IRCMessageOPER m -> {}
                case IRCMessagePART m -> handle(connection, m);
                case IRCMessagePASS m -> handle(connection, m);
                case IRCMessagePING m -> handle(connection, m);
                case IRCMessagePONG m -> handle(connection, m);
                case IRCMessagePRIVMSG m -> handle(connection, m);
                case IRCMessageQUIT m -> {}
                case IRCMessageTOPIC m -> handle(connection, m);
                case IRCMessageUSER m -> handle(connection, m);
                case IRCMessageTooLong m -> {
                    send(
                            connection,
                            server(),
                            message,
                            Objects.requireNonNullElse(me.getNickname(), "*"),
                            "Input line was too long",
                            IRCMessage417::new);
                }
                case IRCMessageNotEnoughParameters m -> {
                    send(
                            connection,
                            server(),
                            message,
                            Objects.requireNonNullElse(me.getNickname(), "*"),
                            m.getCommand(),
                            "Not enough parameters",
                            IRCMessage461::new);
                }
                default -> {
                    send(connection, server(), message, "NOT IMPLEMENTED", IRCMessageERROR::new);
                }
            }
        } catch (InvalidPasswordException e) {
            send(connection, server(), message, "*", IRCMessage464::new);
            send(connection, server(), message, "Invalid password", IRCMessageERROR::new);
            connection.closeDeferred();
        } catch (StateInvariantException e) {
            send(connection, server(), message, e.getFactory());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error handling message (%s)".formatted(message.getRawMessage()), e);
            send(connection, server(), message, e.getMessage(), IRCMessageERROR::new);
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
        send(
                connection,
                server(),
                message,
                state.getNickname(connection),
                false,
                capabilities,
                IRCMessageCAPLSResponse::new);
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
                    server(),
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
                    server(),
                    message,
                    state.getNickname(connection),
                    unknownEnableCapabilities,
                    unknownDisableCapabilities,
                    IRCMessageCAPNAK::new);
        }
    }

    private void handle(IRCConnection connection, IRCMessageJOINNormal message) throws Exception {
        serverStateGuard.doTransactionally(state -> {
            for (int i = 0; i < message.getChannels().size(); i++) {
                String channel = message.getChannels().get(i);
                String key = message.getKeys().size() > i ? message.getKeys().get(i) : null;
                state.joinChannel(connection, channel, key);
            }
        });

        ServerState state = serverStateGuard.getState();
        ServerUser me = state.getUserForConnection(connection);
        for (String channelName : message.getChannels()) {
            ServerChannel channel = state.getExistingChannel(connection, channelName);
            MessageTarget watchers = state.getWatchers(channel);
            sendToTarget(
                    me,
                    message,
                    watchers,
                    (raw, tags, nick, user, host) -> new IRCMessageJOINNormal(
                            raw, tags, nick, user, host, List.of(channel.getName()), List.of()));
            sendTopic(connection, message, channelName, false);
            sendNames(connection, message, channelName);
        }
    }

    private void handle(IRCConnection connection, IRCMessageNICK message)
            throws StateInvariantException, InvalidPasswordException {
        ServerState state = serverStateGuard.getState();
        ServerUser me = state.getUserForConnection(connection);
        MessageSource sender = state.isRegistered(connection)
                ? new MessageSource.NamedMessageSource(me.getNickname(), me.getUsername())
                : server();
        MessageTarget watchers = state.getWatchers(me, true);
        Pair<String, String> result = state.setNickname(connection, message.getNick());
        sendToTarget(
                sender,
                message,
                watchers,
                (raw, tags, nick, user, host) -> new IRCMessageNICK(raw, tags, nick, user, host, result.right()));
        if (state.tryFinishRegistration(connection)) {
            sendWelcome(connection, message);
        }
    }

    private void handle(IRCConnection connection, IRCMessageNOTICE message) throws StateInvariantException {
        ServerState state = serverStateGuard.getState();
        ServerUser me = state.getUserForConnection(connection);
        List<MessageTarget> targets = new ArrayList<>();
        for (String target : message.getTargets()) {
            try {
                targets.add(state.resolveMask(connection, target));
            } catch (Exception e) {
                LOG.log(Level.FINE, "Invalid NOTICE target '%s', silently ignored".formatted(target), e);
                return;
            }
        }
        for (MessageTarget target : targets) {
            sendToTarget(
                    me,
                    message,
                    target,
                    (raw, tags, nick, user, host) -> new IRCMessageNOTICE(
                            raw, tags, nick, user, host, List.of(target.getMask()), message.getMessage()));
        }
        if (me.hasCapability(IRCCapability.ECHO_MESSAGE)) {
            echo(connection, me, message, message.getTargets(), message.getMessage(), IRCMessageNOTICE::new);
        }
    }

    private void handle(IRCConnection connection, IRCMessagePART message) throws Exception {
        List<Runnable> results = new ArrayList<>();
        serverStateGuard.doTransactionally(state -> {
            ServerUser me = state.getUserForConnection(connection);
            for (String channelName : message.getChannels()) {
                ServerChannel channel = state.findChannel(channelName);
                if (channel == null) {
                    results.add(() -> send(
                            connection,
                            server(),
                            message,
                            me.getNickname(),
                            channelName,
                            "No channel named '%s'".formatted(channelName),
                            IRCMessage403::new));
                    continue;
                }
                if (!channel.getMembers().contains(me)) {
                    results.add(() ->
                            send(connection, server(), message, me.getNickname(), channelName, IRCMessage442::new));
                    continue;
                }

                MessageTarget watchers = state.getWatchers(channel);
                state.partChannel(connection, channelName);
                results.add(() -> sendToTarget(
                        me,
                        message,
                        watchers,
                        (raw, tags, nick, user, host) -> new IRCMessagePART(
                                raw, tags, nick, user, host, List.of(channel.getName()), message.getReason())));
            }
        });

        results.forEach(Runnable::run);
    }

    private void handle(IRCConnection connection, IRCMessagePASS message)
            throws StateInvariantException, InvalidPasswordException {
        ServerState state = serverStateGuard.getState();
        state.checkPassword(connection, message.getPass());
    }

    private void handle(IRCConnection connection, IRCMessagePING message) {
        if (message.getToken() == null || message.getToken().isEmpty()) {
            ServerState state = serverStateGuard.getState();
            ServerUser me = state.getUserForConnection(connection);
            send(
                    connection,
                    server(),
                    message,
                    Objects.requireNonNullElse(me.getNickname(), "*"),
                    message.getCommand(),
                    "Not enough parameters",
                    IRCMessage461::new);
            return;
        }
        send(connection, server(), message, properties.getServer(), message.getToken(), IRCMessagePONG::new);
    }

    private void handle(IRCConnection connection, IRCMessagePONG message) {
        ServerState state = serverStateGuard.getState();
        try {
            long token = Long.parseLong(message.getToken());
            long lastPing = state.getLastPing(connection);
            long lastPong = state.getLastPong(connection);
            if (token <= lastPing && token >= lastPong) {
                state.setLastPong(connection, token);
            }
        } catch (NumberFormatException e) {
            LOG.log(Level.FINE, "Invalid PONG token", e);
        }
    }

    private void handle(IRCConnection connection, IRCMessagePRIVMSG message) throws StateInvariantException {
        ServerState state = serverStateGuard.getState();
        ServerUser me = state.getUserForConnection(connection);
        if (message.getMessage() == null || message.getMessage().isEmpty()) {
            send(connection, server(), message, me.getNickname(), "No text to send", IRCMessage412::new);
            return;
        }
        List<MessageTarget> targets = new ArrayList<>();
        for (String target : message.getTargets()) {
            targets.add(state.resolveMask(connection, target));
        }
        for (MessageTarget target : targets) {
            sendToTarget(
                    me,
                    message,
                    target,
                    (raw, tags, nick, user, host) -> new IRCMessagePRIVMSG(
                            raw, tags, nick, user, host, List.of(target.getMask()), message.getMessage()));
            if (me.hasCapability(IRCCapability.ECHO_MESSAGE)) {
                echo(connection, me, message, List.of(target.getMask()), message.getMessage(), IRCMessagePRIVMSG::new);
            }
        }
    }

    private void handle(IRCConnection connection, IRCMessageTOPIC message) throws StateInvariantException {
        if (message.getTopic() == null) {
            sendTopic(connection, message, message.getChannel(), true);
            return;
        }

        ServerState state = serverStateGuard.getState();
        ServerChannel channel = state.getExistingChannel(connection, message.getChannel());
        state.setChannelTopic(connection, channel, message.getTopic());
        ServerUser me = state.getUserForConnection(connection);
        MessageTarget target = state.getWatchers(channel);
        sendToTarget(
                me,
                message,
                target,
                (raw, tags, nick, user, host) ->
                        new IRCMessageTOPIC(raw, tags, nick, user, host, channel.getName(), message.getTopic()));
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
                server(),
                initiator,
                state.getNickname(connection),
                "Welcome to the %s Network, %s".formatted(parameters.getNetwork(), state.getNickname(connection)),
                IRCMessage001::new);
        send(
                connection,
                server(),
                initiator,
                state.getNickname(connection),
                "Your host is RitsIRC, running version 1.0.0",
                IRCMessage002::new);
        send(
                connection,
                server(),
                initiator,
                state.getNickname(connection),
                "This server was created %s".formatted(DateTimeFormatter.ISO_INSTANT.format(START_TIME)),
                IRCMessage003::new);
        String availableUserModes = "io";
        String availableChannelModes = "biklmnoprst";
        send(
                connection,
                server(),
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
                    server(),
                    initiator,
                    state.getNickname(connection),
                    map,
                    "are supported by this server",
                    IRCMessage005::new);
        }
    }

    private void sendTopic(IRCConnection connection, IRCMessage initiator, String channelName, boolean sendNoTopic)
            throws StateInvariantException {
        ServerState state = serverStateGuard.getState();
        ServerUser me = state.getUserForConnection(connection);
        ServerChannel channel = state.getExistingChannel(connection, channelName);
        if (channel.getTopic() != null) {
            send(
                    connection,
                    server(),
                    initiator,
                    me.getNickname(),
                    channel.getName(),
                    channel.getTopic(),
                    IRCMessage332::new);
            send(
                    connection,
                    server(),
                    initiator,
                    me.getNickname(),
                    channel.getName(),
                    channel.getTopicSetBy().nickname(),
                    channel.getTopicSetAt(),
                    IRCMessage333::new);
        } else if (sendNoTopic) {
            send(connection, server(), initiator, me.getNickname(), channel.getName(), IRCMessage331::new);
        }
    }

    private void sendNames(IRCConnection connection, IRCMessage initiator, String channelName)
            throws StateInvariantException {
        ServerState state = serverStateGuard.getState();
        ServerUser me = state.getUserForConnection(connection);
        ServerChannel channel = state.getExistingChannel(connection, channelName);
        List<String> nicks = new ArrayList<>();
        List<Character> modes = new ArrayList<>();
        IRCServerParameters parameters = state.getParameters();
        int namesPerMessage = 400 / parameters.getNickLength();
        String channelStatus = channel.checkFlag('s') ? "@" : channel.checkFlag('p') ? "*" : "=";
        for (ServerUser member : channel.getMembers().stream()
                .sorted(Comparator.comparing(ServerUser::getNickname))
                .toList()) {
            ServerChannelMembership membership = channel.getMembership(member);
            nicks.add(member.getNickname());
            modes.add(membership.getHighestPowerPrefix());
            if (nicks.size() % namesPerMessage == 0) {
                send(
                        connection,
                        server(),
                        initiator,
                        me.getNickname(),
                        channelStatus,
                        channel.getName(),
                        nicks,
                        modes,
                        IRCMessage353::new);
                nicks.clear();
                modes.clear();
            }
        }
        if (!nicks.isEmpty()) {
            send(
                    connection,
                    server(),
                    initiator,
                    me.getNickname(),
                    channelStatus,
                    channel.getName(),
                    nicks,
                    modes,
                    IRCMessage353::new);
        }
        send(connection, server(), initiator, me.getNickname(), channel.getName(), IRCMessage366::new);
    }

    private <T extends IRCMessage> void sendToTarget(
            MessageSource messageSource, IRCMessage initiator, MessageTarget target, IRCMessageFactory0<T> factory) {
        for (IRCConnection targetConnection : target.getConnections()) {
            send(targetConnection, messageSource, initiator, factory);
        }
    }

    private void ping() {
        ServerState state = serverStateGuard.getState();
        Set<IRCConnection> connections = state.getConnections();
        long now = System.currentTimeMillis();
        for (IRCConnection connection : connections) {
            long lastPong = state.getLastPong(connection);
            if (now - lastPong > properties.getMaxIdleMilliseconds()) {
                send(
                        connection,
                        server(),
                        null,
                        "No PONG in %dms".formatted(properties.getMaxIdleMilliseconds()),
                        IRCMessageERROR::new);
                connection.closeDeferred();
                continue;
            }
            state.setLastPing(connection, now);
            send(connection, server(), null, String.valueOf(now), IRCMessagePING::new);
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
