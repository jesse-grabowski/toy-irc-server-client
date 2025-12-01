import java.util.SequencedMap;

public final class IRCMessagePING extends IRCMessage {

    public static final String COMMAND = "PING";

    private final String token;

    public IRCMessagePING(String rawMessage,
                             SequencedMap<String, String> tags,
                             String prefixName,
                             String prefixUser,
                             String prefixHost,
                             String token) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
