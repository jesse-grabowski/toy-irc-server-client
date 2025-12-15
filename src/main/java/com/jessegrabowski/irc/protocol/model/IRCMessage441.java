package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage441 extends IRCMessage {
  public static final String COMMAND = "441";
  private final String client;
  private final String nick;
  private final String channel;

  public IRCMessage441(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      String channel) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.channel = channel;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public String getChannel() {
    return channel;
  }
}
