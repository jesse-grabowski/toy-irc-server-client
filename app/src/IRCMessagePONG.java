import java.util.SequencedMap;

public final class IRCMessagePONG extends IRCMessage {

    public static final String COMMAND = "PONG";

    private final String server;
    private final String token;

    public IRCMessagePONG(String rawMessage,
                          SequencedMap<String, String> tags,
                          String prefixName,
                          String prefixUser,
                          String prefixHost,
                          String server,
                          String token) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.server = server;
        this.token = token;
    }

    public String getServer() {
        return server;
    }

    public String getToken() {
        return token;
    }
}
