import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageCAPACK extends IRCMessage {

    public static final String COMMAND = "CAP";

    private final String nick;
    private final List<String> enableCapabilities;
    private final List<String> disableCapabilities;

    public IRCMessageCAPACK(String rawMessage,
                            SequencedMap<String, String> tags,
                            String prefixName,
                            String prefixUser,
                            String prefixHost,
                            String nick,
                            List<String> enableCapabilities,
                            List<String> disableCapabilities) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.nick = nick;
        this.enableCapabilities = enableCapabilities;
        this.disableCapabilities = disableCapabilities;
    }

    public IRCMessageCAPACK(String nick, List<String> enableCapabilities, List<String> disableCapabilities) {
        this(null, new LinkedHashMap<>(), null, null, null, nick, enableCapabilities, disableCapabilities);
    }

    public String getNick() {
        return nick;
    }

    public List<String> getEnableCapabilities() {
        return enableCapabilities;
    }

    public List<String> getDisableCapabilities() {
        return disableCapabilities;
    }
}