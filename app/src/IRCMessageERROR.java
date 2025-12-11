import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageERROR extends IRCMessage {

    public static final String COMMAND = "ERROR";

    private final String reason;

    public IRCMessageERROR(String rawMessage,
                           SequencedMap<String, String> tags,
                           String prefixName,
                           String prefixUser,
                           String prefixHost,
                           String reason) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.reason = reason;
    }

    public IRCMessageERROR(String reason) {
        this(null, new LinkedHashMap<>(), null, null, null, reason);
    }

    public String getReason() {
        return reason;
    }
}
