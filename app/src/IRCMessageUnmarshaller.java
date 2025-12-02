import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.SequencedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCMessageUnmarshaller {

    // This is really hard to read, but it's just splitting out the tags/prefix/command/params parts as defined
    // in the modern IRC grammar (but not processing individual tags/params yet)
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(
            "^(?:@(?<tags>[^\\s\\u0000]+)\\s+)?(?::(?<prefix>[^\\s\\u0000]+)\\s+)?(?<command>(?:[A-Za-z]+)|(?:\\d{3}))(?:\\s+(?<params>.+))?\\s*$");

    private static final Pattern PREFIX_PATTERN = Pattern.compile(
            "^(?<name>[^\\s\\u0000!@]+)(?:!(?<user>[^\\s\\u0000@]+))?(?:@(?<host>[^\\s\\u0000]+))?$");

    public IRCMessage unmarshal(String message) {
        Matcher matcher = MESSAGE_PATTERN.matcher(message);
        if (!matcher.matches()) {
            return new IRCMessageUnsupported(null, message, new LinkedHashMap<>(), null, null, null);
        }

        SequencedMap<String, String> tags = parseTags(matcher.group("tags"));
        PrefixParts prefix = parsePrefix(matcher.group("prefix"));
        String command = matcher.group("command").toUpperCase(Locale.ROOT);
        List<String> params = parseParams(matcher.group("params"));

        try {
            return switch (command) {
                case IRCMessageJOINNormal.COMMAND -> parseJoin(message, tags, prefix, params);
                case IRCMessageNICK.COMMAND -> parseNick(message, tags, prefix, params);
                case IRCMessagePING.COMMAND -> parsePing(message, tags, prefix, params);
                case IRCMessagePONG.COMMAND -> parsePong(message, tags, prefix, params);
                case IRCMessagePRIVMSG.COMMAND -> parsePrivmsg(message, tags, prefix, params);
                case IRCMessageUSER.COMMAND -> parseUser(message, tags, prefix, params);
                case IRCMessage001.COMMAND -> parse001(message, tags, prefix, params);
                default ->
                        new IRCMessageUnsupported(command, message, tags, prefix.name(), prefix.user(), prefix.host());
            };
        } catch (Exception e) {
            return new IRCMessageParseError(command, message, tags, prefix.name(), prefix.user(), prefix.host(), e.getMessage());
        }
    }

    private SequencedMap<String, String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return new LinkedHashMap<>();
        }

        SequencedMap<String, String> results = new LinkedHashMap<>();
        for (String tag : tags.split(";")) {
            String[] parts = tag.split("=", 2);
            if (parts.length == 1) { // boolean tag
                results.put(parts[0], "");
            } else {
                results.put(parts[0], unescapeTag(parts[1]));
            }
        }
        return results;
    }

    private String unescapeTag(String tag) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (char c: tag.toCharArray()) {
            if (escaped) {
                escaped = false;
                result.append(switch (c) {
                    case ':' -> ";";
                    case 's' -> " ";
                    case '\\' -> "\\";
                    case 'r' -> "\r";
                    case 'n' -> "\n";
                    default -> "\\" + c;
                });
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        if (escaped) {
            result.append('\\');
        }
        return result.toString();
    }

    private PrefixParts parsePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return new PrefixParts();
        }

        Matcher matcher = PREFIX_PATTERN.matcher(prefix);
        if (!matcher.matches()) {
            return new PrefixParts();
        }
        String name = matcher.group("name");
        String user = matcher.group("user");
        String host = matcher.group("host");
        return new PrefixParts(name, user, host);
    }

    private List<String> parseParams(String params) {
        if (params == null || params.isBlank()) {
            return List.of();
        }

        // params starts with a :
        if (params.startsWith(":")) {
            if (params.length() > 1) {
                return List.of(params.substring(1));
            } else {
                return List.of("");
            }
        }

        // otherwise we need to handle middle params
        String[] parts = params.split("\\s+:", 2);
        List<String> results = new ArrayList<>(Arrays.asList(parts[0].split("\\s+")));
        if (parts.length > 1) {
            results.add(parts[1]);
        }
        return results;
    }

    private IRCMessage parseJoin(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        if (params.isEmpty()) {
            throw new IllegalArgumentException("JOIN must have at least one parameter <channel>{,<channel>}");
        }

        if (params.size() == 1) {
            String param = params.getFirst();
            if ("0".equals(param)) {
                return new IRCMessageJOIN0(raw, tags, prefix.name(), prefix.user(), prefix.host());
            } else {
                return new IRCMessageJOINNormal(raw, tags, prefix.name(), prefix.user(), prefix.host(), Arrays.asList(param.split(",")), List.of());
            }
        }

        List<String> channels = Arrays.asList(params.get(0).split(","));
        List<String> keys = Arrays.asList(params.get(1).split(","));

        return new IRCMessageJOINNormal(raw, tags, prefix.name(), prefix.user(), prefix.host(), channels, keys);
    }

    private IRCMessageNICK parseNick(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        return new IRCMessageNICK(raw, tags, prefix.name(), prefix.user(), prefix.host(), safeGetIndex(params, 0));
    }

    private IRCMessagePING parsePing(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        return new IRCMessagePING(raw, tags, prefix.name(), prefix.user(), prefix.host(), safeGetLast(params));
    }

    private IRCMessagePONG parsePong(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        if (params.size() == 2) {
            return new IRCMessagePONG(raw, tags, prefix.name(), prefix.user(), prefix.host(), params.get(0), params.get(1));
        } else {
            return new IRCMessagePONG(raw, tags, prefix.name(), prefix.user(), prefix.host(), null, safeGetLast(params));
        }
    }

    private IRCMessagePRIVMSG parsePrivmsg(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        if (params.size() < 2) {
            throw new IllegalArgumentException("PRIVMSG must have at least 2 parameters <targets> :<message>");
        }
        return new IRCMessagePRIVMSG(raw, tags, prefix.name(), prefix.user(), prefix.host(), Arrays.asList(params.getFirst().split(",")), safeGetLast(params));
    }

    private IRCMessageUSER parseUser(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        if (params.size() < 4) {
            throw new IllegalArgumentException("USER must have at least 4 parameters <user> ~ ~ <realName>");
        }
        return new IRCMessageUSER(raw, tags, prefix.name(), prefix.user(), prefix.host(), safeGetIndex(params, 0), safeGetIndex(params, 3));
    }

    private IRCMessage001 parse001(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        return new IRCMessage001(raw, tags, prefix.name(), prefix.user(), prefix.host(), safeGetIndex(params, 0), safeGetLast(params));
    }

    private String safeGetLast(List<String> params) {
        return params.isEmpty() ? null : params.getLast();
    }

    private String safeGetIndex(List<String> params, int index) {
        if (index < 0 || index >= params.size()) {
            return null;
        }
        return params.get(index);
    }

    private record PrefixParts(String name, String user, String host) {
        PrefixParts() {
            this(null, null, null);
        }
    }
}
