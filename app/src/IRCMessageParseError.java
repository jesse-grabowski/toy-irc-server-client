import java.util.SequencedMap;

public final class IRCMessageParseError extends IRCMessage {

    private final String error;

    public IRCMessageParseError(String command,
                                String rawMessage,
                                SequencedMap<String, String> tags,
                                String prefixName,
                                String prefixUser,
                                String prefixHost,
                                String error) {
        super(command, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
