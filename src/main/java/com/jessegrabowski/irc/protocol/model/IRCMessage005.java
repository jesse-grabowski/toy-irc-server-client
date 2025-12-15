package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage005 extends IRCMessage {

  public static final String COMMAND = "005";

  private final String client;
  private final SequencedMap<String, String> parameters;
  private final String text;

  public IRCMessage005(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      SequencedMap<String, String> parameters,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.parameters = parameters;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public SequencedMap<String, String> getParameters() {
    return parameters;
  }

  public String getText() {
    return text;
  }
}
