package com.jessegrabowski.irc.protocol.model;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageCAPLISTRequest extends IRCMessage {

  public static final String COMMAND = "CAP";

  public IRCMessageCAPLISTRequest(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
  }

  public IRCMessageCAPLISTRequest() {
    this(null, new LinkedHashMap<>(), null, null, null);
  }
}
