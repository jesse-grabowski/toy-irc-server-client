package com.jessegrabowski.irc.protocol.model;

import java.util.List;
import java.util.SequencedMap;

public final class IRCMessage319 extends IRCMessage {
  public static final String COMMAND = "319";
  private final String client;
  private final String nick;
  private final List<String> channels;
  private final List<Character> prefixes;

  public IRCMessage319(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      List<String> channels,
      List<Character> prefixes) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.channels = channels;
    this.prefixes = prefixes;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public List<String> getChannels() {
    return channels;
  }

  public List<Character> getPrefixes() {
    return prefixes;
  }
}
