package com.jessegrabowski.irc.protocol.model;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageUSER extends IRCMessage {

  public static final String COMMAND = "USER";

  private final String user;
  private final String realName;

  public IRCMessageUSER(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String user,
      String realName) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.user = user;
    this.realName = realName;
  }

  public IRCMessageUSER(String user, String realName) {
    this(null, new LinkedHashMap<>(), null, null, null, user, realName);
  }

  public String getUser() {
    return user;
  }

  public String getRealName() {
    return realName;
  }
}
