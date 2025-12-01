import java.util.SequencedMap;

public final class IRCMessageJOIN0 extends IRCMessage {

    public static final String COMMAND = "JOIN";

    public IRCMessageJOIN0(String rawMessage,
                           SequencedMap<String, String> tags,
                           String prefixName,
                           String prefixUser,
                           String prefixHost) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    }
}
