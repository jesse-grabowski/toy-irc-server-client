package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage333 extends IRCMessage {
  public static final String COMMAND = "333";
  private final String client;
  private final String channel;
  private final String setBy;
  private final long setAt;

  public IRCMessage333(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      String setBy,
      long setAt) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.setBy = setBy;
    this.setAt = setAt;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public String getSetBy() {
    return setBy;
  }

  public long getSetAt() {
    return setAt;
  }
}
