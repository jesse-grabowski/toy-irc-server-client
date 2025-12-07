import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageCAPREQ extends IRCMessage {

    public static final String COMMAND = "CAP";

    private List<String> enableCapabilities;
    private List<String> disableCapabilities;

    public IRCMessageCAPREQ(String rawMessage,
                            SequencedMap<String, String> tags,
                            String prefixName,
                            String prefixUser,
                            String prefixHost,
                            List<String> enableCapabilities,
                            List<String> disableCapabilities) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.enableCapabilities = enableCapabilities;
        this.disableCapabilities = disableCapabilities;
    }

    public IRCMessageCAPREQ(List<String> enableCapabilities, List<String> disableCapabilities) {
        this(null, new LinkedHashMap<>(), null, null, null, enableCapabilities, disableCapabilities);
    }

    public List<String> getEnableCapabilities() {
        return enableCapabilities;
    }

    public void setEnableCapabilities(List<String> enableCapabilities) {
        this.enableCapabilities = enableCapabilities;
    }

    public List<String> getDisableCapabilities() {
        return disableCapabilities;
    }

    public void setDisableCapabilities(List<String> disableCapabilities) {
        this.disableCapabilities = disableCapabilities;
    }
}