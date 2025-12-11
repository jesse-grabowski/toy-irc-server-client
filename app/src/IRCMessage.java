import java.util.SequencedMap;

public sealed abstract class IRCMessage permits
        // capability negotiation
        IRCMessageCAPACK, IRCMessageCAPDEL, IRCMessageCAPEND, IRCMessageCAPLIST, IRCMessageCAPLS, IRCMessageCAPNAK,
        IRCMessageCAPNEW, IRCMessageCAPREQ,
        // standard messages
        IRCMessageJOIN0, IRCMessageJOINNormal, IRCMessageNICK, IRCMessagePART, IRCMessagePASS, IRCMessagePING, IRCMessagePONG,
        IRCMessagePRIVMSG, IRCMessageUSER, IRCMessageQUIT,
        // numerics
        IRCMessage001, IRCMessage353,
        // other / unsupported
        IRCMessageUnsupported, IRCMessageParseError {

    private final String rawMessage;

    private final SequencedMap<String, String> tags;
    private final String prefixName;
    private final String prefixUser;
    private final String prefixHost;
    private final String command;

    protected IRCMessage(String command,
                         String rawMessage,
                         SequencedMap<String, String> tags,
                         String prefixName,
                         String prefixUser,
                         String prefixHost) {
        this.rawMessage = rawMessage;
        this.tags = tags;
        this.prefixName = prefixName;
        this.prefixUser = prefixUser;
        this.prefixHost = prefixHost;
        this.command = command;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public SequencedMap<String, String> getTags() {
        return tags;
    }

    public String getPrefixName() {
        return prefixName;
    }

    public String getPrefixUser() {
        return prefixUser;
    }

    public String getPrefixHost() {
        return prefixHost;
    }

    public String getCommand() {
        return command;
    }
}
