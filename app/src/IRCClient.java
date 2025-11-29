public class IRCClient {

    public static void main(String[] args) {
        IRCClientProperties properties = parseArgs(args);

        REPL.InteractiveREPL repl = new REPL.InteractiveREPL(System.in, System.out);
        repl.addInputHandler(repl::println);
        new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                repl.println("Hello from server %d".formatted(i++));
            }
        }).start();

        repl.start();
    }

    private static IRCClientProperties parseArgs(String[] args) {
        ArgsParser<IRCClientProperties> argsParser = new ArgsParser<>(IRCClient.class, IRCClientProperties::new)
                .addInetAddressPositional(0, IRCClientProperties::setHost, "hostname of the IRC server", true)
                .addIntegerFlag('p', "port", IRCClientProperties::setPort, "port of the IRC server (default 6667)", false)
                .addBooleanFlag('s', "simple-ui", IRCClientProperties::setUseSimpleTerminal, "use non-interactive mode (no cursor repositioning or dynamic updates; required on some terminals)", false);

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
