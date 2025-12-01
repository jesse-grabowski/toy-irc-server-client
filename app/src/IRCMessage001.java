import java.util.SequencedMap;

public final class IRCMessage001 extends IRCMessage {

    public static final String COMMAND = "001";

    private final String message;

    public IRCMessage001(String rawMessage,
                         SequencedMap<String, String> tags,
                         String prefixName,
                         String prefixUser,
                         String prefixHost,
                         String message) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}