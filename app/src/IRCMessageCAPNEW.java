import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

public final class IRCMessageCAPNEW extends IRCMessage {

    public static final String COMMAND = "CAP";

    private String nick;
    private SequencedMap<String, String> capabilities;

    public IRCMessageCAPNEW(String rawMessage,
                            SequencedMap<String, String> tags,
                            String prefixName,
                            String prefixUser,
                            String prefixHost,
                            String nick,
                            SequencedMap<String, String> capabilities) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.nick = nick;
        this.capabilities = capabilities;
    }

    public IRCMessageCAPNEW(String nick, SequencedMap<String, String> capabilities) {
        this(null, new LinkedHashMap<>(), null, null, null, nick, capabilities);
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public SequencedMap<String, String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(SequencedMap<String, String> capabilities) {
        this.capabilities = capabilities;
    }
}