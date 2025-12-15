package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage255 extends IRCMessage {
  public static final String COMMAND = "255";
  private final String client;
  private final String message;

  public IRCMessage255(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String message) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.message = message;
  }

  public String getClient() {
    return client;
  }

  public String getMessage() {
    return message;
  }
}
