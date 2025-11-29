import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class IRCClientEngine {

    private final BlockingQueue<IRCClientCommand> commands = new LinkedBlockingQueue<>();
    private final IRCClientProperties properties;
    private final REPL terminal;

    private volatile boolean running = false;
    private volatile IRCClientState state = IRCClientState.CONNECTING;

    private Thread workerThread; // handles outgoing commands
    private Thread readerThread; // handles incoming server messages

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public IRCClientEngine(IRCClientProperties properties, REPL terminal) {
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

        terminal.println("Connected to IRC server %s/%d".formatted(properties.getHost(), properties.getPort()));
        state = IRCClientState.SENDING_NICKNAME;
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
                    case CONNECTING: break; // do nothing
                    case SENDING_NICKNAME:
                        sendNicknameCommand();
                        break;
                    case SENDING_USER:
                        sendUserCommand();
                        break;
                    case CHATTING:
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
                terminal.println("[SERVER] " + line);
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
            case "PING" -> sendPong(message.getParams().getFirst());
        }
    }

    private void handleCommand(IRCClientCommand msg) {
        // TODO: serialize msg into IRC protocol format and send via `out.println(...)`
        terminal.println("[ENGINE] Received command: " + msg);
    }

    private void sendNicknameCommand() {
        out.printf("NICK %s\r\n", properties.getNickname());
        terminal.println("[ENGINE] Sending nickname command");
        state = IRCClientState.SENDING_USER;
    }

    private void sendUserCommand() {
        out.printf("USER %s 0 * :%s\r\n", properties.getNickname(), properties.getRealName());
        terminal.println("[ENGINE] Sending user command");
        state = IRCClientState.CHATTING;
    }

    private void sendPong(String value) {
        out.printf("PONG :%s\r\n", value);
    }
}
