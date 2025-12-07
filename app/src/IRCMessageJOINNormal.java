import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageJOINNormal extends IRCMessage {

    public static final String COMMAND = "JOIN";

    private final List<String> channels;
    private final List<String> keys;

    public IRCMessageJOINNormal(String rawMessage,
                                SequencedMap<String, String> tags,
                                String prefixName,
                                String prefixUser,
                                String prefixHost,
                                List<String> channels,
                                List<String> keys) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.channels = channels;
        this.keys = keys;
    }

    public IRCMessageJOINNormal(List<String> channels, List<String> keys) {
        this(null, new LinkedHashMap<>(), null, null, null, channels, keys);
    }

    public List<String> getChannels() {
        return channels;
    }

    public List<String> getKeys() {
        return keys;
    }
}
