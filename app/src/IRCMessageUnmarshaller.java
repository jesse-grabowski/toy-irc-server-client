import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.SequencedMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCMessageUnmarshaller {

    // This is really hard to read, but it's just splitting out the tags/prefix/command/params parts as defined
    // in the modern IRC grammar (but not processing individual tags/params yet)
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(
            "^(?:@(?<tags>[^\\s\\u0000]+)\\s+)?(?::(?<prefix>[^\\s\\u0000]+)\\s+)?(?<command>(?:[A-Za-z]+)|(?:\\d{3}))(?:\\s+(?<params>.+))?\\s*$");

    private static final Pattern PREFIX_PATTERN = Pattern.compile(
            "^(?<name>[^\\s\\u0000!@]+)(?:!(?<user>[^\\s\\u0000@]+))?(?:@(?<host>[^\\s\\u0000]+))?$");

    private static final Set<String> CAP_KNOWN_SUBCOMMANDS = Set.of("LS", "LIST", "REQ", "ACK", "NAK", "END", "NEW", "DEL");

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
                case IRCMessageCAPLS.COMMAND -> parseCap(message, tags, prefix, params);
                case IRCMessageJOINNormal.COMMAND -> parseJoin(message, tags, prefix, params);
                case IRCMessageNICK.COMMAND -> parseNick(message, tags, prefix, params);
                case IRCMessagePASS.COMMAND -> parsePass(message, tags, prefix, params);
                case IRCMessagePING.COMMAND -> parsePing(message, tags, prefix, params);
                case IRCMessagePONG.COMMAND -> parsePong(message, tags, prefix, params);
                case IRCMessagePRIVMSG.COMMAND -> parsePrivmsg(message, tags, prefix, params);
                case IRCMessageUSER.COMMAND -> parseUser(message, tags, prefix, params);
                case IRCMessageQUIT.COMMAND -> parseQuit(message, tags, prefix, params);
                case IRCMessage001.COMMAND -> parse001(message, tags, prefix, params);
                case IRCMessage353.COMMAND -> parse353(message, tags, prefix, params);
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

    private IRCMessage parseCap(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        if (params.isEmpty()) {
            throw new IllegalArgumentException("CAP must have at least one parameter <subcommand>");
        }

        String nickOrSubcommand = safeGetIndex(params, 0);
        if (CAP_KNOWN_SUBCOMMANDS.contains(nickOrSubcommand)) {
            return switch (nickOrSubcommand) {
                case "END" -> new IRCMessageCAPEND(raw, tags, prefix.name(), prefix.user(), prefix.host());
                case "LS" -> new IRCMessageCAPLS(raw, tags, prefix.name(), prefix.user(), prefix.host(), null, safeGetIndex(params, 1), false, List.of());
                case "LIST" -> new IRCMessageCAPLIST(raw, tags, prefix.name(), prefix.user(), prefix.host(), null, false, List.of());
                case "REQ" -> {
                    String rawCapabilities = safeGetIndex(params, 1);
                    if (rawCapabilities == null) {
                        yield new IRCMessageCAPREQ(raw, tags, prefix.name(), prefix.user(), prefix.host(), List.of(), List.of());
                    }

                    List<String> enabledCapabilities = new ArrayList<>();
                    List<String> disabledCapabilities = new ArrayList<>();
                    for (String cap : rawCapabilities.split("\\s+")) {
                        if (cap.startsWith("-")) {
                            disabledCapabilities.add(cap.substring(1));
                        } else {
                            enabledCapabilities.add(cap);
                        }
                    }
                    yield new IRCMessageCAPREQ(raw, tags, prefix.name(), prefix.user(), prefix.host(), enabledCapabilities, disabledCapabilities);
                }
                default -> throw new IllegalArgumentException("Unsupported subcommand: " + nickOrSubcommand);
            };
        } else {
            String subcommand = safeGetIndex(params, 1);
            return switch (subcommand) {
                case "ACK" -> {
                    String rawCapabilities = safeGetIndex(params, 2);
                    if (rawCapabilities == null) {
                        yield new IRCMessageCAPACK(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, List.of(), List.of());
                    }

                    List<String> enabledCapabilities = new ArrayList<>();
                    List<String> disabledCapabilities = new ArrayList<>();
                    for (String cap : rawCapabilities.split("\\s+")) {
                        if (cap.startsWith("-")) {
                            disabledCapabilities.add(cap.substring(1));
                        } else {
                            enabledCapabilities.add(cap);
                        }
                    }
                    yield new IRCMessageCAPACK(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, enabledCapabilities, disabledCapabilities);
                }
                case "DEL" -> {
                    String rawCapabilities = safeGetIndex(params, 2);
                    if (rawCapabilities == null) {
                        yield new IRCMessageCAPDEL(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, List.of());
                    }
                    yield new IRCMessageCAPDEL(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, Arrays.asList(rawCapabilities.split("\\s+")));
                }
                case "LS" -> {
                    if (params.size() == 4) {
                        String capabilities = safeGetIndex(params, 3);
                        if (capabilities == null || capabilities.isBlank()) {
                            yield new IRCMessageCAPLS(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, null, true, List.of());
                        } else {
                            yield new IRCMessageCAPLS(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, null, true, Arrays.asList(capabilities.split("\\s+")));
                        }
                    } else if (params.size() == 3) {
                        String capabilities = safeGetIndex(params, 2);
                        if (capabilities == null || capabilities.isBlank()) {
                            yield new IRCMessageCAPLS(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, null, false, List.of());
                        } else {
                            yield new IRCMessageCAPLS(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, null, false, Arrays.asList(capabilities.split("\\s+")));
                        }
                    } else {
                        throw new IllegalArgumentException("Incorrect arguments for subcommand: " + subcommand);
                    }
                }
                case "LIST" -> {
                    if (params.size() == 4) {
                        String capabilities = safeGetIndex(params, 3);
                        if (capabilities == null || capabilities.isBlank()) {
                            yield new IRCMessageCAPLIST(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, true, List.of());
                        } else {
                            yield new IRCMessageCAPLIST(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, true, Arrays.asList(capabilities.split("\\s+")));
                        }
                    } else if (params.size() == 3) {
                        String capabilities = safeGetIndex(params, 2);
                        if (capabilities == null || capabilities.isBlank()) {
                            yield new IRCMessageCAPLIST(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, false, List.of());
                        } else {
                            yield new IRCMessageCAPLIST(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, false, Arrays.asList(capabilities.split("\\s+")));
                        }
                    } else {
                        throw new IllegalArgumentException("Incorrect arguments for subcommand: " + subcommand);
                    }
                }
                case "NAK" -> {
                    String rawCapabilities = safeGetIndex(params, 2);
                    if (rawCapabilities == null) {
                        yield new IRCMessageCAPNAK(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, List.of(), List.of());
                    }

                    List<String> enabledCapabilities = new ArrayList<>();
                    List<String> disabledCapabilities = new ArrayList<>();
                    for (String cap : rawCapabilities.split("\\s+")) {
                        if (cap.startsWith("-")) {
                            disabledCapabilities.add(cap.substring(1));
                        } else {
                            enabledCapabilities.add(cap);
                        }
                    }
                    yield new IRCMessageCAPNAK(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, enabledCapabilities, disabledCapabilities);
                }
                case "NEW" -> {
                    String rawCapabilities = safeGetIndex(params, 2);
                    if (rawCapabilities == null) {
                        yield new IRCMessageCAPNEW(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, List.of());
                    }
                    yield new IRCMessageCAPNEW(raw, tags, prefix.name(), prefix.user(), prefix.host(), nickOrSubcommand, Arrays.asList(rawCapabilities.split("\\s+")));
                }
                default -> throw new IllegalArgumentException("Unsupported subcommand: " + subcommand);
            };
        }
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

    private IRCMessagePASS parsePass(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        return new IRCMessagePASS(raw, tags, prefix.name(), prefix.user(), prefix.host(), safeGetIndex(params, 0));
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

    private IRCMessageQUIT parseQuit(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        return new IRCMessageQUIT(raw, tags, prefix.name(), prefix.user(), prefix.host(), safeGetLast(params));
    }

    private IRCMessage001 parse001(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        return new IRCMessage001(raw, tags, prefix.name(), prefix.user(), prefix.host(), safeGetIndex(params, 0), safeGetLast(params));
    }

    private IRCMessage353 parse353(String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
        String nicksRaw = safeGetIndex(params, 3);
        List<String> nicks = new ArrayList<>();
        List<String> modes = new ArrayList<>();
        if (nicksRaw != null) {
            String[] splitNicks = nicksRaw.split("\\s+", -1);
            for (String nick : splitNicks) {
                if (nick.isBlank()) {
                    continue;
                }
                switch (nick.charAt(0)) {
                    case '~', '&', '@', '%', '+' -> {
                        modes.add("" + nick.charAt(0));
                        nicks.add(nick.substring(1));
                    }
                    default -> {
                        modes.add("");
                        nicks.add(nick);
                    }
                }
            }
        }
        return new IRCMessage353(raw, tags, prefix.name(), prefix.user(), prefix.host(), safeGetIndex(params, 0),
                safeGetIndex(params, 1), safeGetIndex(params, 2), nicks, modes);
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
