import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageNOTICE extends IRCMessage {

    public static final String COMMAND = "NOTICE";

    private final List<String> targets;
    private final String message;

    public IRCMessageNOTICE(String rawMessage,
                            SequencedMap<String, String> tags,
                            String prefixName,
                            String prefixUser,
                            String prefixHost,
                            List<String> targets,
                            String message) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.targets = targets;
        this.message = message;
    }

    public IRCMessageNOTICE(List<String> targets, String message) {
        this(null, new LinkedHashMap<>(), null, null, null, targets, message);
    }

    public List<String> getTargets() {
        return targets;
    }

    public String getMessage() {
        return message;
    }
}
