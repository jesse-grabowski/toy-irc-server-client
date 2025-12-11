import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessageMODE extends IRCMessage {

    public static final String COMMAND = "MODE";

    private final String target;
    private final String modeString;
    private final List<String> modeArguments;

    public IRCMessageMODE(String rawMessage,
                          SequencedMap<String, String> tags,
                          String prefixName,
                          String prefixUser,
                          String prefixHost,
                          String target,
                          String modeString,
                          List<String> modeArguments) {
        super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
        this.target = target;
        this.modeString = modeString;
        this.modeArguments = modeArguments;
    }

    public IRCMessageMODE(String target, String modeString, List<String> modeArguments) {
        this(null, new LinkedHashMap<>(), null, null, null, target, modeString, modeArguments);
    }

    public String getTarget() {
        return target;
    }

    public String getModeString() {
        return modeString;
    }

    public List<String> getModeArguments() {
        return modeArguments;
    }
}
