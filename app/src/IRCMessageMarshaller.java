import java.util.SequencedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IRCMessageMarshaller {

    public String marshal(IRCMessage message) {
        return switch (message) {
            case IRCMessageJOIN0 m -> marshal(m, this::marshallJoin0);
            case IRCMessageJOINNormal m -> marshal(m, this::marshallJoinNormal);
            case IRCMessageNICK m -> marshal(m, this::marshallNick);
            case IRCMessagePING m -> marshal(m, this::marshallPing);
            case IRCMessagePONG m -> marshal(m, this::marshallPong);
            case IRCMessagePRIVMSG m -> marshal(m, this::marshallPrivmsg);
            case IRCMessageUSER m -> marshal(m, this::marshallUser);
            case IRCMessage001 m -> marshal(m, this::marshall001);
            case IRCMessageUnsupported m -> m.getRawMessage();
            case IRCMessageParseError m -> m.getRawMessage();
        };
    }

    private <T extends IRCMessage> String marshal(T message, Function<T, String> paramMarshaller) {
        StringBuilder messageBuilder = new StringBuilder();
        if (message.getTags() != null && !message.getTags().isEmpty()) {
            messageBuilder.append('@');
            messageBuilder.append(marshallTags(message.getTags()));
            messageBuilder.append(' ');
        }

        if (message.getPrefixName() != null) {
            messageBuilder.append(':');
            messageBuilder.append(
                    marshallPrefix(message.getPrefixName(), message.getPrefixUser(), message.getPrefixHost()));
            messageBuilder.append(' ');
        }

        messageBuilder.append(message.getCommand());

        String params = paramMarshaller.apply(message);
        if (params != null) {
            messageBuilder.append(' ');
            messageBuilder.append(params);
        }

        messageBuilder.append('\r');
        messageBuilder.append('\n');

        return messageBuilder.toString();
    }

    private String marshallTags(SequencedMap<String, String> tags) {
        return tags.sequencedEntrySet().stream()
                .map(e -> e.getValue() == null ? e.getKey() : e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
    }

    private String marshallPrefix(String name, String user, String host) {
        if (user != null && host != null) {
            return name + "!" + user + "@" + host;
        } else {
            return name;
        }
    }

    private String marshallJoin0(IRCMessageJOIN0 message) {
        return "0";
    }

    private String marshallJoinNormal(IRCMessageJOINNormal message) {
        StringBuilder result = new StringBuilder();
        result.append(String.join(",", message.getChannels()));
        if (message.getKeys() != null && !message.getKeys().isEmpty()) {
            result.append(' ');
            result.append(String.join(",", message.getKeys()));
        }
        return result.toString();
    }

    private String marshallNick(IRCMessageNICK message) {
        return message.getNick();
    }

    private String marshallPing(IRCMessagePING message) {
        return ":" + message.getToken();
    }

    private String marshallPong(IRCMessagePONG message) {
        StringBuilder result = new StringBuilder();
        if (message.getServer() != null && !message.getServer().isBlank()) {
            result.append(message.getServer());
            result.append(' ');
        }
        result.append(':');
        result.append(message.getToken());
        return result.toString();
    }

    private String marshallPrivmsg(IRCMessagePRIVMSG message) {
        return String.join(",", message.getTargets()) + " :" + message.getMessage();
    }

    private String marshallUser(IRCMessageUSER message) {
        return message.getUser() + " 0 * :" + message.getRealName();
    }

    private String marshall001(IRCMessage001 message) {
        return ":" + message.getMessage();
    }
}
