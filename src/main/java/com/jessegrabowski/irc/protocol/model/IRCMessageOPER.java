package com.jessegrabowski.irc.protocol.model;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageOPER extends IRCMessage {

  public static final String COMMAND = "OPER";

  private final String name;
  private final String password;

  public IRCMessageOPER(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String name,
      String password) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.name = name;
    this.password = password;
  }

  public IRCMessageOPER(String name, String password) {
    this(null, new LinkedHashMap<>(), null, null, null, name, password);
  }

  public String getName() {
    return name;
  }

  public String getPassword() {
    return password;
  }
}
