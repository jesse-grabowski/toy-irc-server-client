package com.jessegrabowski.irc.protocol.model;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageQUIT extends IRCMessage {

  public static final String COMMAND = "QUIT";

  private final String reason;

  public IRCMessageQUIT(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String reason) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.reason = reason;
  }

  public IRCMessageQUIT(String reason) {
    this(null, new LinkedHashMap<>(), null, null, null, reason);
  }

  public String getReason() {
    return reason;
  }
}
