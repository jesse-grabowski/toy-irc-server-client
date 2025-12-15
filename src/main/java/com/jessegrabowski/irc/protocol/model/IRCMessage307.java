package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage307 extends IRCMessage {
  public static final String COMMAND = "307";
  private final String client;
  private final String nick;

  public IRCMessage307(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }
}
