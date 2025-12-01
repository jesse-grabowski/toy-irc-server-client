import java.time.LocalTime;
import java.util.List;

public class ClientCommandParser {

    private final TerminalUI terminal;
    private final IRCClientEngine engine;

    public ClientCommandParser(TerminalUI terminal, IRCClientEngine engine) {
        this.terminal = terminal;
        this.engine = engine;
    }

    public void accept(String command) {
        ClientCommand c = parse(command);
        if (c != null) {
            engine.send(c);
        } else {
            terminal.println(new TerminalMessage(LocalTime.now(), "SYSTEM", "Failed to parse command: " + command));
        }
    }

    private ClientCommand parse(String command) {
        String[] parts = command.split("\\s+");
        return switch (parts[0]) {
            case "/join" -> {
                ClientCommand c = new ClientCommand();
                c.setCommand("JOIN");
                c.setParams(List.of(parts[1]));
                yield c;
            }
            case "/msg" -> {
                ClientCommand c = new ClientCommand();
                c.setCommand("PRIVMSG");
                c.setParams(List.of(parts[1], command.split("\\s+", 3)[2]));
                yield c;
            }
            case "/exit" -> {
                ClientCommand c = new ClientCommand();
                c.setCommand("EXIT");
                yield c;
            }
            default -> null;
        };
    }

}
