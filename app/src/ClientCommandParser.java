import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.Collectors;

public class ClientCommandParser {

    private final ArgsTokenizer tokenizer = new ArgsTokenizer();

    private final SequencedMap<String, ArgsParser<? extends ClientCommand>> parsers;
    private final ArgsParser<? extends ClientCommand> defaultParser;
    private final List<String> sortedCommands;

    public ClientCommandParser() {
        defaultParser = ArgsParser.builder(ClientCommandMsgCurrent::new, false, "message the current channel")
                .addGreedyStringPositional(0, ClientCommandMsgCurrent::setText, "message text", true)
                .build();

        parsers = new LinkedHashMap<>();
        parsers.put("/connect", ArgsParser.builder(ClientCommandConnect::new, false, "reconnect to the server")
                .addUsageExample("/connect")
                .build());
        parsers.put("/exit", ArgsParser.builder(ClientCommandExit::new, false, "quit RitsIRC")
                .addUsageExample("/exit")
                .build());
        parsers.put("/help", ArgsParser.builder(ClientCommandHelp::new, false, "view information about commands")
                        .addUsageExample("/help [<command>]")
                        .addUsageExample("/help")
                        .addUsageExample("/help /msg")
                        .addUsageExample("/help msg")
                        .addGreedyStringPositional(0, ClientCommandHelp::setCommand, "command to look up", false)
                .build());
        parsers.put("/join", ArgsParser.builder(ClientCommandJoin::new, false, "join a channel")
                        .addUsageExample("/join [-n] <channel1>[,<channel2>...] [<key1[,key2...]]")
                        .addUsageExample("/join #channel")
                        .addUsageExample("/join #protectedchan,#channel key")
                        .addUsageExample("/join -n #channel")
                        .addBooleanFlag('n', "no-switch", ClientCommandJoin::setNoSwitch, "do not switch to new channel", false)
                        .addCommaSeparatedListPositional(0, ClientCommandJoin::setChannels, "channel name(s)", true)
                        .addCommaSeparatedListPositional(1, ClientCommandJoin::setKeys, "key to join channels (channels with a key must be the first in list)", false)
                .build());
        parsers.put("/msg", ArgsParser.builder(ClientCommandMsg::new, false, "send message to a nick or channel")
                        .addUsageExample("/msg <target>[,<target>...] <text>")
                        .addUsageExample("/msg #channel hello there")
                .addCommaSeparatedListPositional(0, ClientCommandMsg::setTargets, "nick or channel (may be mask, \"*\" = current channel)", true)
                        .addGreedyStringPositional(1, ClientCommandMsg::setText, "text to send", true)
                .build());
        parsers.put("/quit", ArgsParser.builder(ClientCommandQuit::new, false, "disconnect from the server")
                .addUsageExample("/quit [<reason>]")
                .addUsageExample("/quit")
                .addUsageExample("/quit jobs done")
                .addGreedyStringPositional(0, ClientCommandQuit::setReason, "reason for disconnection", false)
                .build());

        List<String> commands = new ArrayList<>(parsers.keySet());
        commands.sort((a, b) -> Integer.compare(b.length(), a.length()));
        this.sortedCommands = commands;
    }

    public ClientCommand parse(String command) throws ArgsParserHelpRequestedException {
        for (String prefix : sortedCommands) {
            if (command.startsWith(prefix)) {
                String suffix = command.substring(prefix.length());
                return parsers.get(prefix).parse(suffix, tokenizer.tokenize(suffix));
            }
        }
        return defaultParser.parse(command, tokenizer.tokenize(command));
    }

    public String getHelpText() {
        String commandHelps = parsers.entrySet().stream().map(
                e -> "\t%s: %s".formatted(e.getKey(), e.getValue().getDescription()))
                .collect(Collectors.joining("\n"));
        return """
        RitsIRC client commands. You may view more information about a given command using '/help <command>'
        
        Commands:
        %s""".formatted(commandHelps);
    }

    public String getHelpText(String command) {
        String cleanCommand = command.trim();
        if (!cleanCommand.startsWith("/")) {
            cleanCommand = "/" + cleanCommand;
        }
        if (parsers.containsKey(cleanCommand)) {
            return parsers.get(cleanCommand).getHelpText();
        } else {
            return """
            Could not find command '%s'
            
            %s""".formatted(cleanCommand, parsers.get(cleanCommand).getHelpText());
        }
    }
}
