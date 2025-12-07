import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageNICK extends IRCMessage {

    public static final String COMMAND = "NICK";

    private final String nick;

    public IRCMessageNICK(String rawMessage,
                          SequencedMap<String, String> tags,
                          String prefixName,
                          String prefixUser,
                          String prefixHost,
                          String nick) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.nick = nick;
    }

    public IRCMessageNICK(String nick) {
        this(null, new LinkedHashMap<>(), null, null, null, nick);
    }

    public String getNick() {
        return nick;
    }
}
