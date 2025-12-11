import java.awt.Color;
import java.io.Closeable;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IRCClientEngine implements Closeable {

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

        // using a single-threaded executor for engine tasks greatly simplifies our state management without
        // any real loss of performance (as the IRCConnection class handles additional threads for server
        // communication)
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1,
                Thread.ofPlatform().name("irc-client-executor") // name the thread so it shows up nicely in our logs
                        .daemon(false) // we don't want the JVM to terminate until this thread dies so daemon = false
                        .uncaughtExceptionHandler((t, e) -> LOG.log(Level.SEVERE, "Error executing task", e))
                        .factory(),
                // the more important bit is that we're overriding the default exception behavior, the logging
                // just makes it a bit easier for us to spot stray messages during shutdown (which are harmless)
                (task, exec) -> LOG.log(exec.isShutdown() ? Level.FINE : Level.SEVERE, "Task {0} rejected from {1}", new Object[] {task, exec}));
        executor.execute(spy(() -> {
            // bind the state guard to the current (executor) thread
            // any access from other threads will raise an exception
            clientStateGuard.bindToCurrentThread();
            // finally, start the garbage collection process to clean up
            // users that we no longer have visibility for. This is nested
            // to avoid a (very unlikely) state-guard-related race condition
            executor.scheduleWithFixedDelay(spy(() -> {
                IRCClientState state = clientStateGuard.getState();
                if (state != null) {
                    LOG.info("Garbage collecting IRC client state unused users / channels");
                    state.gc(System.currentTimeMillis() - Duration.ofMinutes(5).toMillis());
                }
            }), 0, 5, TimeUnit.MINUTES);
        }));
        this.executor = executor;
    }

    private void connect() {
        if (!engineState.compareAndSet(IRCClientEngineState.DISCONNECTED, IRCClientEngineState.CONNECTING)) {
            IRCClientEngineState current = engineState.get();
            LOG.warning("Cannot create connection from state " + current);
            if (current == IRCClientEngineState.CONNECTED || current == IRCClientEngineState.REGISTERED) {
                terminal.println(makeSystemTerminalMessage("You are already connected to the server and must `/quit` before reconnecting"));
            }
            return;
        }
        connectionHolder.set(null);

        IRCConnection connection = null;
        try {
            connection = IRCClientConnectionFactory.create(
                    properties.getHost(),
                    properties.getPort(),
                    Charset.forName(properties.getCharset()),
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
            send(new IRCMessageCAPLS(null, "302", false, new LinkedHashMap<>()));
            if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
                send(new IRCMessagePASS(properties.getPassword()));
            }
            send(new IRCMessageNICK(nick));
            send(new IRCMessageUSER(nick, properties.getRealName()));
            updateStatusAndPrompt();
        } catch (Exception e) {
            terminal.println(makeSystemTerminalMessage(
                    "Failed to connect to IRC server %s:%d, try again with /connect".formatted(
                            properties.getHost().getHostName(), properties.getPort())));
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
        receive(UNMARSHALLER.unmarshal(message));
    }

    private void receive(IRCMessage message) {
        if (engineState.get() != IRCClientEngineState.CLOSED) {
            executor.execute(spy(() -> handle(message)));
        }
    }

    // note that I'm using sealed classes + switch expressions
    // to force compile-time handling of all subtypes
    // traditionally this would have been done with the
    // visitor pattern but we're living in the future
    private void handle(IRCMessage message) {
        switch (message) {
            case IRCMessageCAPACK m -> handle(m);
            case IRCMessageCAPDEL m -> handle(m);
            case IRCMessageCAPEND m -> { /* ignore */ }
            case IRCMessageCAPLIST m -> handle(m);
            case IRCMessageCAPLS m -> handle(m);
            case IRCMessageCAPNAK m -> handle(m);
            case IRCMessageCAPNEW m -> handle(m);
            case IRCMessageCAPREQ m -> { /* ignore */ }
            case IRCMessageERROR m -> handle(m);
            case IRCMessageJOIN0 m -> { /* ignore */ }
            case IRCMessageJOINNormal m -> handle(m);
            case IRCMessageKICK m -> handle(m);
            case IRCMessageNICK m -> handle(m);
            case IRCMessagePART m -> handle(m);
            case IRCMessagePASS m -> { /* ignore */ }
            case IRCMessagePING m -> handle(m);
            case IRCMessagePONG m -> { /* ignore */ }
            case IRCMessagePRIVMSG m -> handle(m);
            case IRCMessageQUIT m -> handle(m);
            case IRCMessageUSER m -> { /* ignore */ }
            case IRCMessage001 m -> handle(m);
            case IRCMessage005 m -> handle(m);
            case IRCMessage353 m -> handle(m);
            case IRCMessageUnsupported m -> terminal.println(makeSystemTerminalMessage("» " + m.getRawMessage()));
            case IRCMessageParseError m -> terminal.println(makeSystemTerminalMessage("(PARSE ERROR) » " + m.getRawMessage()));
        }
        updateStatusAndPrompt();
    }

    private void handle(IRCMessageCAPLS message) {
        IRCClientState state = clientStateGuard.getState();
        IRCClientEngineState engineState = this.engineState.get();
        if (state == null || (engineState != IRCClientEngineState.CONNECTED && engineState != IRCClientEngineState.REGISTERED)) {
            LOG.warning("Unexpected CAP LS message in state " + engineState);
            return;
        }

        IRCClientState.Capabilities capabilities = state.getCapabilities();

        if (!capabilities.isReceivingCapabilities()) {
            capabilities.clearServerCapabilities();
            capabilities.startReceivingCapabilities();
        }

        for (Map.Entry<String, String> capability : message.getCapabilities().sequencedEntrySet()) {
            IRCCapability.forName(capability.getKey()).ifPresent(
                    c -> capabilities.addServerCapability(c, capability.getValue()));
        }

        if (!message.isHasMore()) {
            capabilities.stopReceivingCapabilities();
            if (capabilities.getServerCapabilities().isEmpty() && engineState == IRCClientEngineState.CONNECTED) {
                send(new IRCMessageCAPEND());
            } else {
                Set<IRCCapability> requestedCapabilities = capabilities.getServerCapabilities();
                requestedCapabilities.forEach(capabilities::addRequestedCapability);
                send(new IRCMessageCAPREQ(
                        requestedCapabilities.stream().map(IRCCapability::getName).toList(),
                        List.of()));
            }
        }
    }

    private void handle(IRCMessageCAPACK message) {
        IRCClientState state = clientStateGuard.getState();
        IRCClientEngineState engineState = this.engineState.get();
        if (state == null || (engineState != IRCClientEngineState.CONNECTED && engineState != IRCClientEngineState.REGISTERED)) {
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

        if (engineState == IRCClientEngineState.CONNECTED && capabilities.getRequestedCapabilities().isEmpty()) {
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

    private void handle(IRCMessageCAPLIST message) {
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
        if (state == null || (engineState != IRCClientEngineState.CONNECTED && engineState != IRCClientEngineState.REGISTERED)) {
            LOG.warning("Unexpected CAP NAK message in state " + engineState);
            return;
        }

        IRCClientState.Capabilities capabilities = state.getCapabilities();

        for (String capabilityString : message.getEnableCapabilities()) {
            IRCCapability.forName(capabilityString).ifPresent(capabilities::removeRequestedCapability);
        }

        LOG.info(() -> "Server rejected capabilities: " + message.getEnableCapabilities());

        if (engineState == IRCClientEngineState.CONNECTED && capabilities.getRequestedCapabilities().isEmpty()) {
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
                    newCapabilities.stream().map(IRCCapability::getName).toList(),
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
                    s(f(message.getPrefixName()), f(Color.GREEN, " joined channel "), f(channelName), f(Color.GREEN, "!"))));
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
                        s(message.getReason() != null && !message.getReason().isBlank()
                                ? f(Color.GRAY, s(" (", message.getReason(), ")"))
                                : ""))));
    }

    private void handle(IRCMessageNICK nick) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            return;
        }

        state.changeNickname(nick.getPrefixName(), nick.getNick());
        terminal.println(new TerminalMessage(getMessageTime(nick), f(nick.getNick()), null,
                s(f(nick.getPrefixName()), " changed their nick to ", f(nick.getNick()))));
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
                            s(message.getReason() != null && !message.getReason().isBlank()
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
                s(f(message.getPrefixName()), f(Color.GRAY, s(" has left the server",
                        message.getReason() != null && !message.getReason().isBlank()
                                ? " (%s)".formatted(message.getReason())
                                : "")))));
    }

    private void handle(IRCMessage001 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || !engineState.compareAndSet(IRCClientEngineState.CONNECTED, IRCClientEngineState.REGISTERED)) {
            terminal.println(makeSystemTerminalMessage("Fatal error during registration"));
            disconnect();
            return;
        }

        state.setMe(message.getClient());
    }

    private void handle(IRCMessage005 message) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null) {
            return;
        }

        IRCClientState.Parameters parameters = state.getParameters();

        for (Map.Entry<String, String> entry : message.getParameters().sequencedEntrySet()) {
            IRCParameterParser.parse(entry.getKey(), entry.getValue(), parameters);
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

        Map<Character,Character> prefixToMode = new HashMap<>();
        for (Map.Entry<Character,Character> e : state.getParameters().getPrefixes().entrySet()) {
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

    private void handle(ClientCommand command) {
        switch (command) {
            case ClientCommandConnect c -> handle(c);
            case ClientCommandExit c -> handle(c);
            case ClientCommandHelp c -> { /* handled externally */ }
            case ClientCommandJoin c -> handle(c);
            case ClientCommandKick c -> handle(c);
            case ClientCommandMsg c -> handle(c);
            case ClientCommandMsgCurrent c -> handle(c);
            case ClientCommandNick c -> handle(c);
            case ClientCommandPart c -> handle(c);
            case ClientCommandQuit c -> handle(c);
        }
        updateStatusAndPrompt();
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

    private void handle(ClientCommandMsg command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            terminal.println(makeSystemTerminalMessage("Could not send message -- connection not yet registered"));
            return;
        }

        send(new IRCMessagePRIVMSG(command.getTargets(), command.getText()));

        if (!state.getCapabilities().isActive(IRCCapability.ECHO_MESSAGE)) {
            for (String target : command.getTargets()) {
                terminal.println(new TerminalMessage(LocalTime.now(), f(state.getMe()), f(target), s(command.getText())));
            }
        }
    }

    private void handle(ClientCommandMsgCurrent command) {
        IRCClientState state = clientStateGuard.getState();
        if (state == null || engineState.get() != IRCClientEngineState.REGISTERED) {
            terminal.println(makeSystemTerminalMessage("Could not send message -- connection not yet registered"));
            return;
        }

        IRCClientState.Channel channel = state.getFocusedChannel().orElse(null);
        if (channel == null) {
            terminal.println(makeSystemTerminalMessage("Failed to send message -- no channel focused"));
            return;
        }

        send(new IRCMessagePRIVMSG(List.of(channel.getName()), command.getText()));

        if (!state.getCapabilities().isActive(IRCCapability.ECHO_MESSAGE)) {
            terminal.println(new TerminalMessage(LocalTime.now(), f(state.getMe()), f(channel.getName()), s(command.getText())));
        }
    }

    private void handle(ClientCommandNick command) {
        send(new IRCMessageNICK(command.getNick()));
    }

    private void handle(ClientCommandPart command) {
        send(new IRCMessagePART(command.getChannels(), command.getReason()));
    }

    private void handle(ClientCommandQuit command) {
        send(new IRCMessageQUIT(command.getReason()));
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

    private TerminalMessage makeSystemErrorMessage(String message) {
        return new TerminalMessage(LocalTime.now(), f(Color.YELLOW, "SYSTEM"), null, f(Color.RED, message));
    }

    private void updateStatusAndPrompt() {
        IRCClientEngineState ies = engineState.get();
        RichString prompt = switch (ies) {
            case NEW, INITIALIZING, DISCONNECTED, CONNECTING, CLOSED -> s("[", f(properties.getNickname()), "]:");
            case CONNECTED, REGISTERED -> s("[", f(properties.getNickname()), "@", f(properties.getHost().getHostName()), "]:");
        };
        RichString status = switch (ies) {
            case NEW, INITIALIZING -> s("Initializing client, please wait...");
            case DISCONNECTED -> s("Disconnected: Reconnect using `/connect` or view more options with `/help`");
            case CONNECTING -> s("Establishing connection, please wait...");
            case CONNECTED -> s("Registering client, please wait...");
            case REGISTERED -> s("Waiting to chat, join a channel using `/join <name>` or view more options with `/help`");
            case CLOSED -> s("Shutting down...");
        };
        if (ies == IRCClientEngineState.REGISTERED) {
            IRCClientState state = clientStateGuard.getState();
            if (state != null) {
                IRCClientState.Channel channel = state.getFocusedChannel().orElse(null);
                prompt = s("[", f(state.getMe()), "@", f(properties.getHost().getHostName()), "]:");
                if (channel != null) {
                    RichString[] members = channel.getMemberships().entrySet().stream()
                            .filter(e -> !Objects.equals(e.getKey().getNickname(), state.getMe()))
                            .sorted(Comparator.comparing(e -> e.getKey().getNickname()))
                            .map(entry -> {
                                IRCClientState.User user = entry.getKey();
                                IRCClientState.Membership membership = entry.getValue();
                                return state.getParameters().getPrefixes().entrySet().stream()
                                        .filter(e -> membership.getModes().contains(e.getKey()))
                                        .findFirst()
                                        .map(e -> s(f(Color.YELLOW, e.getValue()), f(user.getNickname())))
                                        .orElse(f(user.getNickname()));
                            })
                            .toArray(RichString[]::new);
                    if (members.length > 0) {
                        status = s("Chatting in ", f(channel.getName()), " with ", j(", ", members));
                    } else {
                        status = s("Chatting in ", f(channel.getName()), " all alone");
                    }
                    prompt = s("[", f(state.getMe()), "@", f(properties.getHost().getHostName()), "/", f(channel.getName()), "]:");
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

        IRCConnection connection = connectionHolder.getAndSet(null);
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception unused) {
                // do nothing
            }
        }

        executor.shutdownNow();
    }

    private static final class StateGuard<T> {

        private volatile Thread thread;

        private T state;

        public T getState() {
            assertEngineThread();
            return state;
        }

        public void setState(T state) {
            assertEngineThread();
            this.state = state;
        }

        public void bindToCurrentThread() {
            if (this.thread != null && this.thread != Thread.currentThread()) {
                throw new IllegalStateException("state has already been bound to a different thread");
            }
            this.thread = Thread.currentThread();
        }

        private void assertEngineThread() {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("state can only be accessed from the engine thread");
            }
        }
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

    // We can't import static from the default package so we reimplement these here
    // to use them as single-character functions
    private static RichString s(Object arg0, Object ... args) {
        return RichString.s(arg0, args);
    }

    private static RichString j(Object del, RichString ... args) {
        return RichString.j(del, args);
    }

    private static RichString f(Object arg0) {
        return RichString.f(arg0);
    }

    private static RichString f(Color color, Object arg0) {
        return RichString.f(color, arg0);
    }
}
