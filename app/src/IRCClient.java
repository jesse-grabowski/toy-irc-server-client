import java.io.IOException;

public class IRCClient {

    public static void main(String[] args) throws IOException {
        IRCClientProperties properties = parseArgs(args);

        TUI.InteractiveTUI repl = new TUI.InteractiveTUI(System.in, System.out);
        IRCClientEngine engine = new IRCClientEngine(properties, repl);
        IRCClientCommandParser parser = new IRCClientCommandParser(repl, engine);
        repl.addInputHandler(parser::accept);
        engine.start();
        repl.start();
    }

    private static IRCClientProperties parseArgs(String[] args) {
        ArgsParser<IRCClientProperties> argsParser = new ArgsParser<>(IRCClient.class, IRCClientProperties::new)
                .addInetAddressPositional(0, IRCClientProperties::setHost, "hostname of the IRC server", true)
                .addIntegerFlag('p', "port", IRCClientProperties::setPort, "port of the IRC server (default 6667)", false)
                .addBooleanFlag('s', "simple-ui", IRCClientProperties::setUseSimpleTerminal, "use non-interactive mode (no cursor repositioning or dynamic updates; required on some terminals)", false)
                .addStringFlag('n', "nickname", IRCClientProperties::setNickname, "nickname of the IRC user", false)
                .addStringFlag('r', "real-name", IRCClientProperties::setRealName, "real name of the IRC user", false);

        try {
            return argsParser.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.out.println(argsParser.getHelpText());
            System.exit(2);
        } catch (ArgsParserHelpRequestedException e) {
            System.out.println(argsParser.getHelpText());
            System.exit(0);
        }

        // this should never happen
        throw new AssertionError("Application failed to terminate on System.exit");
    }

}
