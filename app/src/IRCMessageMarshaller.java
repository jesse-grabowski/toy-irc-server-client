import java.util.SequencedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IRCMessageMarshaller {

    public String marshal(IRCMessage message) {
        return switch (message) {
            case IRCMessageCAPACK m -> marshal(m, this::marshalCapACK);
            case IRCMessageCAPDEL m -> marshal(m, this::marshalCapDEL);
            case IRCMessageCAPEND m -> marshal(m, this::marshalCapEND);
            case IRCMessageCAPLIST m -> marshal(m, this::marshalCapLIST);
            case IRCMessageCAPLS m -> marshal(m, this::marshalCapLS);
            case IRCMessageCAPNAK m -> marshal(m, this::marshalCapNAK);
            case IRCMessageCAPNEW m -> marshal(m, this::marshalCapNEW);
            case IRCMessageCAPREQ m -> marshal(m, this::marshalCapREQ);
            case IRCMessageJOIN0 m -> marshal(m, this::marshalJoin0);
            case IRCMessageJOINNormal m -> marshal(m, this::marshalJoinNormal);
            case IRCMessageNICK m -> marshal(m, this::marshalNick);
            case IRCMessagePING m -> marshal(m, this::marshalPing);
            case IRCMessagePONG m -> marshal(m, this::marshalPong);
            case IRCMessagePRIVMSG m -> marshal(m, this::marshalPrivmsg);
            case IRCMessageUSER m -> marshal(m, this::marshalUser);
            case IRCMessage001 m -> marshal(m, this::marshal001);
            case IRCMessageUnsupported m -> m.getRawMessage();
            case IRCMessageParseError m -> m.getRawMessage();
        };
    }

    private <T extends IRCMessage> String marshal(T message, Function<T, String> paramMarshaller) {
        StringBuilder messageBuilder = new StringBuilder();
        if (message.getTags() != null && !message.getTags().isEmpty()) {
            messageBuilder.append('@');
            messageBuilder.append(marshalTags(message.getTags()));
            messageBuilder.append(' ');
        }

        if (message.getPrefixName() != null) {
            messageBuilder.append(':');
            messageBuilder.append(
                    marshalPrefix(message.getPrefixName(), message.getPrefixUser(), message.getPrefixHost()));
            messageBuilder.append(' ');
        }

        messageBuilder.append(message.getCommand());

        String params = paramMarshaller.apply(message);
        if (params != null) {
            messageBuilder.append(' ');
            messageBuilder.append(params);
        }

        return messageBuilder.toString();
    }

    private String marshalTags(SequencedMap<String, String> tags) {
        return tags.sequencedEntrySet().stream()
                .map(e -> e.getValue() == null || e.getValue().isEmpty()
                        ? e.getKey()
                        : e.getKey() + "=" + escapeTag(e.getValue()))
                .collect(Collectors.joining(";"));
    }

    private String escapeTag(String value) {
        StringBuilder result = new StringBuilder(value.length());

        for (char c : value.toCharArray()) {
            switch (c) {
                case ';' -> result.append("\\:");
                case ' ' -> result.append("\\s");
                case '\\' -> result.append("\\\\");
                case '\r' -> result.append("\\r");
                case '\n' -> result.append("\\n");
                default -> result.append(c);
            }
        }

        return result.toString();
    }

    private String marshalPrefix(String name, String user, String host) {
        if (user != null && host != null) {
            return name + "!" + user + "@" + host;
        } else if (user != null) {
            return name + "!" + user;
        } else if (host != null) {
            return name + "@" + host;
        } else {
            return name;
        }
    }

    private String marshalCapACK(IRCMessageCAPACK message) {
        return message.getNick() + " ACK :" + Stream.concat(
                message.getEnableCapabilities().stream(),
                message.getDisableCapabilities().stream().map(s -> "-" + s)
        ).collect(Collectors.joining(" "));
    }

    private String marshalCapDEL(IRCMessageCAPDEL message) {
        return message.getNick() + " DEL :" + String.join(" ", message.getCapabilities());
    }

    private String marshalCapEND(IRCMessageCAPEND message) {
        return "END";
    }

    private String marshalCapLIST(IRCMessageCAPLIST message) {
        if (message.getNick() == null) { // request
            return "LIST";
        }

        if (message.isHasMore()) {
            return "%s LIST * :%s".formatted(message.getNick(), String.join(" ", message.getCapabilities()));
        } else {
            return "%s LIST :%s".formatted(message.getNick(), String.join(" ", message.getCapabilities()));
        }
    }

    private String marshalCapLS(IRCMessageCAPLS message) {
        if (message.getNick() == null) { // request
            if (message.getVersion() == null) {
                return "LS";
            } else {
                return "LS %s".formatted(message.getVersion());
            }
        }

        if (message.isHasMore()) {
            return "%s LS * :%s".formatted(message.getNick(), String.join(" ", message.getCapabilities()));
        } else {
            return "%s LS :%s".formatted(message.getNick(), String.join(" ", message.getCapabilities()));
        }
    }

    private String marshalCapNAK(IRCMessageCAPNAK message) {
        return message.getNick() + " NAK :" + Stream.concat(
                message.getEnableCapabilities().stream(),
                message.getDisableCapabilities().stream().map(s -> "-" + s)
        ).collect(Collectors.joining(" "));
    }

    private String marshalCapNEW(IRCMessageCAPNEW message) {
        return message.getNick() + " NEW :" + String.join(" ", message.getCapabilities());
    }

    private String marshalCapREQ(IRCMessageCAPREQ message) {
        return "REQ :" + Stream.concat(
                message.getEnableCapabilities().stream(),
                message.getDisableCapabilities().stream().map(s -> "-" + s)
        ).collect(Collectors.joining(" "));
    }

    private String marshalJoin0(IRCMessageJOIN0 message) {
        return "0";
    }

    private String marshalJoinNormal(IRCMessageJOINNormal message) {
        StringBuilder result = new StringBuilder();
        result.append(String.join(",", message.getChannels()));
        if (message.getKeys() != null && !message.getKeys().isEmpty()) {
            result.append(' ');
            result.append(String.join(",", message.getKeys()));
        }
        return result.toString();
    }

    private String marshalNick(IRCMessageNICK message) {
        return message.getNick();
    }

    private String marshalPing(IRCMessagePING message) {
        return ":" + message.getToken();
    }

    private String marshalPong(IRCMessagePONG message) {
        StringBuilder result = new StringBuilder();
        if (message.getServer() != null && !message.getServer().isBlank()) {
            result.append(message.getServer());
            result.append(' ');
        }
        result.append(':');
        result.append(message.getToken());
        return result.toString();
    }

    private String marshalPrivmsg(IRCMessagePRIVMSG message) {
        return String.join(",", message.getTargets()) + " :" + message.getMessage();
    }

    private String marshalUser(IRCMessageUSER message) {
        return message.getUser() + " 0 * :" + message.getRealName();
    }

    private String marshal001(IRCMessage001 message) {
        return message.getClient() + " :" + message.getMessage();
    }
}
