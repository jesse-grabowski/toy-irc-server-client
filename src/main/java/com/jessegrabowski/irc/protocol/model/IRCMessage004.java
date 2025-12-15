package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public final class IRCMessage004 extends IRCMessage {
  public static final String COMMAND = "004";
  private final String client;
  private final String serverName;
  private final String version;
  private final String userModes;
  private final String channelModes;
  private final String channelModesWithParam;

  public IRCMessage004(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String serverName,
      String version,
      String userModes,
      String channelModes,
      String channelModesWithParam) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.serverName = serverName;
    this.version = version;
    this.userModes = userModes;
    this.channelModes = channelModes;
    this.channelModesWithParam = channelModesWithParam;
  }

  public String getClient() {
    return client;
  }

  public String getServerName() {
    return serverName;
  }

  public String getVersion() {
    return version;
  }

  public String getUserModes() {
    return userModes;
  }

  public String getChannelModes() {
    return channelModes;
  }

  public String getChannelModesWithParam() {
    return channelModesWithParam;
  }
}
