import static java.util.function.Predicate.not;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IRCMessageUnmarshaller {

  private static final Logger LOG = Logger.getLogger(IRCMessageUnmarshaller.class.getName());

  // This is really hard to read, but it's just splitting out the tags/prefix/command/params parts
  // as defined
  // in the modern IRC grammar (but not processing individual tags/params yet)
  private static final Pattern MESSAGE_PATTERN =
      Pattern.compile(
          "^(?:@(?<tags>[^\\s\\u0000]+)\\s+)?(?::(?<prefix>[^\\s\\u0000]+)\\s+)?(?<command>(?:[A-Za-z]+)|(?:\\d{3}))(?:\\s+(?<params>.+))?\\s*$");

  private static final Pattern PREFIX_PATTERN =
      Pattern.compile(
          "^(?<name>[^\\s\\u0000!@]+)(?:!(?<user>[^\\s\\u0000@]+))?(?:@(?<host>[^\\s\\u0000]+))?$");

  private static final Set<String> CAP_KNOWN_SUBCOMMANDS =
      Set.of("LS", "LIST", "REQ", "ACK", "NAK", "END", "NEW", "DEL");

  public IRCMessage unmarshal(String message) {
    Matcher matcher = MESSAGE_PATTERN.matcher(message);
    if (!matcher.matches()) {
      return new IRCMessageUnsupported(null, message, new LinkedHashMap<>(), null, null, null);
    }

    SequencedMap<String, String> tags = safeParseMap(matcher.group("tags"), ";", this::unescapeTag);
    PrefixParts prefix = parsePrefix(matcher.group("prefix"));
    String command = matcher.group("command").toUpperCase(Locale.ROOT);
    List<String> params = parseParams(matcher.group("params"));
    Parameters parameters = new Parameters(message, tags, prefix, params);

    try {
      return switch (command) {
        case IRCMessageCAPLS.COMMAND -> parseCap(parameters);
        case IRCMessageERROR.COMMAND -> parseError(parameters);
        case IRCMessageJOINNormal.COMMAND -> parseJoin(parameters);
        case IRCMessageKICK.COMMAND -> parseKick(parameters);
        case IRCMessageMODE.COMMAND -> parseMode(parameters);
        case IRCMessageNICK.COMMAND -> parseNick(parameters);
        case IRCMessageNOTICE.COMMAND -> parseNotice(parameters);
        case IRCMessagePART.COMMAND -> parsePart(parameters);
        case IRCMessagePASS.COMMAND -> parsePass(parameters);
        case IRCMessagePING.COMMAND -> parsePing(parameters);
        case IRCMessagePONG.COMMAND -> parsePong(parameters);
        case IRCMessagePRIVMSG.COMMAND -> parsePrivmsg(parameters);
        case IRCMessageUSER.COMMAND -> parseUser(parameters);
        case IRCMessageQUIT.COMMAND -> parseQuit(parameters);
        case IRCMessage001.COMMAND -> parse001(parameters);
        case IRCMessage002.COMMAND -> parse002(parameters);
        case IRCMessage003.COMMAND -> parse003(parameters);
        case IRCMessage004.COMMAND -> parse004(parameters);
        case IRCMessage005.COMMAND -> parse005(parameters);
        case IRCMessage010.COMMAND -> parse010(parameters);
        case IRCMessage212.COMMAND -> parse212(parameters);
        case IRCMessage219.COMMAND -> parse219(parameters);
        case IRCMessage221.COMMAND -> parse221(parameters);
        case IRCMessage242.COMMAND -> parse242(parameters);
        case IRCMessage251.COMMAND -> parse251(parameters);
        case IRCMessage252.COMMAND -> parse252(parameters);
        case IRCMessage253.COMMAND -> parse253(parameters);
        case IRCMessage254.COMMAND -> parse254(parameters);
        case IRCMessage255.COMMAND -> parse255(parameters);
        case IRCMessage256.COMMAND -> parse256(parameters);
        case IRCMessage257.COMMAND -> parse257(parameters);
        case IRCMessage258.COMMAND -> parse258(parameters);
        case IRCMessage259.COMMAND -> parse259(parameters);
        case IRCMessage263.COMMAND -> parse263(parameters);
        case IRCMessage265.COMMAND -> parse265(parameters);
        case IRCMessage266.COMMAND -> parse266(parameters);
        case IRCMessage276.COMMAND -> parse276(parameters);
        case IRCMessage301.COMMAND -> parse301(parameters);
        case IRCMessage302.COMMAND -> parse302(parameters);
        case IRCMessage305.COMMAND -> parse305(parameters);
        case IRCMessage306.COMMAND -> parse306(parameters);
        case IRCMessage307.COMMAND -> parse307(parameters);
        case IRCMessage311.COMMAND -> parse311(parameters);
        case IRCMessage312.COMMAND -> parse312(parameters);
        case IRCMessage313.COMMAND -> parse313(parameters);
        case IRCMessage314.COMMAND -> parse314(parameters);
        case IRCMessage315.COMMAND -> parse315(parameters);
        case IRCMessage317.COMMAND -> parse317(parameters);
        case IRCMessage318.COMMAND -> parse318(parameters);
        case IRCMessage319.COMMAND -> parse319(parameters);
        case IRCMessage320.COMMAND -> parse320(parameters);
        case IRCMessage321.COMMAND -> parse321(parameters);
        case IRCMessage322.COMMAND -> parse322(parameters);
        case IRCMessage323.COMMAND -> parse323(parameters);
        case IRCMessage324.COMMAND -> parse324(parameters);
        case IRCMessage329.COMMAND -> parse329(parameters);
        case IRCMessage330.COMMAND -> parse330(parameters);
        case IRCMessage331.COMMAND -> parse331(parameters);
        case IRCMessage332.COMMAND -> parse332(parameters);
        case IRCMessage333.COMMAND -> parse333(parameters);
        case IRCMessage336.COMMAND -> parse336(parameters);
        case IRCMessage337.COMMAND -> parse337(parameters);
        case IRCMessage338.COMMAND -> parse338(parameters);
        case IRCMessage341.COMMAND -> parse341(parameters);
        case IRCMessage346.COMMAND -> parse346(parameters);
        case IRCMessage347.COMMAND -> parse347(parameters);
        case IRCMessage348.COMMAND -> parse348(parameters);
        case IRCMessage349.COMMAND -> parse349(parameters);
        case IRCMessage351.COMMAND -> parse351(parameters);
        case IRCMessage352.COMMAND -> parse352(parameters);
        case IRCMessage353.COMMAND -> parse353(parameters);
        case IRCMessage364.COMMAND -> parse364(parameters);
        case IRCMessage365.COMMAND -> parse365(parameters);
        case IRCMessage366.COMMAND -> parse366(parameters);
        case IRCMessage367.COMMAND -> parse367(parameters);
        case IRCMessage368.COMMAND -> parse368(parameters);
        case IRCMessage369.COMMAND -> parse369(parameters);
        case IRCMessage371.COMMAND -> parse371(parameters);
        case IRCMessage372.COMMAND -> parse372(parameters);
        case IRCMessage374.COMMAND -> parse374(parameters);
        case IRCMessage375.COMMAND -> parse375(parameters);
        case IRCMessage376.COMMAND -> parse376(parameters);
        case IRCMessage378.COMMAND -> parse378(parameters);
        case IRCMessage379.COMMAND -> parse379(parameters);
        case IRCMessage381.COMMAND -> parse381(parameters);
        case IRCMessage382.COMMAND -> parse382(parameters);
        case IRCMessage391.COMMAND -> parse391(parameters);
        case IRCMessage400.COMMAND -> parse400(parameters);
        case IRCMessage401.COMMAND -> parse401(parameters);
        case IRCMessage402.COMMAND -> parse402(parameters);
        case IRCMessage403.COMMAND -> parse403(parameters);
        case IRCMessage404.COMMAND -> parse404(parameters);
        case IRCMessage405.COMMAND -> parse405(parameters);
        case IRCMessage406.COMMAND -> parse406(parameters);
        case IRCMessage409.COMMAND -> parse409(parameters);
        case IRCMessage411.COMMAND -> parse411(parameters);
        case IRCMessage412.COMMAND -> parse412(parameters);
        case IRCMessage417.COMMAND -> parse417(parameters);
        case IRCMessage421.COMMAND -> parse421(parameters);
        case IRCMessage422.COMMAND -> parse422(parameters);
        case IRCMessage431.COMMAND -> parse431(parameters);
        case IRCMessage432.COMMAND -> parse432(parameters);
        case IRCMessage433.COMMAND -> parse433(parameters);
        case IRCMessage436.COMMAND -> parse436(parameters);
        case IRCMessage441.COMMAND -> parse441(parameters);
        case IRCMessage442.COMMAND -> parse442(parameters);
        case IRCMessage443.COMMAND -> parse443(parameters);
        case IRCMessage451.COMMAND -> parse451(parameters);
        case IRCMessage461.COMMAND -> parse461(parameters);
        case IRCMessage462.COMMAND -> parse462(parameters);
        case IRCMessage464.COMMAND -> parse464(parameters);
        case IRCMessage465.COMMAND -> parse465(parameters);
        case IRCMessage471.COMMAND -> parse471(parameters);
        case IRCMessage472.COMMAND -> parse472(parameters);
        case IRCMessage473.COMMAND -> parse473(parameters);
        case IRCMessage474.COMMAND -> parse474(parameters);
        case IRCMessage475.COMMAND -> parse475(parameters);
        case IRCMessage476.COMMAND -> parse476(parameters);
        case IRCMessage481.COMMAND -> parse481(parameters);
        case IRCMessage482.COMMAND -> parse482(parameters);
        case IRCMessage483.COMMAND -> parse483(parameters);
        case IRCMessage491.COMMAND -> parse491(parameters);
        case IRCMessage501.COMMAND -> parse501(parameters);
        case IRCMessage502.COMMAND -> parse502(parameters);
        case IRCMessage524.COMMAND -> parse524(parameters);
        case IRCMessage525.COMMAND -> parse525(parameters);
        case IRCMessage670.COMMAND -> parse670(parameters);
        case IRCMessage671.COMMAND -> parse671(parameters);
        case IRCMessage691.COMMAND -> parse691(parameters);
        case IRCMessage696.COMMAND -> parse696(parameters);
        case IRCMessage704.COMMAND -> parse704(parameters);
        case IRCMessage705.COMMAND -> parse705(parameters);
        case IRCMessage706.COMMAND -> parse706(parameters);
        case IRCMessage723.COMMAND -> parse723(parameters);
        default ->
            new IRCMessageUnsupported(
                command, message, tags, prefix.name(), prefix.user(), prefix.host());
      };
    } catch (Exception e) {
      return new IRCMessageParseError(
          command, message, tags, prefix.name(), prefix.user(), prefix.host(), e.getMessage());
    }
  }

  private String unescapeTag(String tag) {
    StringBuilder result = new StringBuilder();
    boolean escaped = false;
    for (char c : tag.toCharArray()) {
      if (escaped) {
        escaped = false;
        result.append(
            switch (c) {
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

  private IRCMessage parseCap(Parameters parameters) throws Exception {
    return parameters
        .injectConditionally()
        .ifIndexEquals(0, "END", p -> p.inject(IRCMessageCAPEND::new))
        .ifIndexEquals(
            0,
            "LS",
            p ->
                p.inject(
                    none(String.class),
                    required("version"),
                    constant(false),
                    constant(new LinkedHashMap<String, String>()),
                    IRCMessageCAPLS::new))
        .ifIndexEquals(
            0,
            "LIST",
            p ->
                p.inject(
                    none(String.class),
                    constant(false),
                    constant(List.<String>of()),
                    IRCMessageCAPLIST::new))
        .ifIndexEquals(
            0,
            "REQ",
            p -> {
              SplittingParameterInjector<List<String>, List<String>> splitter =
                  splitRequired("extension", this::splitEnabledDisabledCaps);
              return p.inject(
                  splitter.left(List.of()), splitter.right(List.of()), IRCMessageCAPREQ::new);
            })
        .ifIndexEquals(
            1,
            "ACK",
            p -> {
              SplittingParameterInjector<List<String>, List<String>> splitter =
                  splitRequired("extension", this::splitEnabledDisabledCaps);
              return p.inject(
                  required("nick"),
                  splitter.left(List.of()),
                  splitter.right(List.of()),
                  IRCMessageCAPACK::new);
            })
        .ifIndexEquals(
            1,
            "DEL",
            p ->
                p.inject(
                    required("nick"),
                    required("extension", splitToListDelimited("\\s+")),
                    IRCMessageCAPDEL::new))
        .ifIndexEquals(
            1,
            "LS",
            p ->
                p.inject(
                    required("nick"),
                    none(String.class),
                    optional("*", s -> true, false),
                    greedyRequiredMap("extensions", this::splitToEntry),
                    IRCMessageCAPLS::new))
        .ifIndexEquals(
            1,
            "LIST",
            p ->
                p.inject(
                    required("nick"),
                    optional("*", s -> true, false),
                    required("extension", splitToListDelimited("\\s+")),
                    IRCMessageCAPLIST::new))
        .ifIndexEquals(
            1,
            "NAK",
            p -> {
              SplittingParameterInjector<List<String>, List<String>> splitter =
                  splitRequired("extension", this::splitEnabledDisabledCaps);
              return p.inject(
                  required("nick"),
                  splitter.left(List.of()),
                  splitter.right(List.of()),
                  IRCMessageCAPNAK::new);
            })
        .ifIndexEquals(
            1,
            "NEW",
            p ->
                p.inject(
                    required("nick"),
                    greedyRequiredMap("extensions", this::splitToEntry),
                    IRCMessageCAPNEW::new))
        .inject();
  }

  private Pair<List<String>, List<String>> splitEnabledDisabledCaps(String caps) {
    List<String> enabledCapabilities = new ArrayList<>();
    List<String> disabledCapabilities = new ArrayList<>();
    for (String cap : caps.split("\\s+")) {
      if (cap.startsWith("-")) {
        disabledCapabilities.add(cap.substring(1));
      } else {
        enabledCapabilities.add(cap);
      }
    }
    return new Pair<>(enabledCapabilities, disabledCapabilities);
  }

  private IRCMessageERROR parseError(Parameters parameters) throws Exception {
    return parameters.inject(required("reason"), IRCMessageERROR::new);
  }

  private IRCMessage parseJoin(Parameters parameters) throws Exception {
    return parameters
        .injectConditionally()
        .ifIndexEquals(0, "0", p -> p.inject(IRCMessageJOIN0::new))
        .ifNoneMatch(
            p ->
                p.inject(
                    required("channel", this::splitToList),
                    optional("key", this::splitToList, List.<String>of()),
                    IRCMessageJOINNormal::new))
        .inject();
  }

  private IRCMessageKICK parseKick(Parameters parameters) throws Exception {
    return parameters.inject(
        required("channel"), required("nick"), optional("comment"), IRCMessageKICK::new);
  }

  private IRCMessageMODE parseMode(Parameters parameters) throws Exception {
    return parameters.inject(
        required("target"),
        optional("modestring"),
        greedyOptional("mode arguments"),
        IRCMessageMODE::new);
  }

  private IRCMessageNICK parseNick(Parameters parameters) throws Exception {
    return parameters.inject(required("nickname"), IRCMessageNICK::new);
  }

  private IRCMessageNOTICE parseNotice(Parameters parameters) throws Exception {
    return parameters.inject(
        required("target", this::splitToList), required("text to be sent"), IRCMessageNOTICE::new);
  }

  private IRCMessagePART parsePart(Parameters parameters) throws Exception {
    return parameters.inject(
        required("channel", this::splitToList), optional("reason"), IRCMessagePART::new);
  }

  private IRCMessagePASS parsePass(Parameters parameters) throws Exception {
    return parameters.inject(required("password"), IRCMessagePASS::new);
  }

  private IRCMessagePING parsePing(Parameters parameters) throws Exception {
    return parameters.inject(required("token"), IRCMessagePING::new);
  }

  private IRCMessagePONG parsePong(Parameters parameters) throws Exception {
    return parameters.inject(optional("server"), required("token"), IRCMessagePONG::new);
  }

  private IRCMessagePRIVMSG parsePrivmsg(Parameters parameters) throws Exception {
    return parameters.inject(
        required("target", this::splitToList), required("text to be sent"), IRCMessagePRIVMSG::new);
  }

  private IRCMessageUSER parseUser(Parameters parameters) throws Exception {
    return parameters
        .consume(1, 2)
        .inject(required("username"), required("realname"), IRCMessageUSER::new);
  }

  private IRCMessageQUIT parseQuit(Parameters parameters) throws Exception {
    return parameters.inject(optional("reason"), IRCMessageQUIT::new);
  }

  private IRCMessage001 parse001(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("message"), IRCMessage001::new);
  }

  private IRCMessage002 parse002(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("message"), IRCMessage002::new);
  }

  private IRCMessage003 parse003(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("message"), IRCMessage003::new);
  }

  private IRCMessage004 parse004(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("servername"),
        required("version"),
        required("available user modes"),
        required("available channel modes"),
        optional("channel modes with a parameter"),
        IRCMessage004::new);
  }

  private IRCMessage005 parse005(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        greedyRequiredMap("tokens", this::splitToEntry),
        required("text"),
        discardLast(IRCMessage005::new));
  }

  private IRCMessage010 parse010(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("hostname"),
        required("port", Integer::parseInt),
        required("info"),
        IRCMessage010::new);
  }

  private IRCMessage212 parse212(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("command"),
        required("count", Integer::parseInt),
        optional("byte count", Integer::parseInt),
        optional("remote count", Integer::parseInt),
        IRCMessage212::new);
  }

  private IRCMessage219 parse219(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("stats letter"), IRCMessage219::new);
  }

  private IRCMessage221 parse221(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("user modes"), IRCMessage221::new);
  }

  private IRCMessage242 parse242(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("message"), IRCMessage242::new);
  }

  private IRCMessage251 parse251(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("message"), IRCMessage251::new);
  }

  private IRCMessage252 parse252(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("ops", Integer::parseInt), IRCMessage252::new);
  }

  private IRCMessage253 parse253(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("connections", Integer::parseInt), IRCMessage253::new);
  }

  private IRCMessage254 parse254(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channels", Integer::parseInt), IRCMessage254::new);
  }

  private IRCMessage255 parse255(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("message"), IRCMessage255::new);
  }

  private IRCMessage256 parse256(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), optional("server"), required("text"), discardLast(IRCMessage256::new));
  }

  private IRCMessage257 parse257(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("info"), IRCMessage257::new);
  }

  private IRCMessage258 parse258(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("info"), IRCMessage258::new);
  }

  private IRCMessage259 parse259(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("info"), IRCMessage259::new);
  }

  private IRCMessage263 parse263(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("command"), required("text"), IRCMessage263::new);
  }

  private IRCMessage265 parse265(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        optional("u", Integer::parseInt),
        optional("m", Integer::parseInt),
        required("text"),
        IRCMessage265::new);
  }

  private IRCMessage266 parse266(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        optional("u", Integer::parseInt),
        optional("m", Integer::parseInt),
        required("text"),
        IRCMessage266::new);
  }

  private IRCMessage276 parse276(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nick"), required("text"), IRCMessage276::new);
  }

  private IRCMessage301 parse301(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nick"), required("message"), IRCMessage301::new);
  }

  private IRCMessage302 parse302(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("reply", splitToListDelimited("\\s+")), IRCMessage302::new);
  }

  private IRCMessage305 parse305(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage305::new);
  }

  private IRCMessage306 parse306(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage306::new);
  }

  private IRCMessage307 parse307(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("nick"), IRCMessage307::new);
  }

  private IRCMessage311 parse311(Parameters parameters) throws Exception {
    return parameters
        .consume(4)
        .inject(
            required("client"),
            required("nick"),
            required("username"),
            required("host"),
            required("realname"),
            IRCMessage311::new);
  }

  private IRCMessage312 parse312(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("nick"),
        required("server"),
        required("server info"),
        IRCMessage312::new);
  }

  private IRCMessage313 parse313(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nick"), required("text"), IRCMessage313::new);
  }

  private IRCMessage314 parse314(Parameters parameters) throws Exception {
    return parameters
        .consume(4)
        .inject(
            required("client"),
            required("nick"),
            required("username"),
            required("host"),
            required("realname"),
            IRCMessage314::new);
  }

  private IRCMessage315 parse315(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("mask"), IRCMessage315::new);
  }

  private IRCMessage317 parse317(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("nick"),
        required("secs"),
        required("signon", Long::parseLong),
        IRCMessage317::new);
  }

  private IRCMessage318 parse318(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("nick"), IRCMessage318::new);
  }

  private IRCMessage319 parse319(Parameters parameters) throws Exception {
    SplittingParameterInjector<List<String>, List<Character>> splitter =
        splitRequired("[prefix]nick", splitForPrefixes(parameters.getChannelMembershipPrefixes()));
    return parameters.inject(
        required("client"),
        required("nick"),
        splitter.left(List.of()),
        splitter.right(List.of()),
        IRCMessage319::new);
  }

  private IRCMessage320 parse320(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nick"), required("text"), IRCMessage320::new);
  }

  private IRCMessage321 parse321(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage321::new);
  }

  private IRCMessage322 parse322(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("channel"),
        required("client count", Integer::parseInt),
        required("topic"),
        IRCMessage322::new);
  }

  private IRCMessage323 parse323(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage323::new);
  }

  private IRCMessage324 parse324(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("channel"),
        required("modestring"),
        greedyOptional("mode arguments"),
        IRCMessage324::new);
  }

  private IRCMessage329 parse329(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("channel"),
        required("creationtime", Long::parseLong),
        IRCMessage329::new);
  }

  private IRCMessage330 parse330(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nick"), required("account"), IRCMessage330::new);
  }

  private IRCMessage331 parse331(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("channel"), IRCMessage331::new);
  }

  private IRCMessage332 parse332(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("topic"), IRCMessage332::new);
  }

  private IRCMessage333 parse333(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("channel"),
        required("nick"),
        required("setat", Long::parseLong),
        IRCMessage333::new);
  }

  private IRCMessage336 parse336(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("channel"), IRCMessage336::new);
  }

  private IRCMessage337 parse337(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage337::new);
  }

  private IRCMessage parse338(Parameters parameters) throws Exception {
    return parameters
        .injectConditionally(false)
        .ifIndex(
            2,
            s -> s.contains("@"),
            p -> {
              SplittingParameterInjector<String, String> splitter =
                  splitString("host@hostname", "@");
              return p.inject(
                  required("client"),
                  required("nick"),
                  splitter.left(null),
                  splitter.right(null),
                  required("ip"),
                  required("text"),
                  discardLast(IRCMessage338::new));
            })
        .ifIndex(
            2,
            this::isIP,
            p ->
                p.inject(
                    required("client"),
                    required("nick"),
                    none(String.class),
                    none(String.class),
                    required("ip"),
                    required("text"),
                    discardLast(IRCMessage338::new)))
        .ifIndex(
            2,
            not(this::isIP),
            p ->
                p.inject(
                    required("client"),
                    required("nick"),
                    none(String.class),
                    required("host"),
                    none(String.class),
                    required("text"),
                    discardLast(IRCMessage338::new)))
        .ifNoneMatch(
            p ->
                p.inject(
                    required("client"),
                    required("nick"),
                    none(String.class),
                    none(String.class),
                    none(String.class),
                    required("text"),
                    discardLast(IRCMessage338::new)))
        .inject();
  }

  private IRCMessage341 parse341(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nick"), required("channel"), IRCMessage341::new);
  }

  private IRCMessage346 parse346(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("mask"), IRCMessage346::new);
  }

  private IRCMessage347 parse347(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("channel"), IRCMessage347::new);
  }

  private IRCMessage348 parse348(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("mask"), IRCMessage348::new);
  }

  private IRCMessage349 parse349(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("channel"), IRCMessage349::new);
  }

  private IRCMessage351 parse351(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("version"),
        required("server"),
        required("comments"),
        IRCMessage351::new);
  }

  private IRCMessage352 parse352(Parameters parameters) throws Exception {
    SplittingParameterInjector<String, String> splitter =
        splitString("<hopcount> <realname>", "\\s+");
    return parameters.inject(
        required("client"),
        required("channel"),
        required("username"),
        required("host"),
        required("server"),
        required("nick"),
        required("flags"),
        splitter.left(null),
        splitter.right(null),
        IRCMessage352::new);
  }

  private IRCMessage353 parse353(Parameters parameters) throws Exception {
    SplittingParameterInjector<List<String>, List<Character>> splitter =
        splitRequired("[prefix]nick", splitForPrefixes(parameters.getChannelMembershipPrefixes()));
    return parameters.inject(
        required("client"),
        required("symbol"),
        required("channel"),
        splitter.left(List.of()),
        splitter.right(List.of()),
        IRCMessage353::new);
  }

  private IRCMessage364 parse364(Parameters parameters) throws Exception {
    SplittingParameterInjector<String, String> splitter =
        splitString("<hopcount> <server info>", "\\s+");
    return parameters.inject(
        required("client"),
        required("server1"),
        required("server2"),
        splitter.left(null),
        splitter.right(null),
        IRCMessage364::new);
  }

  private IRCMessage365 parse365(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage365::new);
  }

  private IRCMessage366 parse366(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("channel"), IRCMessage366::new);
  }

  private IRCMessage367 parse367(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("channel"),
        required("mask"),
        optional("who"),
        optional("set-ts", Long::parseLong),
        IRCMessage367::new);
  }

  private IRCMessage368 parse368(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("channel"), IRCMessage368::new);
  }

  private IRCMessage369 parse369(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("nick"), IRCMessage369::new);
  }

  private IRCMessage371 parse371(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("string"), IRCMessage371::new);
  }

  private IRCMessage372 parse372(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("string"), IRCMessage372::new);
  }

  private IRCMessage374 parse374(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage374::new);
  }

  private IRCMessage375 parse375(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("string"), IRCMessage375::new);
  }

  private IRCMessage376 parse376(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage376::new);
  }

  private IRCMessage378 parse378(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nick"), required("text"), IRCMessage378::new);
  }

  private IRCMessage379 parse379(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nick"), required("modes"), IRCMessage379::new);
  }

  private IRCMessage381 parse381(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage381::new);
  }

  private IRCMessage382 parse382(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("config file"), IRCMessage382::new);
  }

  private IRCMessage391 parse391(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("server"),
        optional("timestamp", Long::parseLong),
        optional("ts offset"),
        required("human-readable time"),
        IRCMessage391::new);
  }

  private IRCMessage400 parse400(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), greedyRequired("command"), required("info"), IRCMessage400::new);
  }

  private IRCMessage401 parse401(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nickname"), required("text"), IRCMessage401::new);
  }

  private IRCMessage402 parse402(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("server name"), required("text"), IRCMessage402::new);
  }

  private IRCMessage403 parse403(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage403::new);
  }

  private IRCMessage404 parse404(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage404::new);
  }

  private IRCMessage405 parse405(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage405::new);
  }

  private IRCMessage406 parse406(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nickname"), required("text"), IRCMessage406::new);
  }

  private IRCMessage409 parse409(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage409::new);
  }

  private IRCMessage411 parse411(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage411::new);
  }

  private IRCMessage412 parse412(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage412::new);
  }

  private IRCMessage417 parse417(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage417::new);
  }

  private IRCMessage421 parse421(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("command"), IRCMessage421::new);
  }

  private IRCMessage422 parse422(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage422::new);
  }

  private IRCMessage431 parse431(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage431::new);
  }

  private IRCMessage432 parse432(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("nickname"), IRCMessage432::new);
  }

  private IRCMessage433 parse433(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("nickname"), IRCMessage433::new);
  }

  private IRCMessage436 parse436(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nickname"), required("text"), IRCMessage436::new);
  }

  private IRCMessage441 parse441(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nickname"), required("channel"), IRCMessage441::new);
  }

  private IRCMessage442 parse442(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("channel"), IRCMessage442::new);
  }

  private IRCMessage443 parse443(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nickname"), required("channel"), IRCMessage443::new);
  }

  private IRCMessage451 parse451(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage451::new);
  }

  private IRCMessage461 parse461(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("command"), required("text"), IRCMessage461::new);
  }

  private IRCMessage462 parse462(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage462::new);
  }

  private IRCMessage464 parse464(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), IRCMessage464::new);
  }

  private IRCMessage465 parse465(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("reason"), IRCMessage465::new);
  }

  private IRCMessage471 parse471(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage471::new);
  }

  private IRCMessage472 parse472(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("modechar", s -> s.charAt(0)),
        required("text"),
        IRCMessage472::new);
  }

  private IRCMessage473 parse473(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage473::new);
  }

  private IRCMessage474 parse474(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage474::new);
  }

  private IRCMessage475 parse475(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage475::new);
  }

  private IRCMessage476 parse476(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage476::new);
  }

  private IRCMessage481 parse481(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage481::new);
  }

  private IRCMessage482 parse482(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("channel"), required("text"), IRCMessage482::new);
  }

  private IRCMessage483 parse483(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage483::new);
  }

  private IRCMessage491 parse491(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage491::new);
  }

  private IRCMessage501 parse501(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage501::new);
  }

  private IRCMessage502 parse502(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage502::new);
  }

  private IRCMessage524 parse524(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("subject"), required("text"), IRCMessage524::new);
  }

  private IRCMessage525 parse525(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("target chan"), required("text"), IRCMessage525::new);
  }

  private IRCMessage670 parse670(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage670::new);
  }

  private IRCMessage671 parse671(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("nickname"), required("text"), IRCMessage671::new);
  }

  private IRCMessage691 parse691(Parameters parameters) throws Exception {
    return parameters.inject(required("client"), required("text"), IRCMessage691::new);
  }

  private IRCMessage696 parse696(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"),
        required("target chan/user"),
        required("mode char", s -> s.charAt(0)),
        required("parameter"),
        required("description"),
        IRCMessage696::new);
  }

  private IRCMessage704 parse704(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("subject"), required("text"), IRCMessage704::new);
  }

  private IRCMessage705 parse705(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("subject"), required("text"), IRCMessage705::new);
  }

  private IRCMessage706 parse706(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("subject"), required("text"), IRCMessage706::new);
  }

  private IRCMessage723 parse723(Parameters parameters) throws Exception {
    return parameters.inject(
        required("client"), required("priv"), required("text"), IRCMessage723::new);
  }

  // BEYOND THIS POINT IS SCARY PARSING CODE TO ENABLE MY NICE DSL

  private boolean isIP(String value) {
    try {
      InetAddress.ofLiteral(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private List<String> splitToList(String raw) {
    return Arrays.asList(raw.split(","));
  }

  private UnsafeFunction<String, List<String>> splitToListDelimited(String regex) {
    return raw -> Arrays.asList(raw.split(regex));
  }

  private Map.Entry<String, String> splitToEntry(String raw) {
    String[] parts = raw.split("=", 2);
    if (parts.length == 1) {
      return Map.entry(parts[0], "");
    } else {
      return Map.entry(parts[0], parts[1]);
    }
  }

  private UnsafeFunction2<String, List<String>, List<Character>> splitForPrefixes(
      Set<Character> validPrefixes) {
    return raw -> {
      List<String> values = new ArrayList<>();
      List<Character> prefixes = new ArrayList<>();
      if (raw != null) {
        String[] splitRaw = raw.split("\\s+", -1);
        for (String part : splitRaw) {
          if (part.isBlank()) {
            continue;
          }

          if (validPrefixes.contains(part.charAt(0))) {
            prefixes.add(part.charAt(0));
            values.add(part.substring(1));
          } else {
            prefixes.add(null);
            values.add(part);
          }
        }
      }
      return new Pair<>(values, prefixes);
    };
  }

  private <T> SequencedMap<String, T> safeParseMap(
      String param, String delimiter, Function<String, T> mapper) {
    SequencedMap<String, T> map = new LinkedHashMap<>();
    if (param == null || param.isEmpty()) {
      return map;
    }

    String[] entries = param.split(delimiter);
    for (String entry : entries) {
      String[] kv = entry.split(delimiter, 2);
      if (kv.length == 1) {
        map.put(kv[0], mapper.apply(""));
      } else {
        map.put(kv[0], mapper.apply(kv[1]));
      }
    }
    return map;
  }

  private record PrefixParts(String name, String user, String host) {
    PrefixParts() {
      this(null, null, null);
    }
  }

  private static class Parameters {

    private final Set<Character> channelMembershipPrefixes = Set.of('~', '&', '@', '%', '+');

    private final Set<String> errorParameters = new HashSet<>();

    private final String raw;
    private final SequencedMap<String, String> tags;
    private final PrefixParts prefix;
    private final List<String> params;

    public Parameters(
        String raw, SequencedMap<String, String> tags, PrefixParts prefix, List<String> params) {
      this.raw = raw;
      this.tags = tags;
      this.prefix = prefix;
      this.params = params;
    }

    public Set<Character> getChannelMembershipPrefixes() {
      return channelMembershipPrefixes;
    }

    public Parameters consume(int... indexes) throws NotEnoughParametersException {
      List<String> filteredParameters = new ArrayList<>(params);
      List<Integer> removalIndexes =
          Arrays.stream(indexes).distinct().boxed().sorted(Comparator.reverseOrder()).toList();
      for (int i : removalIndexes) {
        if (i >= filteredParameters.size()) {
          throw new NotEnoughParametersException(
              "expected at least %d parameters".formatted(i + 1));
        }
        filteredParameters.remove(i);
      }
      return new Parameters(raw, tags, prefix, filteredParameters);
    }

    public ConditionalInjectionBuilder injectConditionally() {
      return injectConditionally(true);
    }

    public ConditionalInjectionBuilder injectConditionally(boolean consume) {
      return new ConditionalInjectionBuilder(this, consume);
    }

    public <T extends IRCMessage> T inject(IRCMessageFactory0<T> constructor) {
      return constructor.create(raw, tags, prefix.name(), prefix.user(), prefix.host());
    }

    public <T extends IRCMessage, A> T inject(
        ParameterExtractor<A> arg0, IRCMessageFactory1<T, A> constructor) throws Exception {
      ParameterPlanner planner = new ParameterPlanner(params.size(), arg0);
      return validate(
          constructor.create(
              raw, tags, prefix.name(), prefix.user(), prefix.host(), planner.get(0, this, arg0)));
    }

    public <T extends IRCMessage, A, B> T inject(
        ParameterExtractor<A> arg0,
        ParameterExtractor<B> arg1,
        IRCMessageFactory2<T, A, B> constructor)
        throws Exception {
      ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1);
      return validate(
          constructor.create(
              raw,
              tags,
              prefix.name(),
              prefix.user(),
              prefix.host(),
              planner.get(0, this, arg0),
              planner.get(1, this, arg1)));
    }

    public <T extends IRCMessage, A, B, C> T inject(
        ParameterExtractor<A> arg0,
        ParameterExtractor<B> arg1,
        ParameterExtractor<C> arg2,
        IRCMessageFactory3<T, A, B, C> constructor)
        throws Exception {
      ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1, arg2);
      return validate(
          constructor.create(
              raw,
              tags,
              prefix.name(),
              prefix.user(),
              prefix.host(),
              planner.get(0, this, arg0),
              planner.get(1, this, arg1),
              planner.get(2, this, arg2)));
    }

    public <T extends IRCMessage, A, B, C, D> T inject(
        ParameterExtractor<A> arg0,
        ParameterExtractor<B> arg1,
        ParameterExtractor<C> arg2,
        ParameterExtractor<D> arg3,
        IRCMessageFactory4<T, A, B, C, D> constructor)
        throws Exception {
      ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3);
      return validate(
          constructor.create(
              raw,
              tags,
              prefix.name(),
              prefix.user(),
              prefix.host(),
              planner.get(0, this, arg0),
              planner.get(1, this, arg1),
              planner.get(2, this, arg2),
              planner.get(3, this, arg3)));
    }

    public <T extends IRCMessage, A, B, C, D, E> T inject(
        ParameterExtractor<A> arg0,
        ParameterExtractor<B> arg1,
        ParameterExtractor<C> arg2,
        ParameterExtractor<D> arg3,
        ParameterExtractor<E> arg4,
        IRCMessageFactory5<T, A, B, C, D, E> constructor)
        throws Exception {
      ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4);
      return validate(
          constructor.create(
              raw,
              tags,
              prefix.name(),
              prefix.user(),
              prefix.host(),
              planner.get(0, this, arg0),
              planner.get(1, this, arg1),
              planner.get(2, this, arg2),
              planner.get(3, this, arg3),
              planner.get(4, this, arg4)));
    }

    public <T extends IRCMessage, A, B, C, D, E, F> T inject(
        ParameterExtractor<A> arg0,
        ParameterExtractor<B> arg1,
        ParameterExtractor<C> arg2,
        ParameterExtractor<D> arg3,
        ParameterExtractor<E> arg4,
        ParameterExtractor<F> arg5,
        IRCMessageFactory6<T, A, B, C, D, E, F> constructor)
        throws Exception {
      ParameterPlanner planner =
          new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4, arg5);
      return validate(
          constructor.create(
              raw,
              tags,
              prefix.name(),
              prefix.user(),
              prefix.host(),
              planner.get(0, this, arg0),
              planner.get(1, this, arg1),
              planner.get(2, this, arg2),
              planner.get(3, this, arg3),
              planner.get(4, this, arg4),
              planner.get(5, this, arg5)));
    }

    public <T extends IRCMessage, A, B, C, D, E, F, G> T inject(
        ParameterExtractor<A> arg0,
        ParameterExtractor<B> arg1,
        ParameterExtractor<C> arg2,
        ParameterExtractor<D> arg3,
        ParameterExtractor<E> arg4,
        ParameterExtractor<F> arg5,
        ParameterExtractor<G> arg6,
        IRCMessageFactory7<T, A, B, C, D, E, F, G> constructor)
        throws Exception {

      ParameterPlanner planner =
          new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4, arg5, arg6);

      return validate(
          constructor.create(
              raw,
              tags,
              prefix.name(),
              prefix.user(),
              prefix.host(),
              planner.get(0, this, arg0),
              planner.get(1, this, arg1),
              planner.get(2, this, arg2),
              planner.get(3, this, arg3),
              planner.get(4, this, arg4),
              planner.get(5, this, arg5),
              planner.get(6, this, arg6)));
    }

    public <T extends IRCMessage, A, B, C, D, E, F, G, H> T inject(
        ParameterExtractor<A> arg0,
        ParameterExtractor<B> arg1,
        ParameterExtractor<C> arg2,
        ParameterExtractor<D> arg3,
        ParameterExtractor<E> arg4,
        ParameterExtractor<F> arg5,
        ParameterExtractor<G> arg6,
        ParameterExtractor<H> arg7,
        IRCMessageFactory8<T, A, B, C, D, E, F, G, H> constructor)
        throws Exception {

      ParameterPlanner planner =
          new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);

      return validate(
          constructor.create(
              raw,
              tags,
              prefix.name(),
              prefix.user(),
              prefix.host(),
              planner.get(0, this, arg0),
              planner.get(1, this, arg1),
              planner.get(2, this, arg2),
              planner.get(3, this, arg3),
              planner.get(4, this, arg4),
              planner.get(5, this, arg5),
              planner.get(6, this, arg6),
              planner.get(7, this, arg7)));
    }

    public <T extends IRCMessage, A, B, C, D, E, F, G, H, I> T inject(
        ParameterExtractor<A> arg0,
        ParameterExtractor<B> arg1,
        ParameterExtractor<C> arg2,
        ParameterExtractor<D> arg3,
        ParameterExtractor<E> arg4,
        ParameterExtractor<F> arg5,
        ParameterExtractor<G> arg6,
        ParameterExtractor<H> arg7,
        ParameterExtractor<I> arg8,
        IRCMessageFactory9<T, A, B, C, D, E, F, G, H, I> constructor)
        throws Exception {

      ParameterPlanner planner =
          new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);

      return validate(
          constructor.create(
              raw,
              tags,
              prefix.name(),
              prefix.user(),
              prefix.host(),
              planner.get(0, this, arg0),
              planner.get(1, this, arg1),
              planner.get(2, this, arg2),
              planner.get(3, this, arg3),
              planner.get(4, this, arg4),
              planner.get(5, this, arg5),
              planner.get(6, this, arg6),
              planner.get(7, this, arg7),
              planner.get(8, this, arg8)));
    }

    private <T> T validate(T value) throws Exception {
      if (!errorParameters.isEmpty()) {
        throw new ParserErrorException(
            "Error parsing parameters %s"
                .formatted(errorParameters.stream().sorted().collect(Collectors.joining(", "))));
      }
      return value;
    }
  }

  private record ConditionalInjection(
      int index, Predicate<String> predicate, UnsafeFunction<Parameters, IRCMessage> injection) {}

  private static class ConditionalInjectionBuilder {

    private final Parameters parameters;
    private final List<ConditionalInjection> injections;
    private final boolean consumeTestParameter;

    private UnsafeFunction<Parameters, IRCMessage> defaultInjection;

    public ConditionalInjectionBuilder(Parameters parameters, boolean consumeTestParameter) {
      this.parameters = parameters;
      this.injections = new ArrayList<>();
      this.consumeTestParameter = consumeTestParameter;
    }

    public ConditionalInjectionBuilder ifIndexEquals(
        int index, String value, UnsafeFunction<Parameters, IRCMessage> injection) {
      injections.add(new ConditionalInjection(index, value::equals, injection));
      return this;
    }

    public ConditionalInjectionBuilder ifIndex(
        int index, Predicate<String> predicate, UnsafeFunction<Parameters, IRCMessage> injection) {
      injections.add(new ConditionalInjection(index, predicate, injection));
      return this;
    }

    public ConditionalInjectionBuilder ifNoneMatch(
        UnsafeFunction<Parameters, IRCMessage> injection) {
      defaultInjection = injection;
      return this;
    }

    public IRCMessage inject() throws Exception {
      for (ConditionalInjection injection : injections) {
        if (parameters.params.size() > injection.index()
            && injection.predicate().test(parameters.params.get(injection.index()))) {
          if (consumeTestParameter) {
            List<String> newParams = new ArrayList<>(parameters.params);
            newParams.remove(injection.index());
            return injection
                .injection()
                .apply(
                    new Parameters(parameters.raw, parameters.tags, parameters.prefix, newParams));
          } else {
            return injection.injection().apply(parameters);
          }
        }
      }
      if (defaultInjection != null) {
        return defaultInjection.apply(parameters);
      }
      throw new ParserErrorException("could not determine appropriate subcommand");
    }
  }

  private sealed interface ParameterPlan permits RangeParameterPlan, DefaultParameterPlan {}

  private record RangeParameterPlan(int start, int end) implements ParameterPlan {}

  private record DefaultParameterPlan() implements ParameterPlan {}

  private static class NotEnoughParametersException extends Exception {
    public NotEnoughParametersException(String message) {
      super(message);
    }
  }

  private static class ParserErrorException extends Exception {
    public ParserErrorException(String message) {
      super(message);
    }
  }

  private static class ParameterPlanner {

    private final ParameterPlan[] plans;
    private final ParameterExtractor<?>[] extractors;

    public ParameterPlanner(int paramCount, ParameterExtractor<?>... extractors)
        throws NotEnoughParametersException {
      this.extractors = extractors;
      this.plans = new ParameterPlan[extractors.length];

      int remaining = paramCount;
      int[] stakes = new int[extractors.length];
      for (int i = 0; i < extractors.length; i++) {
        ParameterExtractor<?> extractor = extractors[i];
        stakes[i] += extractor.consumeAtLeast();
        remaining -= extractor.consumeAtLeast();
      }
      for (int i = 0; i < extractors.length && remaining > 0; i++) {
        ParameterExtractor<?> extractor = extractors[i];
        int extra = Math.min(remaining, extractor.consumeAtMost() - extractor.consumeAtLeast());
        stakes[i] += extra;
        remaining -= extra;
      }
      if (remaining < 0) {
        throw new NotEnoughParametersException(
            "expected at least %d parameters".formatted(paramCount - remaining));
      }

      int position = 0;
      for (int i = 0; i < plans.length; i++) {
        if (stakes[i] == 0) {
          plans[i] = new DefaultParameterPlan();
        } else {
          plans[i] = new RangeParameterPlan(position, position + stakes[i] - 1);
          position += stakes[i];
        }
      }
    }

    public <T> T get(int index, Parameters parameters, ParameterExtractor<T> extractor) {
      if (index < 0 || index >= plans.length) {
        throw new IndexOutOfBoundsException(index);
      }
      if (extractor != extractors[index]) {
        throw new IllegalArgumentException("Extractor must match value passed in constructor");
      }
      ParameterPlan plan = plans[index];
      return switch (plan) {
        case DefaultParameterPlan unused -> extractor.getDefaultValue();
        case RangeParameterPlan p -> extractor.extract(p.start(), p.end(), parameters);
      };
    }
  }

  private <T> ParameterExtractor<T> none(Class<T> unused) {
    return new NoneParameterInjector<T>();
  }

  private <T> ParameterExtractor<T> constant(T value) {
    return new ConstantParameterInjector<>(value);
  }

  private ParameterExtractor<String> required(String name) {
    return new SingleParameterExtractor<>(x -> x, true, null, name);
  }

  private <T> ParameterExtractor<T> required(String name, UnsafeFunction<String, T> mapper) {
    return new SingleParameterExtractor<>(mapper, true, null, name);
  }

  private ParameterExtractor<String> optional(String name) {
    return new SingleParameterExtractor<>(x -> x, false, null, name);
  }

  private <T> ParameterExtractor<T> optional(String name, UnsafeFunction<String, T> mapper) {
    return new SingleParameterExtractor<>(mapper, false, null, name);
  }

  private <T> ParameterExtractor<T> optional(
      String name, UnsafeFunction<String, T> mapper, T defaultValue) {
    return new SingleParameterExtractor<>(mapper, false, defaultValue, name);
  }

  private ParameterExtractor<List<String>> greedyRequired(String name) {
    return new MultipleParameterExtractor<List<String>, String, List<String>>(
        ArrayList::new, x -> x, List::add, x -> x, 1, Integer.MAX_VALUE, List.of(), name);
  }

  private ParameterExtractor<List<String>> greedyOptional(String name) {
    return new MultipleParameterExtractor<List<String>, String, List<String>>(
        ArrayList::new, x -> x, List::add, x -> x, 0, Integer.MAX_VALUE, List.of(), name);
  }

  private <K, V> ParameterExtractor<SequencedMap<K, V>> greedyRequiredMap(
      String name, UnsafeFunction<String, Map.Entry<K, V>> mapper) {
    return new MultipleParameterExtractor<
        List<Map.Entry<K, V>>, Map.Entry<K, V>, SequencedMap<K, V>>(
        ArrayList::new,
        mapper,
        List::add,
        entries ->
            entries.stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)),
        1,
        Integer.MAX_VALUE,
        new LinkedHashMap<>(),
        name);
  }

  private <L, R> SplittingParameterInjector<L, R> splitRequired(
      String name, UnsafeFunction2<String, L, R> splitter) {
    return new SplittingParameterInjector<>(true, splitter, name);
  }

  private SplittingParameterInjector<String, String> splitString(String name, String regex) {
    return new SplittingParameterInjector<>(
        true,
        s -> {
          String[] parts = s.split(regex, 2);
          if (parts.length == 2) {
            return new Pair<>(parts[0], parts[1]);
          } else {
            return new Pair<>(parts[0], null);
          }
        },
        name);
  }

  interface ParameterExtractor<T> {
    T extract(int start, int endInclusive, Parameters parameters);

    int consumeAtLeast();

    int consumeAtMost();

    T getDefaultValue();

    String name();
  }

  private static class NoneParameterInjector<T> implements ParameterExtractor<T> {

    @Override
    public T extract(int start, int endInclusive, Parameters parameters) {
      return null;
    }

    @Override
    public int consumeAtLeast() {
      return 0;
    }

    @Override
    public int consumeAtMost() {
      return 0;
    }

    @Override
    public T getDefaultValue() {
      return null;
    }

    @Override
    public String name() {
      return "";
    }
  }

  private static class ConstantParameterInjector<T> implements ParameterExtractor<T> {

    private final T value;

    public ConstantParameterInjector(T value) {
      this.value = value;
    }

    @Override
    public T extract(int start, int endInclusive, Parameters parameters) {
      return value;
    }

    @Override
    public int consumeAtLeast() {
      return 0;
    }

    @Override
    public int consumeAtMost() {
      return 0;
    }

    @Override
    public T getDefaultValue() {
      return value;
    }

    @Override
    public String name() {
      return "";
    }
  }

  private static class SingleParameterExtractor<T> implements ParameterExtractor<T> {

    private final UnsafeFunction<String, T> mapper;
    private final boolean required;
    private final T defaultValue;
    private final String name;

    public SingleParameterExtractor(
        UnsafeFunction<String, T> mapper, boolean required, T defaultValue, String name) {
      this.mapper = mapper;
      this.required = required;
      this.defaultValue = defaultValue;
      this.name = name;
    }

    @Override
    public T extract(int start, int endInclusive, Parameters parameters) {
      String rawValue = null;
      try {
        rawValue = parameters.params.get(start);
        return mapper.apply(rawValue);
      } catch (Exception e) {
        if (LOG.isLoggable(Level.FINEST)) {
          LogRecord record =
              new LogRecord(Level.FINEST, "Error parsing parameter at index {0} with value {1}");
          record.setParameters(new Object[] {start, rawValue});
          record.setThrown(e);
          LOG.log(record);
        }
        parameters.errorParameters.add(name);
        return defaultValue;
      }
    }

    @Override
    public int consumeAtLeast() {
      return required ? 1 : 0;
    }

    @Override
    public int consumeAtMost() {
      return 1;
    }

    @Override
    public T getDefaultValue() {
      return defaultValue;
    }

    @Override
    public String name() {
      return name;
    }
  }

  private static class MultipleParameterExtractor<C extends Collection<T>, T, R>
      implements ParameterExtractor<R> {

    private final Supplier<C> factory;
    private final UnsafeFunction<String, T> mapper;
    private final BiConsumer<C, T> accumulator;
    private final UnsafeFunction<C, R> finisher;
    private final int consumeAtLeast;
    private final int consumeAtMost;
    private final R defaultValue;
    private final String name;

    public MultipleParameterExtractor(
        Supplier<C> factory,
        UnsafeFunction<String, T> mapper,
        BiConsumer<C, T> accumulator,
        UnsafeFunction<C, R> finisher,
        int consumeAtLeast,
        int consumeAtMost,
        R defaultValue,
        String name) {
      this.factory = factory;
      this.mapper = mapper;
      this.accumulator = accumulator;
      this.finisher = finisher;
      this.consumeAtLeast = consumeAtLeast;
      this.consumeAtMost = consumeAtMost;
      this.defaultValue = defaultValue;
      this.name = name;
    }

    @Override
    public R extract(int start, int endInclusive, Parameters parameters) {
      C collection;
      try {
        collection = factory.get();
      } catch (Exception e) {
        if (LOG.isLoggable(Level.FINEST)) {
          LogRecord record =
              new LogRecord(Level.FINEST, "Error initializing collection over {0}..{1}: {2}");
          record.setParameters(new Object[] {start, endInclusive, parameters.raw});
          record.setThrown(e);
          LOG.log(record);
        }
        parameters.errorParameters.add(name);
        return defaultValue;
      }
      for (int i = start; i <= endInclusive; i++) {
        String rawValue = null;
        try {
          rawValue = parameters.params.get(i);
          T mappedValue = mapper.apply(rawValue);
          accumulator.accept(collection, mappedValue);
        } catch (Exception e) {
          if (LOG.isLoggable(Level.FINEST)) {
            LogRecord record =
                new LogRecord(
                    Level.FINEST, "Error parsing parameter at index {0} with value {1}: {2}");
            record.setParameters(new Object[] {start, rawValue, parameters.raw});
            record.setThrown(e);
            LOG.log(record);
          }
          parameters.errorParameters.add(name);
        }
      }
      try {
        return finisher.apply(collection);
      } catch (Exception e) {
        if (LOG.isLoggable(Level.FINEST)) {
          LogRecord record =
              new LogRecord(Level.FINEST, "Error finishing collection over {0}..{1}: {2}");
          record.setParameters(new Object[] {start, endInclusive, parameters.raw});
          record.setThrown(e);
          LOG.log(record);
        }
        parameters.errorParameters.add(name);
        return defaultValue;
      }
    }

    @Override
    public int consumeAtLeast() {
      return consumeAtLeast;
    }

    @Override
    public int consumeAtMost() {
      return consumeAtMost;
    }

    @Override
    public R getDefaultValue() {
      return defaultValue;
    }

    @Override
    public String name() {
      return name;
    }
  }

  private static class SplittingParameterInjector<L, R> implements ParameterExtractor<Void> {

    private final boolean required;
    private final UnsafeFunction2<String, L, R> splitter;
    private final String name;

    private Pair<L, R> results = null;

    public SplittingParameterInjector(
        boolean required, UnsafeFunction2<String, L, R> splitter, String name) {
      this.required = required;
      this.splitter = splitter;
      this.name = name;
    }

    @Override
    public Void extract(int start, int endInclusive, Parameters parameters) {
      String rawValue = null;
      try {
        rawValue = parameters.params.get(start);
        results = splitter.apply(rawValue);
        return null;
      } catch (Exception e) {
        if (LOG.isLoggable(Level.FINEST)) {
          LogRecord record =
              new LogRecord(Level.FINEST, "Error parsing parameter at index {0} with value {1}");
          record.setParameters(new Object[] {start, rawValue});
          record.setThrown(e);
          LOG.log(record);
        }
        parameters.errorParameters.add(name);
        return null;
      }
    }

    @Override
    public int consumeAtLeast() {
      return required ? 1 : 0;
    }

    @Override
    public int consumeAtMost() {
      return 1;
    }

    @Override
    public Void getDefaultValue() {
      return null;
    }

    @Override
    public String name() {
      return name;
    }

    public ParameterExtractor<L> left(L defaultValue) {
      return new LeftPart(defaultValue);
    }

    public ParameterExtractor<R> right(R defaultValue) {
      return new RightPart(defaultValue);
    }

    private class LeftPart implements ParameterExtractor<L> {

      private final L defaultValue;

      public LeftPart(L defaultValue) {
        this.defaultValue = defaultValue;
      }

      @Override
      public L extract(int start, int endInclusive, Parameters parameters) {
        if (results == null) {
          SplittingParameterInjector.this.extract(start, endInclusive, parameters);
        }
        return results == null ? defaultValue : results.left();
      }

      @Override
      public int consumeAtLeast() {
        return required ? 1 : 0;
      }

      @Override
      public int consumeAtMost() {
        return 1;
      }

      @Override
      public L getDefaultValue() {
        return results == null ? defaultValue : results.left();
      }

      @Override
      public String name() {
        return name;
      }
    }

    private class RightPart implements ParameterExtractor<R> {

      private final R defaultValue;

      public RightPart(R defaultValue) {
        this.defaultValue = defaultValue;
      }

      @Override
      public R extract(int start, int endInclusive, Parameters parameters) {
        if (results == null) {
          SplittingParameterInjector.this.extract(start, endInclusive, parameters);
        }
        return results == null ? defaultValue : results.right();
      }

      @Override
      public int consumeAtLeast() {
        return 0;
      }

      @Override
      public int consumeAtMost() {
        return 0;
      }

      @Override
      public R getDefaultValue() {
        return results == null ? defaultValue : results.right();
      }

      @Override
      public String name() {
        return name;
      }
    }
  }

  private <T extends IRCMessage, A> IRCMessageFactory1<T, A> discardFirst(
      IRCMessageFactory0<T> constructor) {
    return (rawMessage, tags, name, user, host, arg0) ->
        constructor.create(rawMessage, tags, name, user, host);
  }

  private <T extends IRCMessage, A> IRCMessageFactory1<T, A> discardLast(
      IRCMessageFactory0<T> constructor) {
    return (rawMessage, tags, name, user, host, arg0) ->
        constructor.create(rawMessage, tags, name, user, host);
  }

  private <T extends IRCMessage, A, B> IRCMessageFactory2<T, A, B> discardFirst(
      IRCMessageFactory1<T, B> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1) ->
        constructor.create(rawMessage, tags, name, user, host, arg1);
  }

  private <T extends IRCMessage, A, B> IRCMessageFactory2<T, A, B> discardLast(
      IRCMessageFactory1<T, A> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1) ->
        constructor.create(rawMessage, tags, name, user, host, arg0);
  }

  private <T extends IRCMessage, A, B, C> IRCMessageFactory3<T, A, B, C> discardFirst(
      IRCMessageFactory2<T, B, C> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1, arg2) ->
        constructor.create(rawMessage, tags, name, user, host, arg1, arg2);
  }

  private <T extends IRCMessage, A, B, C> IRCMessageFactory3<T, A, B, C> discardLast(
      IRCMessageFactory2<T, A, B> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1, arg2) ->
        constructor.create(rawMessage, tags, name, user, host, arg0, arg1);
  }

  private <T extends IRCMessage, A, B, C, D> IRCMessageFactory4<T, A, B, C, D> discardFirst(
      IRCMessageFactory3<T, B, C, D> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3) ->
        constructor.create(rawMessage, tags, name, user, host, arg1, arg2, arg3);
  }

  private <T extends IRCMessage, A, B, C, D> IRCMessageFactory4<T, A, B, C, D> discardLast(
      IRCMessageFactory3<T, A, B, C> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3) ->
        constructor.create(rawMessage, tags, name, user, host, arg0, arg1, arg2);
  }

  private <T extends IRCMessage, A, B, C, D, E> IRCMessageFactory5<T, A, B, C, D, E> discardFirst(
      IRCMessageFactory4<T, B, C, D, E> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4) ->
        constructor.create(rawMessage, tags, name, user, host, arg1, arg2, arg3, arg4);
  }

  private <T extends IRCMessage, A, B, C, D, E> IRCMessageFactory5<T, A, B, C, D, E> discardLast(
      IRCMessageFactory4<T, A, B, C, D> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4) ->
        constructor.create(rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3);
  }

  private <T extends IRCMessage, A, B, C, D, E, F>
      IRCMessageFactory6<T, A, B, C, D, E, F> discardFirst(
          IRCMessageFactory5<T, B, C, D, E, F> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5) ->
        constructor.create(rawMessage, tags, name, user, host, arg1, arg2, arg3, arg4, arg5);
  }

  private <T extends IRCMessage, A, B, C, D, E, F>
      IRCMessageFactory6<T, A, B, C, D, E, F> discardLast(
          IRCMessageFactory5<T, A, B, C, D, E> constructor) {
    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5) ->
        constructor.create(rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4);
  }

  private <T extends IRCMessage, A, B, C, D, E, F, G>
      IRCMessageFactory7<T, A, B, C, D, E, F, G> discardFirst(
          IRCMessageFactory6<T, B, C, D, E, F, G> constructor) {

    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5, arg6) ->
        constructor.create(rawMessage, tags, name, user, host, arg1, arg2, arg3, arg4, arg5, arg6);
  }

  private <T extends IRCMessage, A, B, C, D, E, F, G>
      IRCMessageFactory7<T, A, B, C, D, E, F, G> discardLast(
          IRCMessageFactory6<T, A, B, C, D, E, F> constructor) {

    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5, arg6) ->
        constructor.create(rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5);
  }

  private <T extends IRCMessage, A, B, C, D, E, F, G, H>
      IRCMessageFactory8<T, A, B, C, D, E, F, G, H> discardFirst(
          IRCMessageFactory7<T, B, C, D, E, F, G, H> constructor) {

    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7) ->
        constructor.create(
            rawMessage, tags, name, user, host, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
  }

  private <T extends IRCMessage, A, B, C, D, E, F, G, H>
      IRCMessageFactory8<T, A, B, C, D, E, F, G, H> discardLast(
          IRCMessageFactory7<T, A, B, C, D, E, F, G> constructor) {

    return (rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7) ->
        constructor.create(
            rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
  }

  private <T extends IRCMessage, A, B, C, D, E, F, G, H, I>
      IRCMessageFactory9<T, A, B, C, D, E, F, G, H, I> discardFirst(
          IRCMessageFactory8<T, B, C, D, E, F, G, H, I> constructor) {

    return (rawMessage,
        tags,
        name,
        user,
        host,
        arg0,
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8) ->
        constructor.create(
            rawMessage, tags, name, user, host, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
  }

  private <T extends IRCMessage, A, B, C, D, E, F, G, H, I>
      IRCMessageFactory9<T, A, B, C, D, E, F, G, H, I> discardLast(
          IRCMessageFactory8<T, A, B, C, D, E, F, G, H> constructor) {

    return (rawMessage,
        tags,
        name,
        user,
        host,
        arg0,
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8) ->
        constructor.create(
            rawMessage, tags, name, user, host, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
  }

  @FunctionalInterface
  interface UnsafeFunction<I, O> {
    O apply(I input) throws Exception;
  }

  record Pair<L, R>(L left, R right) {}

  @FunctionalInterface
  interface UnsafeFunction2<I, L, R> {
    Pair<L, R> apply(I input) throws Exception;
  }

  @FunctionalInterface
  interface IRCMessageFactory0<T extends IRCMessage> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost);
  }

  @FunctionalInterface
  interface IRCMessageFactory1<T extends IRCMessage, A> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0);
  }

  @FunctionalInterface
  interface IRCMessageFactory2<T extends IRCMessage, A, B> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0,
        B arg1);
  }

  @FunctionalInterface
  interface IRCMessageFactory3<T extends IRCMessage, A, B, C> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0,
        B arg1,
        C arg2);
  }

  @FunctionalInterface
  interface IRCMessageFactory4<T extends IRCMessage, A, B, C, D> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0,
        B arg1,
        C arg2,
        D arg3);
  }

  @FunctionalInterface
  interface IRCMessageFactory5<T extends IRCMessage, A, B, C, D, E> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0,
        B arg1,
        C arg2,
        D arg3,
        E arg4);
  }

  @FunctionalInterface
  interface IRCMessageFactory6<T extends IRCMessage, A, B, C, D, E, F> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0,
        B arg1,
        C arg2,
        D arg3,
        E arg4,
        F arg5);
  }

  @FunctionalInterface
  interface IRCMessageFactory7<T extends IRCMessage, A, B, C, D, E, F, G> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0,
        B arg1,
        C arg2,
        D arg3,
        E arg4,
        F arg5,
        G arg6);
  }

  @FunctionalInterface
  interface IRCMessageFactory8<T extends IRCMessage, A, B, C, D, E, F, G, H> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0,
        B arg1,
        C arg2,
        D arg3,
        E arg4,
        F arg5,
        G arg6,
        H arg7);
  }

  @FunctionalInterface
  interface IRCMessageFactory9<T extends IRCMessage, A, B, C, D, E, F, G, H, I> {
    T create(
        String rawMessage,
        SequencedMap<String, String> tags,
        String prefixName,
        String prefixUser,
        String prefixHost,
        A arg0,
        B arg1,
        C arg2,
        D arg3,
        E arg4,
        F arg5,
        G arg6,
        H arg7,
        I arg8);
  }
}
