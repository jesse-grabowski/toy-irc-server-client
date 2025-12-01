import java.util.SequencedMap;

public final class IRCMessageUnsupported extends IRCMessage {

    public IRCMessageUnsupported(String command,
                                 String rawMessage,
                                 SequencedMap<String, String> tags,
                                 String prefixName,
                                 String prefixUser,
                                 String prefixHost) {
        super(command, rawMessage, tags, prefixName, prefixUser, prefixHost);
    }
}
