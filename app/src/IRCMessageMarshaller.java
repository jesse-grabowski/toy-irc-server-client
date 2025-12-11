import java.util.List;
import java.util.Map;
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
            case IRCMessageERROR m -> marshal(m, this::marshalError);
            case IRCMessageJOIN0 m -> marshal(m, this::marshalJoin0);
            case IRCMessageJOINNormal m -> marshal(m, this::marshalJoinNormal);
            case IRCMessageKICK m -> marshal(m, this::marshalKick);
            case IRCMessageMODE m -> marshal(m, this::marshalMode);
            case IRCMessageNICK m -> marshal(m, this::marshalNick);
            case IRCMessageNOTICE m -> marshal(m, this::marshalNotice);
            case IRCMessagePART m -> marshal(m, this::marshalPart);
            case IRCMessagePASS m -> marshal(m, this::marshalPass);
            case IRCMessagePING m -> marshal(m, this::marshalPing);
            case IRCMessagePONG m -> marshal(m, this::marshalPong);
            case IRCMessagePRIVMSG m -> marshal(m, this::marshalPrivmsg);
            case IRCMessageUSER m -> marshal(m, this::marshalUser);
            case IRCMessageQUIT m -> marshal(m, this::marshalQuit);
            case IRCMessage001 m -> marshal(m, this::marshal001);
            case IRCMessage005 m -> marshal(m, this::marshal005);
            case IRCMessage353 m -> marshal(m, this::marshal353);
            case IRCMessageUnsupported m -> m.getRawMessage();
            case IRCMessageParseError m -> m.getRawMessage();
        };
    }

    private <T extends IRCMessage> String marshal(T message, Function<T, String> paramMarshaller) {
        StringBuilder messageBuilder = new StringBuilder();
        if (message.getTags() != null && !message.getTags().isEmpty()) {
            messageBuilder.append('@');
            messageBuilder.append(marshalMap(message.getTags(), ";", this::escapeTag));
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

        return "%s LS%s :%s".formatted(
                message.getNick(),
                message.isHasMore() ? " *" : "",
                marshalMap(message.getCapabilities(), " ", Function.identity()));
    }

    private String marshalCapNAK(IRCMessageCAPNAK message) {
        return message.getNick() + " NAK :" + Stream.concat(
                message.getEnableCapabilities().stream(),
                message.getDisableCapabilities().stream().map(s -> "-" + s)
        ).collect(Collectors.joining(" "));
    }

    private String marshalCapNEW(IRCMessageCAPNEW message) {
        return message.getNick() + " NEW :" + marshalMap(message.getCapabilities(), " ", Function.identity());
    }

    private String marshalCapREQ(IRCMessageCAPREQ message) {
        return "REQ :" + Stream.concat(
                message.getEnableCapabilities().stream(),
                message.getDisableCapabilities().stream().map(s -> "-" + s)
        ).collect(Collectors.joining(" "));
    }

    private String marshalError(IRCMessageERROR message) {
        return ":" + message.getReason();
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

    private String marshalKick(IRCMessageKICK message) {
        return "%s %s%s".formatted(message.getChannel(), message.getNick(),
                message.getReason() != null ? " :" + message.getReason() : "");
    }

    private String marshalMode(IRCMessageMODE message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.getTarget());
        if (message.getModeString() != null) {
            sb.append(' ');
            sb.append(message.getModeString());
            for (int i = 0; i < message.getModeArguments().size() - 1; i++) {
                sb.append(' ');
                sb.append(message.getModeArguments().get(i));
            }
            if (!message.getModeArguments().isEmpty()) {
                sb.append(' ');
                sb.append(':');
                sb.append(message.getModeArguments().getLast());
            }
        }
        return sb.toString();
    }

    private String marshalNick(IRCMessageNICK message) {
        return message.getNick();
    }

    private String marshalNotice(IRCMessageNOTICE message) {
        return String.join(",", message.getTargets()) + " :" + message.getMessage();
    }

    private String marshalPart(IRCMessagePART message) {
        return String.join(",", message.getChannels()) + (message.getReason() != null ? " :" + message.getReason() : "");
    }

    private String marshalPass(IRCMessagePASS message) {
        return message.getPass();
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

    private String marshalQuit(IRCMessageQUIT message) {
        return message.getReason() == null ? null : ":" + message.getReason();
    }

    private String marshal001(IRCMessage001 message) {
        return message.getClient() + " :" + message.getMessage();
    }

    private String marshal005(IRCMessage005 message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.getClient());
        for (Map.Entry<String, String> parameter : message.getParameters().entrySet()) {
            sb.append(' ');
            sb.append(parameter.getKey());
            if (parameter.getValue() != null && !parameter.getValue().isEmpty()) {
                sb.append('=');
                sb.append(parameter.getValue());
            }
        }
        sb.append(" :are supported by this server");
        return sb.toString();
    }

    private String marshal353(IRCMessage353 message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.getClient());
        sb.append(' ');
        sb.append(message.getSymbol());
        sb.append(' ');
        sb.append(message.getChannel());
        sb.append(' ');
        sb.append(':');

        List<String> nicks = message.getNicks();
        List<Character> modes = message.getModes();
        for (int i = 0; i < nicks.size(); i++) {
            sb.append(modes.size() > i && modes.get(i) != null ? modes.get(i) : "");
            sb.append(nicks.get(i));
            if (i < nicks.size() - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private <T> String marshalMap(SequencedMap<String, String> map, String delimiter, Function<String, String> mapper) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        return map.sequencedEntrySet().stream()
                .map(e -> e.getValue() == null || e.getValue().isEmpty()
                        ? e.getKey()
                        : e.getKey() + "=" + mapper.apply(e.getValue()))
                .collect(Collectors.joining(delimiter));
    }
}
