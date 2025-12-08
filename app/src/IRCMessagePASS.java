import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessagePASS extends IRCMessage {

    public static final String COMMAND = "PASS";

    private final String pass;

    public IRCMessagePASS(String rawMessage,
                          SequencedMap<String, String> tags,
                          String prefixName,
                          String prefixUser,
                          String prefixHost,
                          String pass) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.pass = pass;
    }

    public IRCMessagePASS(String pass) {
        this(null, new LinkedHashMap<>(), null, null, null, pass);
    }

    public String getPass() {
        return pass;
    }
}
