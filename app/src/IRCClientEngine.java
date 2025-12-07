import java.io.Closeable;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IRCClientEngine implements Closeable {

    private static final Logger LOG = Logger.getLogger(IRCClientEngine.class.getName());
    private static final IRCMessageUnmarshaller UNMARSHALLER = new IRCMessageUnmarshaller();
    private static final IRCMessageMarshaller MARSHALLER = new IRCMessageMarshaller();

    private final AtomicReference<IRCConnection> connectionHolder = new AtomicReference<>();
    private final AtomicReference<IRCClientEngineState> engineState = new AtomicReference<>(IRCClientEngineState.NEW);
    private final BlockingQueue<IRCClientEngineWork> workQueue = new PriorityBlockingQueue<>(11, Comparator.comparing(IRCClientEngineWork::priority));

    private final StateGuard<IRCClientState> clientStateGuard = new StateGuard<>();

    private final IRCClientProperties properties;
    private final TerminalUI terminal;
    private final Thread thread;

    public IRCClientEngine(IRCClientProperties properties, TerminalUI terminal) {
        this.properties = properties;
        this.terminal = terminal;
        this.thread = new Thread(this::run, "irc-client-engine");
    }

    private void connect() {
        if (!engineState.compareAndSet(IRCClientEngineState.DISCONNECTED, IRCClientEngineState.CONNECTING)) {
            IRCClientEngineState current = engineState.get();
            terminal.println(makeSystemTerminalMessage("Cannot create connection from state " + current));
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

            IRCClientState state = new IRCClientState();
            String nick = NicknameGenerator.generate(properties.getNickname());
            state.setNick(nick);
            clientStateGuard.setState(state);

            enqueueSend(new IRCMessageNICK(nick));
            enqueueSend(new IRCMessageUSER(nick, properties.getRealName()));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to connect to IRC server", e);
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception unused) {
                    // do nothing
                }
            }
            if (!engineState.compareAndSet(IRCClientEngineState.CONNECTING, IRCClientEngineState.DISCONNECTED)) {
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
        workQueue.add(new IRCClientEngineWork(() -> {
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
        }, Integer.MIN_VALUE));
    }

    public void accept(ClientCommand command) {
        workQueue.add(new IRCClientEngineWork(() -> handle(command), -1));
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

    private void enqueueSend(IRCMessage message) {
        Runnable work = () -> {
            if (!send(message)) {
                terminal.println(makeSystemTerminalMessage("Fatal error during send"));
            }
        };
        workQueue.add(new IRCClientEngineWork(work, 0));
    }

    private void receive(String message) {
        receive(UNMARSHALLER.unmarshal(message));
    }

    private void receive(IRCMessage message) {
        workQueue.add(new IRCClientEngineWork(() -> handle(message), 0));
    }

    private void handle(IRCMessage message) {
        switch (message) {
            case IRCMessagePING m -> handle(m);
            case IRCMessage001 m -> handle(m);
            default -> terminal.println(makeSystemTerminalMessage("» " + message.getRawMessage()));
        }
    }

    private void handle(IRCMessagePING ping) {
        send(new IRCMessagePONG(null, ping.getToken()));
    }

    private void handle(IRCMessage001 message) {
        if (!engineState.compareAndSet(IRCClientEngineState.CONNECTED, IRCClientEngineState.REGISTERED)) {
            terminal.println(makeSystemTerminalMessage("Fatal error during registration"));
            disconnect();
        }
    }

    private void handle(ClientCommand command) {
        switch (command) {
            case ClientCommandExit c -> handle(c);
            case ClientCommandJoin c -> handle(c);
            case ClientCommandMsg c -> handle(c);
            default -> terminal.println(makeSystemTerminalMessage("Unknown command: " + command));
        }
    }

    private void handle(ClientCommandExit command) {
        close();
    }

    private void handle(ClientCommandJoin command) {
        send(new IRCMessageJOINNormal(command.getChannels(), command.getKeys()));
    }

    private void handle(ClientCommandMsg command) {
        send(new IRCMessagePRIVMSG(command.getTargets(), command.getText()));
    }

    private TerminalMessage makeSystemTerminalMessage(String message) {
        return new TerminalMessage(LocalTime.now(), "SYSTEM", message);
    }

    private void run() {
        try {
            while (engineState.get() != IRCClientEngineState.CLOSED && !Thread.currentThread().isInterrupted()) {
                IRCClientEngineWork work = workQueue.take();
                try {
                    work.task().run();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "error running task", e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            close();
        }
    }

    public void start() {
        if (!engineState.compareAndSet(IRCClientEngineState.NEW, IRCClientEngineState.INITIALIZING)) {
            throw new IllegalStateException("IRCClientEngine has already been started");
        }

        try {
            thread.start();

            if (!engineState.compareAndSet(IRCClientEngineState.INITIALIZING, IRCClientEngineState.DISCONNECTED)) {
                throw new IllegalStateException("inconsistent state in initialization");
            }

            LOG.info("IRCClientEngine started");

            workQueue.add(new IRCClientEngineWork(this::connect));
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

        if (Thread.currentThread() != thread) {
            thread.interrupt();
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

    private record IRCClientEngineWork(Runnable task, int priority) {

        public IRCClientEngineWork(Runnable task) {
            this(task, 0);
        }
    }

    private class StateGuard<T> {
        private T state;

        public T getState() {
            assertEngineThread();
            return state;
        }

        public void setState(T state) {
            assertEngineThread();
            this.state = state;
        }

        private void assertEngineThread() {
            if (Thread.currentThread() != thread) {
                throw new IllegalStateException("state can only be accessed from the engine thread");
            }
        }
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
