import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageCAPDEL extends IRCMessage {

    public static final String COMMAND = "CAP";

    private String nick;
    private List<String> capabilities;

    public IRCMessageCAPDEL(String rawMessage,
                            SequencedMap<String, String> tags,
                            String prefixName,
                            String prefixUser,
                            String prefixHost,
                            String nick,
                            List<String> capabilities) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.nick = nick;
        this.capabilities = capabilities;
    }

    public IRCMessageCAPDEL(String nick, List<String> capabilities) {
        this(null, new LinkedHashMap<>(), null, null, null, nick, capabilities);
    }

    public String getNick() {
        return nick;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }
}