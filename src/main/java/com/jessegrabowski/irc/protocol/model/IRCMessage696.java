package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage696 extends IRCMessage {
  public static final String COMMAND = "696";
  private final String client;
  private final String target;
  private final Character mode;
  private final String parameter;
  private final String description;

  public IRCMessage696(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String target,
      Character mode,
      String parameter,
      String description) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.target = target;
    this.mode = mode;
    this.parameter = parameter;
    this.description = description;
  }

  public String getClient() {
    return client;
  }

  public String getTarget() {
    return target;
  }

  public Character getMode() {
    return mode;
  }

  public String getParameter() {
    return parameter;
  }

  public String getDescription() {
    return description;
  }
}
