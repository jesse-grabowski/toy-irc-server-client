import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// did not expect this to be as painful as it was but here we are
public final class IRCParameterParser {

  private static final Logger LOG = Logger.getLogger(IRCParameterParser.class.getName());
  private static final Pattern PREFIX_PATTERN =
      Pattern.compile("^\\((?<modes>[a-zA-Z]+)\\)(?<prefixes>\\S+)$");

  private IRCParameterParser() {}

  public static void parse(String parameter, String value, IRCClientState.Parameters parameters) {
    boolean disable = parameter.startsWith("-");
    if (disable) {
      parameter = parameter.substring(1);
    }

    try {
      String finalParameter = parameter;
      switch (parameter) {
        case "AWAYLEN" -> parseOptionalInteger(disable, value, parameters::setAwayLength);
        case "CASEMAPPING" -> parseCaseMapping(disable, value, parameters);
        case "CHANLIMIT" -> parseChannelLimit(disable, value, parameters);
        case "CHANMODES" -> parseChannelModes(disable, value, parameters);
        case "CHANNELLEN" -> parseOptionalInteger(disable, value, parameters::setChannelLength);
        case "CHANTYPES" -> parseChannelTypes(disable, value, parameters);
        case "EXCEPTS" -> parseCharacterFlag(disable, value, 'e', parameters::setExcepts);
        case "EXTBAN" -> parseExtendedBanModes(disable, value, parameters);
        case "HOSTLEN" -> parseOptionalInteger(disable, value, parameters::setHostLength);
        case "INVEX" -> parseCharacterFlag(disable, value, 'I', parameters::setInviteExceptions);
        case "KICKLEN" -> parseOptionalInteger(disable, value, parameters::setKickLength);
        case "MAXLIST" -> parseMaxList(disable, value, parameters);
        case "MAXTARGETS" -> parseOptionalInteger(disable, value, parameters::setMaxTargets);
        case "MODES" -> parseOptionalInteger(disable, value, parameters::setModes);
        case "NETWORK" -> parseNetwork(disable, value, parameters);
        case "NICKLEN" -> parseOptionalInteger(disable, value, parameters::setNickLength);
        case "PREFIX" -> parsePrefix(disable, value, parameters);
        case "SAFELIST" -> parameters.setSafeList(!disable);
        case "SILENCE" -> parseOptionalInteger(disable, value, parameters::setSilence);
        case "STATUSMSG" -> parseStatusMessage(disable, value, parameters);
        case "TARGMAX" -> parseTargMax(disable, value, parameters);
        case "TOPICLEN" -> parseOptionalInteger(disable, value, parameters::setTopicLength);
        case "USERLEN" -> parseOptionalInteger(disable, value, parameters::setUserLength);
        default ->
            LOG.fine(() -> "Ignored unknown server parameter " + finalParameter + "=" + value);
      }
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to parse server parameter " + parameter + "=" + value, e);
    }
  }

  private static void parseOptionalInteger(
      boolean disable, String value, java.util.function.IntConsumer setter) {
    setter.accept(disable ? Integer.MAX_VALUE : Integer.parseInt(value));
  }

  private static void parseCaseMapping(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setCaseMapping(IRCCaseMapping.RFC1459);
    } else {
      parameters.setCaseMapping(IRCCaseMapping.forName(value).orElse(IRCCaseMapping.RFC1459));
    }
  }

  private static Map<Character, Integer> parseCharIntMap(String value) {
    String[] parts = value.split(",");
    Map<Character, Integer> map = new HashMap<>();
    for (String part : parts) {
      String[] kv = part.split(":", 2);
      String prefixes = kv[0];
      for (char c : prefixes.toCharArray()) {
        if (kv.length > 1) {
          map.put(c, Integer.parseInt(kv[1]));
        } else {
          map.put(c, Integer.MAX_VALUE);
        }
      }
    }
    return map;
  }

  private static void parseChannelLimit(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setChannelLimits(Map.of());
    } else {
      parameters.setChannelLimits(parseCharIntMap(value));
    }
  }

  private static Set<Character> toCharSet(String s) {
    Set<Character> set = new HashSet<>();
    for (char c : s.toCharArray()) {
      set.add(c);
    }
    return set;
  }

  private static void parseChannelModes(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    parameters.setTypeAChannelModes(Set.of());
    parameters.setTypeBChannelModes(Set.of());
    parameters.setTypeCChannelModes(Set.of());
    parameters.setTypeDChannelModes(Set.of());
    if (!disable) {
      String[] parts = value.split(",");
      if (parts.length > 0) {
        parameters.setTypeAChannelModes(toCharSet(parts[0]));
      }
      if (parts.length > 1) {
        parameters.setTypeBChannelModes(toCharSet(parts[1]));
      }
      if (parts.length > 2) {
        parameters.setTypeCChannelModes(toCharSet(parts[2]));
      }
      if (parts.length > 3) {
        parameters.setTypeDChannelModes(toCharSet(parts[3]));
      }
    }
  }

  private static void parseChannelTypes(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setChannelTypes(Set.of('#', '&'));
    } else {
      parameters.setChannelTypes(toCharSet(value));
    }
  }

  private static void parseCharacterFlag(
      boolean disable,
      String value,
      char defaultChar,
      java.util.function.Consumer<Character> setter) {
    if (disable) {
      setter.accept(null);
    } else {
      setter.accept(value == null || value.isEmpty() ? defaultChar : value.charAt(0));
    }
  }

  private static void parseExtendedBanModes(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setExtendedBanModes(Set.of());
      parameters.setExtendedBanPrefix(null);
    } else {
      String[] parts = value.split(",", 2);
      if (!parts[0].isEmpty()) {
        parameters.setExtendedBanPrefix(parts[0].charAt(0));
      }
      Set<Character> extendedBanTypes = toCharSet(parts[1]);
      parameters.setExtendedBanModes(extendedBanTypes);
    }
  }

  private static void parseMaxList(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setMaxList(Map.of());
    } else {
      parameters.setMaxList(parseCharIntMap(value));
    }
  }

  private static void parseNetwork(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setNetwork("");
    } else {
      parameters.setNetwork(value);
    }
  }

  private static void parsePrefix(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setPrefixes(new LinkedHashMap<>());
    } else {
      Matcher matcher = PREFIX_PATTERN.matcher(value);
      if (!matcher.matches()) {
        parameters.setPrefixes(new LinkedHashMap<>());
        return;
      }
      String modes = matcher.group("modes");
      String prefixes = matcher.group("prefixes");
      if (modes.length() != prefixes.length()) {
        throw new IllegalArgumentException("PREFIX modes and prefixes must be the same length");
      }
      SequencedMap<Character, Character> p = new LinkedHashMap<>();
      for (int i = 0; i < modes.length(); i++) {
        LOG.info("Parsed prefix " + modes.charAt(i) + " -> " + prefixes.charAt(i));
        p.put(modes.charAt(i), prefixes.charAt(i));
      }
      parameters.setPrefixes(p);
    }
  }

  private static void parseStatusMessage(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setStatusMessage(Set.of());
    } else {
      parameters.setStatusMessage(toCharSet(value));
    }
  }

  private static Map<String, Integer> parseStringIntMap(String value) {
    String[] parts = value.split(",");
    Map<String, Integer> map = new HashMap<>();
    for (String part : parts) {
      String[] kv = part.split(":", 2);
      if (kv.length > 1) {
        map.put(kv[0], Integer.parseInt(kv[1]));
      } else {
        map.put(kv[0], Integer.MAX_VALUE);
      }
    }
    return map;
  }

  private static void parseTargMax(
      boolean disable, String value, IRCClientState.Parameters parameters) {
    if (disable) {
      parameters.setTargetMax(Map.of());
    } else {
      if (value.isEmpty()) {
        parameters.setTargetMax(Map.of());
        return;
      }
      parameters.setTargetMax(parseStringIntMap(value));
    }
  }
}
