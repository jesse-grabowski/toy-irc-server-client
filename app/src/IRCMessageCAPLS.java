import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

public final class IRCMessageCAPLS extends IRCMessage {

    public static final String COMMAND = "CAP";

    private String nick;
    private String version;
    private boolean hasMore;
    private SequencedMap<String, String> capabilities;

    public IRCMessageCAPLS(String rawMessage,
                           SequencedMap<String, String> tags,
                           String prefixName,
                           String prefixUser,
                           String prefixHost,
                           String nick,
                           String version,
                           boolean hasMore,
                           SequencedMap<String, String> capabilities) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.nick = nick;
        this.version = version;
        this.hasMore = hasMore;
        this.capabilities = capabilities;
    }

    public IRCMessageCAPLS(String nick, String version, boolean hasMore, SequencedMap<String, String> capabilities) {
        this(null, new LinkedHashMap<>(), null, null, null, nick, version, hasMore, capabilities);
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public SequencedMap<String, String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(SequencedMap<String, String> capabilities) {
        this.capabilities = capabilities;
    }
}