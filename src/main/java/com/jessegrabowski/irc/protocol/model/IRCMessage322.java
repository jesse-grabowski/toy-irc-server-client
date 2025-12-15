package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage322 extends IRCMessage {
  public static final String COMMAND = "322";
  private final String client;
  private final String channel;
  private final Integer clientCount;
  private final String topic;

  public IRCMessage322(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      Integer clientCount,
      String topic) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.clientCount = clientCount;
    this.topic = topic;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public Integer getClientCount() {
    return clientCount;
  }

  public String getTopic() {
    return topic;
  }
}
