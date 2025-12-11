import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageCAPLIST extends IRCMessage {

    public static final String COMMAND = "CAP";

    private final String nick;
    private final boolean hasMore;
    private final List<String> capabilities;

    public IRCMessageCAPLIST(String rawMessage,
                             SequencedMap<String, String> tags,
                             String prefixName,
                             String prefixUser,
                             String prefixHost,
                             String nick,
                             boolean hasMore,
                             List<String> capabilities) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.nick = nick;
        this.hasMore = hasMore;
        this.capabilities = capabilities;
    }

    public IRCMessageCAPLIST(String nick, boolean hasMore, List<String> capabilities) {
        this(null, new LinkedHashMap<>(), null, null, null, nick, hasMore, capabilities);
    }

    public String getNick() {
        return nick;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }
}