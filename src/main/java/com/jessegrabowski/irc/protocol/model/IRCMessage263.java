package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage263 extends IRCMessage {
  public static final String COMMAND = "263";
  private final String client;
  private final String targetCommand;
  private final String text;

  public IRCMessage263(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String targetCommand,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.targetCommand = targetCommand;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getTargetCommand() {
    return targetCommand;
  }

  public String getText() {
    return text;
  }
}
