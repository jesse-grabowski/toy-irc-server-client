package com.jessegrabowski.irc.client.command.model;

import java.util.List;

public final class ClientCommandPart implements ClientCommand {
  private List<String> channels;
  private String reason;

  @Override
  public void validate() {}

  public List<String> getChannels() {
    return channels;
  }

  public void setChannels(List<String> channels) {
    this.channels = channels;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
