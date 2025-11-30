import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCClientEngine {

    private final BlockingQueue<IRCClientCommand> commands = new LinkedBlockingQueue<>();
    private final IRCClientProperties properties;
    private final TUI terminal;

    private volatile boolean running = false;
    private volatile IRCClientState state = IRCClientState.DISCONNECTED;
    private volatile String channel = null;

    private Thread workerThread; // handles outgoing commands
    private Thread readerThread; // handles incoming server messages

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public IRCClientEngine(IRCClientProperties properties, TUI terminal) {
        this.properties = properties;
        this.terminal = terminal;
    }

    public boolean send(IRCClientCommand message) {
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
        workerThread = new Thread(this::handleClientCommand, "IRCClientEngine-Commands");
        workerThread.start();

        // Incoming server listener thread
        readerThread = new Thread(this::listenForServerMessages, "IRCClientEngine-ServerReader");
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

        terminal.println("IRCClientEngine stopped");
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
                        IRCClientCommand command = commands.poll(100, TimeUnit.MILLISECONDS);
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
                IRCMessage message = IRCMessageParser.parse(line);
                handleServerMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                terminal.println("Error reading from server: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    private void handleServerMessage(IRCMessage message) {
        switch (message.getCommand()) {
            case "001" -> {
                terminal.setPrompt("[%s%s%s@%s%s%s]: ".formatted(
                        Colorizer.colorize(properties.getNickname()), properties.getNickname(), Colorizer.SUFFIX,
                        Colorizer.colorize(properties.getHost().getHostName()), properties.getHost().getHostName(), Colorizer.SUFFIX
                ));
                terminal.println(message.getParams().getLast());
                this.state = IRCClientState.REGISTERED;
            }
            case "PING" -> sendPong(message.getParams().getFirst());
            case "PRIVMSG" -> printPrivateMessage(message);
            default -> terminal.println("[SERVER] %s %s %s".formatted(message.getPrefix(), message.getCommand(), message.getParams()));
        }
    }

    private void handleCommand(IRCClientCommand message) {
        switch (message.getCommand()) {
            case "JOIN" -> sendJoin(message.getParams().getFirst());
            case "PRIVMSG" -> sendPrivateMessage(message.getParams().getFirst(), message.getParams().getLast());
            default -> {}
        }
    }

    private void sendNicknameCommand() {
        out.printf("NICK %s\r\n", properties.getNickname());
        terminal.println("[ENGINE] Sending nickname command");
    }

    private void sendUserCommand() {
        out.printf("USER %s 0 * :%s\r\n", properties.getNickname(), properties.getRealName());
        terminal.println("[ENGINE] Sending user command");
    }

    private void sendPong(String value) {
        out.printf("PONG :%s\r\n", value);
    }

    private void sendJoin(String channel) {
        out.printf("JOIN :%s\r\n", channel);
    }

    private void sendPrivateMessage(String channel, String message) {
        out.printf("PRIVMSG %s :%s\r\n", channel, message);
        printMessage(properties.getNickname(), channel, message);
    }

    private void printPrivateMessage(IRCMessage message) {
        Pattern prefixPattern = Pattern.compile("^(?<nick>[a-zA-Z0-9]+)!(?<user>[a-zA-Z0-9~]+)@(?<host>[a-zA-Z0-9._-]+)$");
        Matcher matcher = prefixPattern.matcher(message.getPrefix());
        if (!matcher.matches()) {
            terminal.println("Error receiving private message");
        }
        String nick = matcher.group("nick");
        String user = matcher.group("user");
        String host = matcher.group("host");
        String channel = message.getParams().getFirst();
        printMessage(nick, channel, message.getParams().getLast());
    }

    private void printMessage(String nick, String channel, String text) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        terminal.println("%8s %s%10s%s -> %s%-10s%s \033[0;36m|\033[0m %s".formatted(
                time,
                Colorizer.colorize(nick), nick, Colorizer.SUFFIX,
                Colorizer.colorize(channel), channel, Colorizer.SUFFIX,
                text));
    }
}
