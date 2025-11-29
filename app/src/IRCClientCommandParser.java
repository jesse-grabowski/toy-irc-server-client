public class IRCClientCommandParser {

    private final REPL repl;
    private final IRCClientEngine engine;

    public IRCClientCommandParser(REPL repl, IRCClientEngine engine) {
        this.repl = repl;
        this.engine = engine;
    }

    public void accept(String command) {
        engine.send(new IRCClientCommand());
    }

}
