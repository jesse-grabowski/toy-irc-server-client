package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage317 extends IRCMessage {
  public static final String COMMAND = "317";
  private final String client;
  private final String nick;
  private final Integer secondsIdle;
  private final Long signOnTime;

  public IRCMessage317(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      Integer secondsIdle,
      Long signOnTime) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.secondsIdle = secondsIdle;
    this.signOnTime = signOnTime;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public Integer getSecondsIdle() {
    return secondsIdle;
  }

  public Long getSignOnTime() {
    return signOnTime;
  }
}
