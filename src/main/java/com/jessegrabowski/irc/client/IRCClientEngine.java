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
package com.jessegrabowski.irc.client;

import static com.jessegrabowski.irc.client.tui.RichString.B;
import static com.jessegrabowski.irc.client.tui.RichString.b;
import static com.jessegrabowski.irc.client.tui.RichString.f;
import static com.jessegrabowski.irc.client.tui.RichString.j;
import static com.jessegrabowski.irc.client.tui.RichString.s;

import com.jessegrabowski.irc.client.command.model.*;
import com.jessegrabowski.irc.client.tui.RichString;
import com.jessegrabowski.irc.client.tui.TerminalMessage;
import com.jessegrabowski.irc.client.tui.TerminalUI;
import com.jessegrabowski.irc.network.IRCClientConnectionFactory;
import com.jessegrabowski.irc.network.IRCConnection;
import com.jessegrabowski.irc.protocol.*;
import com.jessegrabowski.irc.protocol.model.*;
import com.jessegrabowski.irc.server.IRCServerParameters;
import com.jessegrabowski.irc.server.IRCServerParametersUnmarshaller;
import com.jessegrabowski.irc.util.StateGuard;
import java.awt.Color;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IRCClientEngine implements Closeable {

    private static final DateTimeFormatter FRIENDLY_DATE_FORMAT = DateTimeFormatter.ofPattern(
                    "EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private static final Logger LOG = Logger.getLogger(IRCClientEngine.class.getName());
    private static final IRCMessageUnmarshaller UNMARSHALLER = new IRCMessageUnmarshaller();
    private static final IRCMessageMarshaller MARSHALLER = new IRCMessageMarshaller();

    private final AtomicReference<IRCConnection> connectionHolder = new AtomicReference<>();
    private final AtomicReference<IRCClientEngineState> engineState = new AtomicReference<>(IRCClientEngineState.NEW);
    private final StateGuard<IRCClientState> clientStateGuard = new StateGuard<>();

    private final ScheduledExecutorService executor;
    private final IRCClientProperties properties;
    private final TerminalUI terminal;

    // there's a lot going on here but I'll try to call out the important bits
    public IRCClientEngine(IRCClientProperties properties, TerminalUI terminal) {
        this.properties = properties;
        this.terminal = terminal;

        // using a single-threaded executor for engine tasks greatly simplifies our state management
        // without any real loss of performance (as the IRCConnection class handles additional threads for
        // server communication)
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                1,
                Thread.ofPlatform()
                        .name("IRCClient-Engine") // name the thread so it shows up nicely in our logs
                        .daemon(false) // we don't want the JVM to terminate until this thread dies so daemon =
                        // false
                        .uncaughtExceptionHandler((t, e) -> LOG.log(Level.SEVERE, "Error executing task", e))
                        .factory(),
                // the more important bit is that we're overriding the default exception behavior, the
                // logging just makes it a bit easier for us to spot stray messages during shutdown (which are
                // harmless)
                (task, exec) -> LOG.log(
                        exec.isShutdown() ? Level.FINE : Level.SEVERE,
                        "Task {0} rejected from {1}",
                        new Object[] {task, exec}));
        executor.execute(spy(() -> {
            // bind the state guard to the current (executor) thread
            // any access from other threads will raise an exception
            clientStateGuard.bindToCurrentThread();
            // finally, start the garbage collection process to clean up
            // users that we no longer have visibility for. This is nested
            // to avoid a (very unlikely) state-guard-related race condition
            executor.scheduleWithFixedDelay(
                    spy(() -> {
                        IRCClientState state = clientStateGuard.getState();
                        if (state != null) {
                            LOG.info("Garbage collecting IRC client state unused users / channels");
                            state.gc(System.currentTimeMillis()
                                    - Duration.ofMinutes(5).toMillis());
                        }
                    }),
                    0,
                    5,
                    TimeUnit.MINUTES);
        }));
        this.executor = executor;
    }

    private void connect() {
        if (!engineState.compareAndSet(IRCClientEngineState.DISCONNECTED, IRCClientEngineState.CONNECTING)) {
            IRCClientEngineState current = engineState.get();
            LOG.warning("Cannot create connection from state " + current);
            if (current == IRCClientEngineState.CONNECTED || current == IRCClientEngineState.REGISTERED) {
                terminal.println(makeSystemTerminalMessage(
                        "You are already connected to the server and must `/quit` before reconnecting"));
            }
            return;
        }
        connectionHolder.set(null);

        IRCConnection connection = null;
        try {
            connection = IRCClientConnectionFactory.create(
                    properties.getHost(),
                    properties.getPort(),
                    properties.getCharset(),
                    properties.getConnectTimeout(),
                    properties.getReadTimeout());
            connection.addShutdownHandler(this::afterDisconnect);
            connection.addIngressHandler(this::receive);
            connection.start();

            connectionHolder.set(connection);
            if (!engineState.compareAndSet(IRCClientEngineState.CONNECTING, IRCClientEngineState.CONNECTED)) {
                throw new IllegalStateException("inconsistent state while establishing IRC connection");
            }

            clientStateGuard.setState(new IRCClientState());

            String nick = NicknameGenerator.generate(properties.getNickname());
            send(new IRCMessageCAPLSRequest("302"));
            if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
                send(new IRCMessagePASS(properties.getPassword()));
            }
            send(new IRCMessageNICK(nick));
            send(new IRCMessageUSER(nick, properties.getRealName()));
            updateStatusAndPrompt();
        } catch (Exception e) {
            terminal.println(makeSystemTerminalMessage("Failed to connect to IRC server %s:%d, try again with /connect"
                    .formatted(properties.getHost().getHostName(), properties.getPort())));
            LOG.log(Level.WARNING, "Failed to connect to IRC server", e);
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception unused) {
                    // do nothing
                }
            }
            if (!engineState.compareAndSet(IRCClientEngineState.CONNECTING, IRCClientEngineState.DISCONNECTED)
                    && engineState.get() != IRCClientEngineState.CLOSED) {
                LOG.log(Level.SEVERE, "inconsistent state while establishing IRC connection");
                terminal.println(makeSystemTerminalMessage("Fatal error during connection establishment"));
                close();
            }
        }
    }

    private void disconnect() {
        IRCClientEngineState state = engineState.get();
        if (state == IRCClientEngineState.CLOSED) {
            return;
        }

        IRCConnection connection = connectionHolder.getAndSet(null);
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception unused) {
                // do nothing
            }
        }
    }

    // called by IRC connection as it closes
    private void afterDisconnect() {
        if (engineState.get() == IRCClientEngineState.CLOSED) {
            return;
        }
        executor.execute(() -> {
            IRCClientEngineState s = engineState.get();
            if (s == IRCClientEngineState.CLOSED) {
                return;
            }
            clientStateGuard.setState(null);
            if (!engineState.compareAndSet(s, IRCClientEngineState.DISCONNECTED)) {
                if (engineState.get() != IRCClientEngineState.CLOSED) {
                    throw new IllegalStateException("inconsistent state while disconnecting");
                }
            }
            updateStatusAndPrompt();
        });
    }

    public void accept(ClientCommand command) {
        if (engineState.get() != IRCClientEngineState.CLOSED) {
            executor.execute(spy(() -> handle(command)));
        }
    }

    private boolean send(String message) {
        IRCConnection connection = connectionHolder.get();
        if (connection == null) {
            return false;
        }
        return connection.offer(message);
    }

    private boolean send(IRCMessage message) {
        return send(MARSHALLER.marshal(message));
    }

    private void receive(String message) {
        if (engineState.get() == IRCClientEngineState.CLOSED) {
            return;
        }
        // use negotiated server parameters from RPL_ISUPPORT if available, otherwise
        // use some reasonable defaults. Unfortunately since this requires state we need
        // to handle parsing on the event loop, but it should be relatively quick
        executor.execute(spy(() -> {
            IRCClientState state = clientStateGuard.getState();
            if (state == null) {
                handle(UNMARSHALLER.unmarshal(new IRCServerParameters(), properties.getCharset(), message));
            } else {
                handle(UNMARSHALLER.unmarshal(state.getParameters(), properties.getCharset(), message));
            }
        }));
    }

    // note that I'm using sealed classes + switch expressions
    // to force compile-time handling of all subtypes
    // traditionally this would have been done with the
    // visitor pattern but we're living in the future
    private void handle(IRCMessage message) {
        switch (message) {
            case IRCMessageAWAY m -> handle(m);
            case IRCMessageCAPACK m -> handle(m);
            case IRCMessageCAPDEL m -> handle(m);
            case IRCMessageCAPEND m -> {
                /* ignore */
            }
            case IRCMessageCAPLISTRequest m -> {
                /* ignored */
            }
            case IRCMessageCAPLISTResponse m -> handle(m);
            case IRCMessageCAPLSRequest m -> {
                /* ignored */
            }
            case IRCMessageCAPLSResponse m -> handle(m);
            case IRCMessageCAPNAK m -> handle(m);
            case IRCMessageCAPNEW m -> handle(m);
            case IRCMessageCAPREQ m -> {
                /* ignore */
            }
            case IRCMessageERROR m -> handle(m);
            case IRCMessageJOIN0 m -> {
                /* ignore */
            }
            case IRCMessageJOINNormal m -> handle(m);
            case IRCMessageKICK m -> handle(m);
            case IRCMessageKILL m ->
                terminal.println(makeSystemTerminalMessage(s(f(m.getPrefixName()), " killed ", f(m.getNickname()))));
            case IRCMessageMODE m -> handle(m);
            case IRCMessageNAMES m -> {
                /* ignore */
            }
            case IRCMessageNICK m -> handle(m);
            case IRCMessageNOTICE m -> handle(m);
            case IRCMessageOPER m -> {
                /* ignore */
            }
            case IRCMessagePART m -> handle(m);
            case IRCMessagePASS m -> {
                /* ignore */
            }
            case IRCMessagePING m -> handle(m);
            case IRCMessagePONG m -> {
                /* ignore */
            }
            case IRCMessagePRIVMSG m -> handle(m);
            case IRCMessageQUIT m -> handle(m);
            case IRCMessageTOPIC m -> handle(m);
            case IRCMessageUSER m -> {
                /* ignore */
            }
            case IRCMessage001 m -> handle(m);
            case IRCMessage002 m -> terminal.println(makeSystemTerminalMessage(m.getMessage()));
            case IRCMessage003 m -> terminal.println(makeSystemTerminalMessage(m.getMessage()));
            case IRCMessage004 m -> {
                /* ignore */
            }
            case IRCMessage005 m -> handle(m);
            case IRCMessage010 m -> {
                /* ignore */
            }
            case IRCMessage212 m -> {
                /* ignore */
            }
            case IRCMessage219 m -> {
                /* ignore */
            }
            case IRCMessage221 m -> {
                /* ignore */
            }
            case IRCMessage242 m -> {
                /* ignore */
            }
            case IRCMessage251 m -> {
                /* ignore */
            }
            case IRCMessage252 m -> {
                /* ignore */
            }
            case IRCMessage253 m -> {
                /* ignore */
            }
            case IRCMessage254 m -> {
                /* ignore */
            }
            case IRCMessage255 m -> {
                /* ignore */
            }
            case IRCMessage256 m -> {
                /* ignore */
            }
            case IRCMessage257 m -> {
                /* ignore */
            }
            case IRCMessage258 m -> {
                /* ignore */
            }
            case IRCMessage259 m -> {
                /* ignore */
            }
            case IRCMessage263 m -> {
                /* ignore */
            }
            case IRCMessage265 m -> {
                /* ignore */
            }
            case IRCMessage266 m -> {
                /* ignore */
            }
            case IRCMessage276 m -> {
                /* ignore */
            }
            case IRCMessage301 m -> handle(m);
            case IRCMessage302 m -> handle(m);
            case IRCMessage305 m -> handle(m);
            case IRCMessage306 m -> handle(m);
            case IRCMessage307 m -> {
                /* ignore */
            }
            case IRCMessage311 m -> {
                /* ignore */
            }
            case IRCMessage312 m -> {
                /* ignore */
            }
            case IRCMessage313 m -> {
                /* ignore */
            }
            case IRCMessage314 m -> {
                /* ignore */
            }
            case IRCMessage315 m -> {
                /* ignore */
            }
            case IRCMessage317 m -> {
                /* ignore */
            }
            case IRCMessage318 m -> {
                /* ignore */
            }
            case IRCMessage319 m -> {
                /* ignore */
            }
            case IRCMessage320 m -> {
                /* ignore */
            }
            case IRCMessage321 m -> {
                /* ignore */
            }
            case IRCMessage322 m -> {
                /* ignore */
            }
            case IRCMessage323 m -> {
                /* ignore */
            }
            case IRCMessage324 m -> {
                /* ignore */
            }
            case IRCMessage329 m -> {
                /* ignore */
            }
            case IRCMessage330 m -> {
                /* ignore */
            }
            case IRCMessage331 m -> handle(m);
            case IRCMessage332 m -> handle(m);
            case IRCMessage333 m -> handle(m);
            case IRCMessage336 m -> {
                /* ignore */
            }
            case IRCMessage337 m -> {
                /* ignore */
            }
            case IRCMessage338 m -> {
                /* ignore */
            }
            case IRCMessage341 m -> {
                /* ignore */
            }
            case IRCMessage346 m -> {
                /* ignore */
            }
            case IRCMessage347 m -> {
                /* ignore */
            }
            case IRCMessage348 m -> {
                /* ignore */
            }
            case IRCMessage349 m -> {
                /* ignore */
            }
            case IRCMessage351 m -> {
                /* ignore */
            }
            case IRCMessage352 m -> handle(m);
            case IRCMessage353 m -> handle(m);
            case IRCMessage364 m -> {
                /* ignore */
            }
            case IRCMessage365 m -> {
                /* ignore */
            }
            case IRCMessage366 m -> {
                /* ignore */
            }
            case IRCMessage367 m -> {
                /* ignore */
            }
            case IRCMessage368 m -> {
                /* ignore */
            }
            case IRCMessage369 m -> {
                /* ignore */
            }
            case IRCMessage371 m -> {
                /* ignore */
            }
            case IRCMessage372 m -> terminal.println(makeSystemMessageOfTheDay(m.getText()));
            case IRCMessage374 m -> {
                /* ignore */
            }
            case IRCMessage375 m -> terminal.println(makeSystemMessageOfTheDay(m.getText()));
            case IRCMessage376 m -> {
                /* ignore */
            }
            case IRCMessage378 m -> {
                /* ignore */
            }
            case IRCMessage379 m -> {
                /* ignore */
            }
            case IRCMessage381 m -> handle(m);
            case IRCMessage382 m -> {
                /* ignore */
            }
            case IRCMessage391 m -> {
                /* ignore */
            }
            case IRCMessage400 m -> {
                /* ignore */
            }
            case IRCMessage401 m -> terminal.println(makeSystemErrorMessage(m.getText()));
            case IRCMessage402 m -> {
                /* ignore */
            }
            case IRCMessage403 m -> {
                /* ignore */
            }
            case IRCMessage404 m -> {
                /* ignore */
            }
            case IRCMessage405 m -> {
                /* ignore */
            }
            case IRCMessage406 m -> {
                /* ignore */
            }
            case IRCMessage409 m -> {
                /* ignore */
            }
            case IRCMessage411 m -> {
                /* ignore */
            }
            case IRCMessage412 m -> {
                /* ignore */
            }
            case IRCMessage417 m -> {
                /* ignore */
            }
            case IRCMessage421 m -> {
                /* ignore */
            }
            case IRCMessage422 m -> makeSystemMessageOfTheDay("There is no Message of the Day");
            case IRCMessage431 m -> {
                /* ignore */
            }
            case IRCMessage432 m -> {
                /* ignore */
            }
            case IRCMessage433 m -> {
                /* ignore */
            }
            case IRCMessage436 m -> {
                /* ignore */
            }
            case IRCMessage441 m -> {
                /* ignore */
            }
            case IRCMessage442 m -> handle(m);
            case IRCMessage443 m -> {
                /* ignore */
            }
            case IRCMessage451 m -> {
                /* ignore */
            }
            case IRCMessage461 m -> {
                /* ignore */
            }
            case IRCMessage462 m -> {
                /* ignore */
            }
            case IRCMessage464 m -> terminal.println(makeSystemErrorMessage("Command failed -- Invalid password"));
            case IRCMessage465 m -> {
                /* ignore */
            }
            case IRCMessage471 m -> {
                /* ignore */
            }
            case IRCMessage472 m -> {
                /* ignore */
            }
            case IRCMessage473 m -> {
                /* ignore */
            }
            case IRCMessage474 m -> {
                /* ignore */
            }
            case IRCMessage475 m -> {
                /* ignore */
            }
            case IRCMessage476 m -> {
                /* ignore */
            }
            case IRCMessage481 m -> {
                /* ignore */
            }
            case IRCMessage482 m -> {
                /* ignore */
            }
            case IRCMessage483 m -> {
                /* ignore */
            }
            case IRCMessage491 m -> terminal.println(makeSystemErrorMessage(m.getText()));
            case IRCMessage501 m -> {
                /* ignore */
            }
            case IRCMessage502 m -> {
                /* ignore */
            }
            case IRCMessage524 m -> {
                /* ignore */
            }
            case IRCMessage525 m -> {
                /* ignore */
            }
            case IRCMessage670 m -> {
                /* ignore */
            }
            case IRCMessage671 m -> {
                /* ignore */
            }
            case IRCMessage691 m -> {
                /* ignore */
            }
            case IRCMessage696 m -> {
                /* ignore */
            }
            case IRCMessage704 m -> {
                /* ignore */
            }
            case IRCMessage705 m -> {
                /* ignore */
            }
            case IRCMessage706 m -> {
                /* ignore */
            }
            case IRCMessage723 m -> {
                /* ignore */
            }
            case IRCMessageUnsupported m -> terminal.println(makeSystemErrorMessage("» " + m.getRawMessage()));
            case IRCMessageParseError m ->
                terminal.println(makeSystemErrorMessage("(PARSE ERROR) » " + m.getRawMessage()));
        }
        updateStatusAndPrompt();
    }

    private void handle(IRCMessageAWAY message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        state.setAway(message.getPrefixName(), message.getText());
    }

    private void handle(IRCMessageCAPLSResponse message) {
        IRCClientState state = clientStateGuard.getState();
        IRCClientEngineState engineState = this.engineState.get();
        if (state == null
                || (engineState != IRCClientEngineState.CONNECTED && engineState != IRCClientEngineState.REGISTERED)) {
            LOG.warning("Unexpected CAP LS message in state " + engineState);
            return;
        }

        IRCClientState.Capabilities capabilities = state.getCapabilities();

        if (!capabilities.isReceivingCapabilities()) {
            capabilities.clearServerCapabilities();
            capabilities.startReceivingCapabilities();
        }

        for (Map.Entry<String, String> capability : message.getCapabilities().sequencedEntrySet()) {
            LOG.info("Server advertised capability: " + capability);
            IRCCapability.forName(capability.getKey())
                    .ifPresent(c -> capabilities.addServerCapability(c, capability.getValue()));
        }

        if (!message.isHasMore()) {
            capabilities.stopReceivingCapabilities();
            if (capabilities.getServerCapabilities().isEmpty() && engineState == IRCClientEngineState.CONNECTED) {
                send(new IRCMessageCAPEND());
            } else {
                Set<IRCCapability> requestedCapabilities = capabilities.getServerCapabilities();
                requestedCapabilities.forEach(capabilities::addRequestedCapability);
                send(new IRCMessageCAPREQ(
                        requestedCapabilities.stream()
                                .map(IRCCapability::getCapabilityName)
                                .toList(),
                        List.of()));
            }
        }
    }

    private void handle(IRCMessageCAPACK message) {
        IRCClientState state = clientStateGuard.getState();
        IRCClientEngineState engineState = this.engineState.get();
        if (state == null
                || (engineState != IRCClientEngineState.CONNECTED && engineState != IRCClientEngineState.REGISTERED)) {
            LOG.warning("Unexpected CAP ACK message in state " + engineState);
            return;
        }

        IRCClientState.Capabilities capabilities = state.getCapabilities();

        for (String capabilityString : message.getEnableCapabilities()) {
            IRCCapability.forName(capabilityString).ifPresent(cap -> {
                capabilities.removeRequestedCapability(cap);
                capabilities.enableCapability(cap);
            });
        }

        LOG.info(() -> "Server acknowledged capabilities: " + message.getEnableCapabilities());

        if (engineState == IRCClientEngineState.CONNECTED
                && capabilities.getRequestedCapabilities().isEmpty()) {
            send(new IRCMessageCAPEND());
        }
    }

    private void handle(IRCMessageCAPDEL message) {
        IRCClientState state = clientStateGuard.getState();
        IRCClientEngineState engineState = this.engineState.get();
        if (state == null || engineState != IRCClientEngineState.REGISTERED) {
            LOG.warning("Unexpected CAP DEL message in state " + engineState);
            return;
        }

        IRCClientState.Capabilities capabilities = state.getCapabilities();

        for (String capabilityString : message.getCapabilities()) {
            IRCCapability.forName(capabilityString).ifPresent(capabilities::removeServerCapability);
        }

        LOG.info(() -> "Server deleted capabilities: " + message.getCapabilities());
    }

    private void handle(IRCMessageCAPLISTResponse message) {
        IRCClientState state = clientStateGuard.getState();
        IRCClientEngineState engineState = this.engineState.get();
        if (state == null || engineState != IRCClientEngineState.REGISTERED) {
            LOG.warning("Unexpected CAP LIST message in state " + engineState);
            return;
        }

        IRCClientState.Capabilities capabilities = state.getCapabilities();

        if (!capabilities.isReceivingCapabilities()) {
            capabilities.clearActiveCapabilities();
            capabilities.startReceivingCapabilities();
        }

        for (String capabilityString : message.getCapabilities()) {
            IRCCapability.forName(capabilityString).ifPresent(capabilities::enableCapability);
        }

        LOG.info(() -> "Server listed active capabilities: " + message.getCapabilities());

        if (!message.isHasMore()) {
            capabilities.stopReceivingCapabilities();
        }
    }

    private void handle(IRCMessageCAPNAK message) {
        IRCClientState state = clientStateGuard.getState();
        IRCClientEngineState engineState = this.engineState.get();
        if (state == null
                || (engineState != IRCClientEngineState.CONNECTED && engineState != IRCClientEngineState.REGISTERED)) {
            LOG.warning("Unexpected CAP NAK message in state " + engineState);
            return;
        }

        IRCClientState.Capabilities capabilities = state.getCapabilities();

        for (String capabilityString : message.getEnableCapabilities()) {
            IRCCapability.forName(capabilityString).ifPresent(capabilities::removeRequestedCapability);
        }

        LOG.info(() -> "Server rejected capabilities: " + message.getEnableCapabilities());

        if (engineState == IRCClientEngineState.CONNECTED
                && capabilities.getRequestedCapabilities().isEmpty()) {
            send(new IRCMessageCAPEND());
        }
    }

    private void handle(IRCMessageCAPNEW message) {
        IRCClientState state = clientStateGuard.getState();
        IRCClientEngineState engineState = this.engineState.get();
        if (state == null || engineState != IRCClientEngineState.REGISTERED) {
            LOG.warning("Unexpected CAP NEW message in state " + engineState);
            return;
        }

        IRCClientState.Capabilities capabilities = state.getCapabilities();
        Set<IRCCapability> newCapabilities = new HashSet<>();
        for (Map.Entry<String, String> capability : message.getCapabilities().sequencedEntrySet()) {
            IRCCapability.forName(capability.getKey()).ifPresent(cap -> {
                capabilities.addServerCapability(cap, capability.getValue());
                newCapabilities.add(cap);
            });
        }

        if (!newCapabilities.stream().allMatch(capabilities::isActive)) {
            newCapabilities.forEach(capabilities::addRequestedCapability);
            send(new IRCMessageCAPREQ(
                    newCapabilities.stream()
                            .map(IRCCapability::getCapabilityName)
                            .toList(),
                    List.of()));
        }
    }

    private void handle(IRCMessageERROR message) {
        terminal.println(makeSystemErrorMessage(message.getReason()));
    }

    private void handle(IRCMessageJOINNormal message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        for (String channelName : message.getChannels()) {
            state.addChannelMember(channelName, message.getPrefixName());
            terminal.println(new TerminalMessage(
                    getMessageTime(message),
                    f(message.getPrefixName()),
                    f(channelName),
                    s(
                            f(message.getPrefixName()),
                            f(Color.GREEN, " joined channel "),
                            f(channelName),
                            f(Color.GREEN, "!"))));
        }
    }

    private void handle(IRCMessageKICK message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        state.deleteChannelMember(message.getChannel(), message.getNick());
        terminal.println(new TerminalMessage(
                getMessageTime(message),
                f(message.getPrefixName()),
                f(message.getChannel()),
                s(
                        f(message.getPrefixName()),
                        f(Color.RED, " kicked "),
                        f(message.getNick()),
                        f(Color.RED, " from "),
                        f(message.getChannel()),
                        f(Color.RED, "!"),
                        s(
                                message.getReason() != null
                                                && !message.getReason().isBlank()
                                        ? f(Color.GRAY, s(" (", message.getReason(), ")"))
                                        : ""))));
    }

    // IRC specification for MODE is genuinely unhinged
    private void handle(IRCMessageMODE mode) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        IRCServerParameters parameters = state.getParameters();

        Boolean adding = null; // we don't know yet
        Iterator<String> argumentIterator = mode.getModeArguments().iterator();
        for (char c : mode.getModeString().toCharArray()) {
            switch (c) {
                case '+' -> adding = true;
                case '-' -> adding = false;
                default -> {
                    if (adding == null) {
                        LOG.warning("Received malformed MODE command: " + mode.getRawMessage());
                        return;
                    }

                    if (isChannel(state, mode.getTarget())) { // channel modes
                        if (parameters.getPrefixes().containsKey(c)) { // user prefix mode
                            if (argumentIterator.hasNext()) {
                                String nick = argumentIterator.next();
                                if (adding) {
                                    state.addChannelMemberModes(mode.getTarget(), nick, c);
                                } else {
                                    state.deleteChannelMemberModes(mode.getTarget(), nick, c);
                                }
                            } else {
                                LOG.warning("Mode '" + c + "' missing required argument: " + mode.getRawMessage());
                            }
                        } else if (parameters.getTypeAChannelModes().contains(c)) { // list values (e.g. bans)
                            if (argumentIterator.hasNext()) {
                                String value = argumentIterator.next();
                                if (adding) {
                                    state.addToChannelList(mode.getTarget(), c, value);
                                } else {
                                    state.removeFromChannelList(mode.getTarget(), c, value);
                                }
                            } else {
                                LOG.warning("Mode '" + c + "' missing required argument: " + mode.getRawMessage());
                            }
                        } else if (parameters
                                .getTypeBChannelModes()
                                .contains(c)) { // settings that always need value (e.g. password)
                            if (argumentIterator.hasNext()) {
                                String value = argumentIterator.next();
                                if (adding) {
                                    state.setChannelSetting(mode.getTarget(), c, value);
                                } else {
                                    state.removeChannelSetting(mode.getTarget(), c);
                                }
                            } else {
                                LOG.warning("Mode '" + c + "' missing required argument: " + mode.getRawMessage());
                                if (!adding) {
                                    state.removeChannelSetting(mode.getTarget(), c);
                                }
                            }
                        } else if (parameters
                                .getTypeCChannelModes()
                                .contains(c)) { // settings that only consume a value on set
                            if (adding) {
                                if (argumentIterator.hasNext()) {
                                    String value = argumentIterator.next();
                                    state.setChannelSetting(mode.getTarget(), c, value);
                                } else {
                                    LOG.warning("Mode '+" + c + "' missing required argument: " + mode.getRawMessage());
                                }
                            } else {
                                state.removeChannelSetting(mode.getTarget(), c);
                            }
                        } else if (parameters.getTypeDChannelModes().contains(c)) { // flags
                            if (adding) {
                                state.setChannelFlag(mode.getTarget(), c);
                            } else {
                                state.clearChannelFlag(mode.getTarget(), c);
                            }
                        } else {
                            LOG.warning("Received unknown mode '" + c + "': " + mode.getRawMessage());
                        }
                    } else { // user modes
                        if (adding) {
                            state.setUserFlag(mode.getTarget(), c);
                        } else {
                            state.clearUserFlag(mode.getTarget(), c);
                        }
                    }
                }
            }
        }
    }

    private void handle(IRCMessageNICK nick) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        state.changeNickname(nick.getPrefixName(), nick.getNick());
        terminal.println(new TerminalMessage(
                getMessageTime(nick),
                f(nick.getNick()),
                null,
                s(f(nick.getPrefixName()), " changed their nick to ", f(nick.getNick()))));
    }

    private void handle(IRCMessageNOTICE message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        LocalTime time = getMessageTime(message);
        for (String target : message.getTargets()) {
            terminal.println(new TerminalMessage(
                    time, f(message.getPrefixName()), f(target), B(f(new Color(204, 187, 68), message.getMessage()))));
        }

        state.touch(message.getPrefixName());
    }

    private void handle(IRCMessagePART message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        for (String channelName : message.getChannels()) {
            state.deleteChannelMember(channelName, message.getPrefixName());
            terminal.println(new TerminalMessage(
                    getMessageTime(message),
                    f(message.getPrefixName()),
                    f(channelName),
                    s(
                            f(message.getPrefixName()),
                            f(Color.RED, " left channel "),
                            f(channelName),
                            f(Color.RED, "!"),
                            s(
                                    message.getReason() != null
                                                    && !message.getReason().isBlank()
                                            ? f(Color.GRAY, s(" (", message.getReason(), ")"))
                                            : ""))));
        }
    }

    private void handle(IRCMessagePING ping) {
        send(new IRCMessagePONG(null, ping.getToken()));
    }

    private void handle(IRCMessagePRIVMSG message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        LocalTime time = getMessageTime(message);
        for (String target : message.getTargets()) {
            terminal.println(new TerminalMessage(time, f(message.getPrefixName()), f(target), s(message.getMessage())));
        }

        state.touch(message.getPrefixName());
    }

    private void handle(IRCMessageQUIT message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        state.quit(message.getPrefixName());
        terminal.println(new TerminalMessage(
                getMessageTime(message),
                f(message.getPrefixName()),
                null,
                s(
                        f(message.getPrefixName()),
                        f(
                                Color.GRAY,
                                s(
                                        " has left the server",
                                        message.getReason() != null
                                                        && !message.getReason().isBlank()
                                                ? " (%s)".formatted(message.getReason())
                                                : "")))));
    }

    private void handle(IRCMessageTOPIC message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        String oldTopic = state.getChannelTopic(message.getChannel());
        state.setChannelTopic(message.getChannel(), message.getTopic());
        state.setChannelTopicUpdater(
                message.getChannel(),
                message.getPrefixName(),
                getMessageInstant(message).toEpochMilli());
        terminal.println(makeSystemTerminalMessage(s(
                f(message.getPrefixName()),
                " changed the topic of ",
                f(message.getChannel()),
                oldTopic != null && !oldTopic.isBlank() ? s(" from \"", f(Color.DARK_GRAY, oldTopic), "\"") : "",
                " to \"",
                f(Color.ORANGE, message.getTopic()),
                "\"")));
    }

    private void handle(IRCMessage001 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null
                || !engineState.compareAndSet(IRCClientEngineState.CONNECTED, IRCClientEngineState.REGISTERED)) {
            terminal.println(makeSystemTerminalMessage("Fatal error during registration"));
            disconnect();
            return;
        }

        state.setMe(message.getClient());

        terminal.println(makeSystemTerminalMessage(s("You are now registered as ", f(message.getClient()), "!")));
    }

    private void handle(IRCMessage005 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        IRCServerParameters parameters = state.getParameters();

        for (Map.Entry<String, String> entry : message.getParameters().sequencedEntrySet()) {
            IRCServerParametersUnmarshaller.unmarshal(entry.getKey(), entry.getValue(), parameters);
        }
    }

    private void handle(IRCMessage301 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        state.setAway(message.getPrefixName(), message.getAwayMessage());
        terminal.println(makeSystemTerminalMessage(
                f(Color.GRAY, s("* ", message.getPrefixName(), " is away: ", message.getAwayMessage()))));
    }

    private void handle(IRCMessage302 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        for (String reply : message.getUserhosts()) {
            String[] parts = reply.split("=", 2);
            if (parts.length != 2) {
                LOG.warning("Malformed USERHOST reply: " + reply);
                continue;
            }
            String nickname = parts[0];
            boolean operator = nickname.endsWith("*");
            if (operator) {
                nickname = nickname.substring(0, nickname.length() - 1);
            }
            boolean away = parts[1].startsWith("-");

            state.setOperator(nickname, operator);
            state.setAway(
                    nickname,
                    away ? Objects.requireNonNullElse(state.getAwayStatus(nickname), "Marked AWAY by server") : null);
        }
    }

    private void handle(IRCMessage305 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        state.setAway(state.getMe(), null);
    }

    private void handle(IRCMessage306 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        state.setAway(
                state.getMe(), Objects.requireNonNullElse(state.getAwayStatus(state.getMe()), "Marked AWAY by server"));
    }

    private void handle(IRCMessage331 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        state.setChannelTopic(message.getChannel(), null);
        terminal.println(makeSystemTerminalMessage(s(f(message.getChannel()), " does not have a topic")));
    }

    private void handle(IRCMessage332 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        state.setChannelTopic(message.getChannel(), message.getTopic());
        terminal.println(makeSystemTerminalMessage(
                s("Topic for ", f(message.getChannel()), " is \"", message.getTopic(), "\"")));
    }

    private void handle(IRCMessage333 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        state.setChannelTopicUpdater(message.getChannel(), message.getSetBy(), message.getSetAt());
        terminal.println(makeSystemTerminalMessage(s(
                "Topic set by ",
                f(message.getSetBy()),
                " on ",
                B(FRIENDLY_DATE_FORMAT.format(Instant.ofEpochMilli(message.getSetAt()))))));
    }

    private void handle(IRCMessage352 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        String flags = message.getFlags();
        if (flags.length() >= 2) {
            state.setOperator(message.getNick(), flags.charAt(1) == '*');
        }
        if (!flags.isEmpty()) {
            boolean away = flags.charAt(0) == 'G';
            state.setAway(
                    message.getNick(),
                    away
                            ? Objects.requireNonNullElse(
                                    state.getAwayStatus(message.getNick()), "Marked AWAY by server")
                            : null);
        }
    }

    private void handle(IRCMessage353 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        List<String> nicks = message.getNicks();
        List<Character> modes = message.getModes();
        String channel = message.getChannel();

        Map<Character, Character> prefixToMode = new HashMap<>();
        for (Map.Entry<Character, Character> e :
                state.getParameters().getPrefixes().entrySet()) {
            prefixToMode.put(e.getValue(), e.getKey());
        }

        for (int i = 0; i < nicks.size(); i++) {
            String nick = nicks.get(i);
            Character prefix = i < modes.size() ? modes.get(i) : null;
            Character mode = prefix != null ? prefixToMode.get(prefix) : null;
            if (mode != null) {
                state.addChannelMember(channel, nick, mode);
            } else {
                state.addChannelMember(channel, nick);
            }
        }
    }

    private void handle(IRCMessage381 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        terminal.println(makeSystemTerminalMessage(f(Color.GREEN, B("You are now an operator"))));
        state.setOperator(state.getMe(), true);
    }

    private void handle(IRCMessage442 message) {
        terminal.println(makeSystemTerminalMessage(
                f(Color.RED, s("Could not complete command, you must first join ", f(message.getChannel()), "!"))));
    }

    private void handle(ClientCommand command) {
        switch (command) {
            case ClientCommandAfk c -> handle(c);
            case ClientCommandBack c -> handle(c);
            case ClientCommandConnect c -> handle(c);
            case ClientCommandExit c -> handle(c);
            case ClientCommandHelp c -> {
                /* handled externally */
            }
            case ClientCommandJoin c -> handle(c);
            case ClientCommandKick c -> handle(c);
            case ClientCommandKill c -> handle(c);
            case ClientCommandMode c -> handle(c);
            case ClientCommandMsg c -> handle(c);
            case ClientCommandNotice c -> handle(c);
            case ClientCommandMsgCurrent c -> handle(c);
            case ClientCommandNick c -> handle(c);
            case ClientCommandOper c -> handle(c);
            case ClientCommandPart c -> handle(c);
            case ClientCommandQuit c -> handle(c);
            case ClientCommandTopic c -> handle(c);
        }
        updateStatusAndPrompt();
    }

    private void handle(ClientCommandAfk command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        String message = Objects.requireNonNullElse(command.getText(), "be right back");
        state.setAway(state.getMe(), message);
        send(new IRCMessageAWAY(message));
    }

    private void handle(ClientCommandBack command) {
        send(new IRCMessageAWAY(null));
    }

    private void handle(ClientCommandConnect command) {
        connect();
    }

    private void handle(ClientCommandExit command) {
        close();
    }

    private void handle(ClientCommandJoin command) {
        send(new IRCMessageJOINNormal(command.getChannels(), command.getKeys()));
    }

    private void handle(ClientCommandKick command) {
        send(new IRCMessageKICK(command.getChannel(), command.getNick(), command.getReason()));
    }

    private void handle(ClientCommandKill command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            terminal.println(makeSystemErrorMessage("Could not kill -- connection not yet registered"));
            return;
        }

        if (!state.isOperator(state.getMe())) {
            terminal.println(makeSystemErrorMessage("You are not an operator (see /oper)"));
            return;
        }

        send(new IRCMessageKILL(command.getNick(), command.getReason()));
    }

    private void handle(ClientCommandMode command) {
        send(new IRCMessageMODE(command.getTarget(), command.getModeString(), command.getModeArguments()));
    }

    private void handle(ClientCommandMsg command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            terminal.println(makeSystemErrorMessage("Could not send message -- connection not yet registered"));
            return;
        }

        send(new IRCMessagePRIVMSG(command.getTargets(), command.getText()));

        if (!state.getCapabilities().isActive(IRCCapability.ECHO_MESSAGE)) {
            for (String target : command.getTargets()) {
                terminal.println(
                        new TerminalMessage(LocalTime.now(), f(state.getMe()), f(target), s(command.getText())));
            }
        }
    }

    private void handle(ClientCommandMsgCurrent command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            terminal.println(makeSystemErrorMessage("Could not send message -- connection not yet registered"));
            return;
        }

        IRCClientState.Channel channel = state.getFocusedChannel().orElse(null);
        if (channel == null) {
            terminal.println(makeSystemErrorMessage("Failed to send message -- no channel focused"));
            return;
        }

        send(new IRCMessagePRIVMSG(List.of(channel.getName()), command.getText()));

        if (!state.getCapabilities().isActive(IRCCapability.ECHO_MESSAGE)) {
            terminal.println(
                    new TerminalMessage(LocalTime.now(), f(state.getMe()), f(channel.getName()), s(command.getText())));
        }
    }

    private void handle(ClientCommandNick command) {
        send(new IRCMessageNICK(command.getNick()));
    }

    private void handle(ClientCommandOper command) {
        send(new IRCMessageOPER(command.getName(), command.getPassword()));
    }

    private void handle(ClientCommandNotice command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            terminal.println(makeSystemErrorMessage("Could not send message -- connection not yet registered"));
            return;
        }

        send(new IRCMessageNOTICE(command.getTargets(), command.getText()));
    }

    private void handle(ClientCommandPart command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            terminal.println(makeSystemErrorMessage("Could not part channel -- connection not yet registered"));
            return;
        }

        send(new IRCMessagePART(command.getChannels(), command.getReason()));
    }

    private void handle(ClientCommandQuit command) {
        send(new IRCMessageQUIT(command.getReason()));
    }

    private void handle(ClientCommandTopic command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            terminal.println(makeSystemErrorMessage("Could not set topic -- connection not yet registered"));
            return;
        }

        String channel = command.getChannel();
        if (command.getChannel() == null) {
            channel = state.getFocusedChannel()
                    .map(IRCClientState.Channel::getName)
                    .orElse(null);
        }

        if (channel != null) {
            send(new IRCMessageTOPIC(channel, command.getTopic()));
        } else {
            terminal.println(makeSystemErrorMessage("Failed to set topic -- no channel specified"));
        }
    }

    private boolean isChannel(IRCClientState state, String target) {
        IRCServerParameters parameters = state.getParameters();
        return parameters.getChannelTypes().contains(target.charAt(0));
    }

    private Instant getMessageInstant(IRCMessage message) {
        String serverTime = message.getTags().get("time");
        if (serverTime != null) {
            try {
                return Instant.parse(serverTime);
            } catch (Exception e) {
                return Instant.now();
            }
        } else {
            return Instant.now();
        }
    }

    private LocalTime getMessageTime(IRCMessage message) {
        String serverTime = message.getTags().get("time");
        if (serverTime != null) {
            try {
                return Instant.parse(serverTime).atZone(ZoneId.systemDefault()).toLocalTime();
            } catch (Exception e) {
                return LocalTime.now();
            }
        } else {
            return LocalTime.now();
        }
    }

    private TerminalMessage makeSystemTerminalMessage(String message) {
        return new TerminalMessage(LocalTime.now(), f(Color.YELLOW, "SYSTEM"), null, f(Color.GRAY, message));
    }

    private TerminalMessage makeSystemTerminalMessage(RichString message) {
        return new TerminalMessage(LocalTime.now(), f(Color.YELLOW, "SYSTEM"), null, f(Color.GRAY, message));
    }

    private TerminalMessage makeSystemMessageOfTheDay(String message) {
        return new TerminalMessage(LocalTime.now(), f(Color.YELLOW, "SYSTEM"), null, f(Color.ORANGE, message));
    }

    private TerminalMessage makeSystemErrorMessage(String message) {
        return new TerminalMessage(LocalTime.now(), f(Color.YELLOW, "SYSTEM"), null, f(Color.RED, message));
    }

    private TerminalMessage makeSystemErrorMessage(RichString message) {
        return new TerminalMessage(LocalTime.now(), f(Color.YELLOW, "SYSTEM"), null, f(Color.RED, message));
    }

    private void updateStatusAndPrompt() {
        IRCClientEngineState ies = engineState.get();
        RichString prompt =
                switch (ies) {
                    case NEW, INITIALIZING, DISCONNECTED, CONNECTING, CLOSED ->
                        s("[", f(properties.getNickname()), "]:");
                    case CONNECTED, REGISTERED ->
                        s(
                                "[",
                                f(properties.getNickname()),
                                "@",
                                f(properties.getHost().getHostName()),
                                "]:");
                };
        RichString status =
                switch (ies) {
                    case NEW, INITIALIZING -> s("Initializing client, please wait...");
                    case DISCONNECTED ->
                        s("Disconnected: Reconnect using `/connect` or view more options with `/help`");
                    case CONNECTING -> s("Establishing connection, please wait...");
                    case CONNECTED -> s("Registering client, please wait...");
                    case REGISTERED ->
                        s("Waiting to chat, join a channel using `/join <name>` or view more options with `/help`");
                    case CLOSED -> s("Shutting down...");
                };
        if (ies == IRCClientEngineState.REGISTERED) {
            IRCClientState state = clientStateGuard.getState();
            if (state != null) {
                RichString operPart = state.isOperator(state.getMe()) ? b(Color.RED, f(Color.WHITE, "[OPER]")) : s("");
                RichString mePart = state.isAway(state.getMe()) ? f(Color.GRAY, state.getMe()) : f(state.getMe());
                IRCClientState.Channel channel = state.getFocusedChannel().orElse(null);
                prompt = s(operPart, "[", mePart, "@", f(properties.getHost().getHostName()), "]:");
                if (channel != null) {
                    String topic = state.getChannelTopic(channel.getName());
                    RichString formattedTopic = topic == null ? s("(no topic)") : s("(", topic, ")");
                    RichString[] members = channel.getMemberships().entrySet().stream()
                            .filter(e -> !Objects.equals(e.getKey().getNickname(), state.getMe()))
                            .sorted(Comparator.comparing(e -> e.getKey().getNickname()))
                            .map(entry -> {
                                IRCClientState.User user = entry.getKey();
                                IRCClientState.Membership membership = entry.getValue();
                                RichString nickname = state.isAway(user.getNickname())
                                        ? f(Color.GRAY, user.getNickname())
                                        : f(user.getNickname());
                                return state.getParameters().getPrefixes().entrySet().stream()
                                        .filter(e -> membership.getModes().contains(e.getKey()))
                                        .findFirst()
                                        .map(e -> s(f(Color.YELLOW, e.getValue()), nickname))
                                        .orElse(nickname);
                            })
                            .toArray(RichString[]::new);
                    if (members.length > 0) {
                        status = s(
                                "Chatting in ", f(channel.getName()), " with ", j(", ", members), " ", formattedTopic);
                    } else {
                        status = s("Chatting in ", f(channel.getName()), " all alone ", formattedTopic);
                    }
                    prompt = s(
                            operPart,
                            "[",
                            mePart,
                            "@",
                            f(properties.getHost().getHostName()),
                            "/",
                            f(channel.getName()),
                            "]:");
                }
            }
        }
        terminal.setStatus(status);
        terminal.setPrompt(prompt);
    }

    public void start() {
        if (!engineState.compareAndSet(IRCClientEngineState.NEW, IRCClientEngineState.INITIALIZING)) {
            throw new IllegalStateException("IRCClientEngine has already been started");
        }

        try {
            if (!engineState.compareAndSet(IRCClientEngineState.INITIALIZING, IRCClientEngineState.DISCONNECTED)) {
                throw new IllegalStateException("inconsistent state in initialization");
            }

            LOG.info("IRCClientEngine started");

            // automatically connect
            accept(new ClientCommandConnect());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "failed to initialize IRCClientEngine", e);
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        IRCClientEngineState oldState = engineState.getAndSet(IRCClientEngineState.CLOSED);
        if (oldState == IRCClientEngineState.CLOSED) {
            return;
        }

        LOG.info("IRCClientEngine shutting down...");

        IRCConnection connection = connectionHolder.getAndSet(null);
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception unused) {
                // do nothing
            }
        }

        executor.shutdownNow();
        terminal.stop();
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

    private enum IRCClientEngineState {
        NEW,
        INITIALIZING,
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        REGISTERED,
        CLOSED
    }
}
