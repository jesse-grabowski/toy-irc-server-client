import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCClientEngine {

    private static final String SYSTEM_SENDER = "SYSTEM";
    private static final String SERVER_SENDER = "SERVER";

    private final BlockingQueue<ClientCommand> commands = new LinkedBlockingQueue<>();
    private final IRCClientProperties properties;
    private final TerminalUI terminal;

    private volatile boolean running = false;
    private volatile IRCClientState state = IRCClientState.DISCONNECTED;
    private volatile String channel = null;

    private Thread workerThread; // handles outgoing commands
    private Thread readerThread; // handles incoming server messages

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public IRCClientEngine(IRCClientProperties properties, TerminalUI terminal) {
        this.properties = properties;
        this.terminal = terminal;
    }

    public boolean send(ClientCommand message) {
        return commands.offer(message);
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        // Connect to IRC server
        socket = new Socket(properties.getHost(), properties.getPort());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        state = IRCClientState.CONNECTING;
        running = true;

        // Outgoing command processing thread
        workerThread = new Thread(this::handleClientCommand, "IRCClientEngine-Egress");
        workerThread.start();

        // Incoming server listener thread
        readerThread = new Thread(this::listenForServerMessages, "IRCClientEngine-Ingress");
        readerThread.start();
    }

    /** Stop engine, threads, and socket */
    public synchronized void stop() {
        running = false;

        try {
            if (workerThread != null) workerThread.interrupt();
            if (readerThread != null) readerThread.interrupt();
        } catch (Exception ignored) {}

        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        terminal.println(new TerminalMessage(LocalTime.now(), SYSTEM_SENDER, "IRCClientEngine stopped"));
    }

    /** Background thread: processes outgoing commands */
    public void handleClientCommand() {
        while (running) {
            try {
                switch (state) {
                    case DISCONNECTED: break; // do nothing
                    case CONNECTING:
                        sendNicknameCommand();
                        sendUserCommand();
                        this.state = IRCClientState.REGISTERING;
                        break;
                    case REGISTERING:
                        Thread.sleep(100);
                        break;
                    case REGISTERED:
                        ClientCommand command = commands.poll(100, TimeUnit.MILLISECONDS);
                        if (command != null) {
                            handleCommand(command);
                        }
                        break;
                }
            } catch (InterruptedException e) {
                if (!running) break;
            }
        }
    }

    /** Background thread: reads messages from server */
    private void listenForServerMessages() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                IRCMessage message = new IRCMessageUnmarshaller().unmarshal(line);
                handleServerMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                terminal.println(new TerminalMessage(LocalTime.now(), SYSTEM_SENDER, "Error reading from server: " + e.getMessage()));
            }
        } finally {
            stop();
        }
    }

    private void handleServerMessage(IRCMessage message) {
        switch (message) {
            case IRCMessage001 m -> {
                terminal.setPrompt("[%s@%s]: ".formatted(properties.getNickname(), properties.getHost().getHostName()));
                terminal.println(new TerminalMessage(LocalTime.now(), SERVER_SENDER, m.getMessage()));
                this.state = IRCClientState.REGISTERED;
            }
            case IRCMessagePING m -> sendPong(m.getToken());
            case IRCMessagePRIVMSG m -> printPrivateMessage(m);
            default -> terminal.println(new TerminalMessage(LocalTime.now(), SERVER_SENDER, message.getRawMessage()));
        }
    }

    private void handleCommand(ClientCommand message) {
        switch (message.getCommand()) {
            case "JOIN" -> sendJoin(message.getParams().getFirst());
            case "PRIVMSG" -> sendPrivateMessage(message.getParams().getFirst(), message.getParams().getLast());
            case "EXIT" -> exit();
            default -> {}
        }
    }

    private void sendNicknameCommand() {
        IRCMessageNICK message = new IRCMessageNICK(null, new LinkedHashMap<>(), null, null, null, properties.getNickname());
        sendLine(message);
        terminal.println(new TerminalMessage(LocalTime.now(), SYSTEM_SENDER, "Sending nickname command"));
    }

    private void sendUserCommand() {
        IRCMessageUSER message = new IRCMessageUSER(null, new LinkedHashMap<>(), null, null, null, properties.getNickname(), properties.getRealName());
        sendLine(message);
        terminal.println(new TerminalMessage(LocalTime.now(), SYSTEM_SENDER, "Sending user command"));
    }

    private void sendPong(String value) {
        IRCMessagePONG message = new IRCMessagePONG(null, new LinkedHashMap<>(), null, null, null, null, value);
        sendLine(message);
    }

    private void sendJoin(String channel) {
        IRCMessageJOINNormal message = new IRCMessageJOINNormal(null, new LinkedHashMap<>(), null, null, null, List.of(channel), null);
        sendLine(message);
    }

    private void sendPrivateMessage(String channel, String text) {
        IRCMessagePRIVMSG message = new IRCMessagePRIVMSG(null, new LinkedHashMap<>(), null, null, null, List.of(channel), text);
        sendLine(message);
        printMessage(properties.getNickname(), channel, text);
    }

    private void sendLine(IRCMessage message) {
        out.printf("%s\r\n", new IRCMessageMarshaller().marshal(message));
    }

    private void printPrivateMessage(IRCMessagePRIVMSG message) {
        printMessage(message.getPrefixName(), channel, message.getMessage());
    }

    private void printMessage(String nick, String channel, String text) {
        terminal.println(new TerminalMessage(LocalTime.now(), nick, text));
    }

    private void exit() {
        this.stop();
        terminal.stop();
    }
}
