package com.jessegrabowski.irc.protocol.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class IRCMessagePART extends IRCMessage {

  public static final String COMMAND = "PART";

  private final List<String> channels;
  private final String reason;

  public IRCMessagePART(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      List<String> channels,
      String reason) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.channels = channels;
    this.reason = reason;
  }

  public IRCMessagePART(List<String> channels, String reason) {
    this(null, new LinkedHashMap<>(), null, null, null, channels, reason);
  }

  public List<String> getChannels() {
    return channels;
  }

  public String getReason() {
    return reason;
  }
}
