import java.util.List;
import java.util.SequencedMap;

public final class IRCMessagePRIVMSG extends IRCMessage {

    public static final String COMMAND = "PRIVMSG";

    private final List<String> targets;
    private final String message;

    public IRCMessagePRIVMSG(String rawMessage,
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

    public List<String> getTargets() {
        return targets;
    }

    public String getMessage() {
        return message;
    }
}
