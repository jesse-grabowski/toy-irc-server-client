package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage337 extends IRCMessage {
  public static final String COMMAND = "337";
  private final String client;

  public IRCMessage337(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
  }

  public String getClient() {
    return client;
  }
}
