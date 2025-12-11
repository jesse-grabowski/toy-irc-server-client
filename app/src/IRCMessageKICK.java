import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageKICK extends IRCMessage {

    public static final String COMMAND = "KICK";

    private final String channel;
    private final String nick;
    private final String reason;

    public IRCMessageKICK(String rawMessage,
                          SequencedMap<String, String> tags,
                          String prefixName,
                          String prefixUser,
                          String prefixHost,
                          String channel,
                          String nick,
                          String reason) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.channel = channel;
        this.nick = nick;
        this.reason = reason;
    }

    public IRCMessageKICK(String channel, String nick, String reason) {
        this(null, new LinkedHashMap<>(), null, null, null, channel, nick, reason);
    }

    public String getChannel() {
        return channel;
    }

    public String getNick() {
        return nick;
    }

    public String getReason() {
        return reason;
    }
}
