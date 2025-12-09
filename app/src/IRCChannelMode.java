import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum IRCChannelMode {
    OWNER("q", "~"),
    ADMIN("a", "&"),
    OPERATOR("o", "@"),
    HALFOP("h", "%"),
    VOICE("v", "+");

    private static final Map<String, IRCChannelMode> MODES_BY_FLAG = new HashMap<>();
    private static final Map<String, IRCChannelMode> MODES_BY_PREFIX = new HashMap<>();

    static {
        for (IRCChannelMode mode : IRCChannelMode.values()) {
            MODES_BY_FLAG.put(mode.flag, mode);
            MODES_BY_PREFIX.put(mode.prefix, mode);
        }
    }

    private final String flag;
    private final String prefix;

    IRCChannelMode(String flag, String prefix) {
        this.flag = flag;
        this.prefix = prefix;
    }

    public static Optional<IRCChannelMode> getByFlag(String flag) {
        return Optional.ofNullable(MODES_BY_FLAG.get(flag));
    }

    public static Optional<IRCChannelMode> getByPrefix(String prefix) {
        return Optional.ofNullable(MODES_BY_PREFIX.get(prefix));
    }

    public String getFlag() {
        return flag;
    }

    public String getPrefix() {
        return prefix;
    }
}
