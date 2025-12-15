package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;
import java.util.Set;

public final class IRCMessageParseError extends IRCMessage {

  private final String error;
  private final Set<String> invalidParameters;

  public IRCMessageParseError(
      String command,
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String error,
      Set<String> invalidParameters) {
    super(command, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.error = error;
    this.invalidParameters = invalidParameters;
  }

  public String getError() {
    return error;
  }

  public Set<String> getInvalidParameters() {
    return invalidParameters;
  }
}
