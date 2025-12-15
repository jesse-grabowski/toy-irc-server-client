package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage253 extends IRCMessage {
  public static final String COMMAND = "253";
  private final String client;
  private final Integer connections;

  public IRCMessage253(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      Integer connections) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.connections = connections;
  }

  public String getClient() {
    return client;
  }

  public Integer getConnections() {
    return connections;
  }
}
