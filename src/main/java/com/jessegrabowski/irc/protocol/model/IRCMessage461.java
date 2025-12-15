package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage461 extends IRCMessage {
  public static final String COMMAND = "461";
  private final String client;
  private final String invalidCommand;
  private final String text;

  public IRCMessage461(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String invalidCommand,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.invalidCommand = invalidCommand;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getInvalidCommand() {
    return invalidCommand;
  }

  public String getText() {
    return text;
  }
}
