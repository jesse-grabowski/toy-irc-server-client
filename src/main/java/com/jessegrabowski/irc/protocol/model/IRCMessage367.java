package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage367 extends IRCMessage {
  public static final String COMMAND = "367";
  private final String client;
  private final String channel;
  private final String mask;
  private final String setBy;
  private final Long setAt;

  public IRCMessage367(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      String mask,
      String setBy,
      Long setAt) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.mask = mask;
    this.setBy = setBy;
    this.setAt = setAt;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public String getMask() {
    return mask;
  }

  public String getSetBy() {
    return setBy;
  }

  public Long getSetAt() {
    return setAt;
  }
}
