package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage010 extends IRCMessage {
  public static final String COMMAND = "010";
  private final String client;
  private final String hostname;
  private final Integer port;
  private final String info;

  public IRCMessage010(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String hostname,
      Integer port,
      String info) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.hostname = hostname;
    this.port = port;
    this.info = info;
  }

  public String getClient() {
    return client;
  }

  public String getHostname() {
    return hostname;
  }

  public Integer getPort() {
    return port;
  }

  public String getInfo() {
    return info;
  }
}
