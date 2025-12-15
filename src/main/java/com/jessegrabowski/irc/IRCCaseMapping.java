package com.jessegrabowski.irc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public enum IRCCaseMapping {
  ASCII("ascii", IRCCaseMapping::casefoldAsciiNickname, IRCCaseMapping::casefoldAsciiChannel),
  RFC1459(
      "rfc1459", IRCCaseMapping::casefoldRfc1459Nickname, IRCCaseMapping::casefoldRfc1459Channel),
  RFC1459_STRICT(
      "rfc1459-strict",
      IRCCaseMapping::casefoldRfc1459StrictNickname,
      IRCCaseMapping::casefoldRfc1459StrictChannel);

  private static final Map<String, IRCCaseMapping> CASE_MAPPINGS = new HashMap<>();

  static {
    for (IRCCaseMapping caseMapping : IRCCaseMapping.values()) {
      CASE_MAPPINGS.put(caseMapping.name, caseMapping);
    }
  }

  private final String name;
  private final UnaryOperator<String> nicknameNormalizer;
  private final UnaryOperator<String> channelNormalizer;

  IRCCaseMapping(
      String name,
      UnaryOperator<String> nicknameNormalizer,
      UnaryOperator<String> channelNormalizer) {
    this.name = name;
    this.nicknameNormalizer = nicknameNormalizer;
    this.channelNormalizer = channelNormalizer;
  }

  public static Optional<IRCCaseMapping> forName(String name) {
    if (name == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(CASE_MAPPINGS.get(name));
  }

  public String normalizeNickname(String string) {
    if (string == null) {
      return null;
    }
    return nicknameNormalizer.apply(string);
  }

  public String normalizeChannel(String string) {
    if (string == null) {
      return null;
    }
    return channelNormalizer.apply(string);
  }

  private static final char[] ASCII_CASEFOLD = initializeCasefold(128, Map.of());
  private static final char[] RFC1459_CASEFOLD =
      initializeCasefold(
          128,
          Map.ofEntries(
              Map.entry('[', '{'), Map.entry(']', '}'), Map.entry('\\', '|'), Map.entry('~', '^')));
  private static final char[] RFC1459_STRICT_CASEFOLD =
      initializeCasefold(
          128, Map.ofEntries(Map.entry('[', '{'), Map.entry(']', '}'), Map.entry('\\', '|')));

  private static char[] initializeCasefold(int size, Map<Character, Character> replacements) {
    char[] chars = new char[size];
    for (int i = 0; i < size; i++) {
      if (i >= 'A' && i <= 'Z') {
        chars[i] = (char) (i + 32);
      } else {
        chars[i] = replacements.getOrDefault((char) i, (char) i);
      }
    }
    return chars;
  }

  private static String casefoldAsciiNickname(String nickname) {
    return casefoldNickname(ASCII_CASEFOLD, nickname);
  }

  private static String casefoldAsciiChannel(String channel) {
    return casefoldChannel(ASCII_CASEFOLD, channel);
  }

  private static String casefoldRfc1459Nickname(String nickname) {
    return casefoldNickname(RFC1459_CASEFOLD, nickname);
  }

  private static String casefoldRfc1459Channel(String channel) {
    return casefoldChannel(RFC1459_CASEFOLD, channel);
  }

  private static String casefoldRfc1459StrictNickname(String nickname) {
    return casefoldNickname(RFC1459_STRICT_CASEFOLD, nickname);
  }

  private static String casefoldRfc1459StrictChannel(String channel) {
    return casefoldChannel(RFC1459_STRICT_CASEFOLD, channel);
  }

  private static String casefoldNickname(char[] dictionary, String nickname) {
    if (nickname == null) {
      return null;
    }
    char[] chars = nickname.toCharArray();
    StringBuilder stringBuilder = new StringBuilder(chars.length);
    casefold(dictionary, stringBuilder, chars, 0, chars.length);
    return stringBuilder.toString();
  }

  private static String casefoldChannel(char[] dictionary, String channel) {
    if (channel == null) {
      return null;
    }
    if (channel.length() <= 1) {
      return channel;
    }
    char[] chars = channel.toCharArray();
    StringBuilder stringBuilder = new StringBuilder(chars.length);
    stringBuilder.append(chars[0]);
    casefold(dictionary, stringBuilder, chars, 1, chars.length);
    return stringBuilder.toString();
  }

  private static StringBuilder casefold(
      char[] dictionary, StringBuilder sb, char[] s, int startInclusive, int endExclusive) {
    for (int i = startInclusive; i < endExclusive; i++) {
      char c = s[i];
      if (c >= dictionary.length) {
        sb.append(c);
      } else {
        sb.append(dictionary[c]);
      }
    }
    return sb;
  }
}
