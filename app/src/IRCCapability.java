import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum IRCCapability {

    CAP_NOTIFY("cap-notify"),
    ECHO_MESSAGE("echo-message"),
    MESSAGE_TAGS("message-tags"),
    SERVER_TIME("server-time");

    private static final Map<String, IRCCapability> CAPABILITY_LOOKUP = new HashMap<>();

    static {
        for (IRCCapability capability : IRCCapability.values()) {
            CAPABILITY_LOOKUP.put(capability.name, capability);
        }
    }

    private final String name;

    IRCCapability(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Optional<IRCCapability> forName(String name) {
        return Optional.ofNullable(CAPABILITY_LOOKUP.get(name));
    }
}
