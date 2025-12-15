package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage266 extends IRCMessage {
  public static final String COMMAND = "266";
  private final String client;
  private final Integer globalUsers;
  private final Integer maxGlobalUsers;
  private final String text;

  public IRCMessage266(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      Integer globalUsers,
      Integer maxGlobalUsers,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.globalUsers = globalUsers;
    this.maxGlobalUsers = maxGlobalUsers;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public Integer getGlobalUsers() {
    return globalUsers;
  }

  public Integer getMaxGlobalUsers() {
    return maxGlobalUsers;
  }

  public String getText() {
    return text;
  }
}
