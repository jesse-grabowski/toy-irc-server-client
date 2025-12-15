package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage465 extends IRCMessage {
  public static final String COMMAND = "465";
  private final String client;
  private final String text;

  public IRCMessage465(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getText() {
    return text;
  }
}
