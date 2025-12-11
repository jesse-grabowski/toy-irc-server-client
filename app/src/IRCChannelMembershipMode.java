import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum IRCChannelMembershipMode {
    OWNER("q", "~"),
    ADMIN("a", "&"),
    OPERATOR("o", "@"),
    HALFOP("h", "%"),
    VOICE("v", "+");

    private static final Map<String, IRCChannelMembershipMode> MODES_BY_FLAG = new HashMap<>();
    private static final Map<String, IRCChannelMembershipMode> MODES_BY_PREFIX = new HashMap<>();

    static {
        for (IRCChannelMembershipMode mode : IRCChannelMembershipMode.values()) {
            MODES_BY_FLAG.put(mode.flag, mode);
            MODES_BY_PREFIX.put(mode.prefix, mode);
        }
    }

    private final String flag;
    private final String prefix;

    IRCChannelMembershipMode(String flag, String prefix) {
        this.flag = flag;
        this.prefix = prefix;
    }

    public static Optional<IRCChannelMembershipMode> getByFlag(String flag) {
        return Optional.ofNullable(MODES_BY_FLAG.get(flag));
    }

    public static Optional<IRCChannelMembershipMode> getByPrefix(String prefix) {
        return Optional.ofNullable(MODES_BY_PREFIX.get(prefix));
    }

    public String getFlag() {
        return flag;
    }

    public String getPrefix() {
        return prefix;
    }
}
