package com.jessegrabowski.irc.client.command.model;

import java.util.List;

public final class ClientCommandJoin implements ClientCommand {
  private List<String> channels;
  private List<String> keys;
  private boolean noSwitch;

  @Override
  public void validate() {}

  public List<String> getChannels() {
    return channels;
  }

  public void setChannels(List<String> channels) {
    this.channels = channels;
  }

  public List<String> getKeys() {
    return keys;
  }

  public void setKeys(List<String> keys) {
    this.keys = keys;
  }

  public boolean isNoSwitch() {
    return noSwitch;
  }

  public void setNoSwitch(boolean noSwitch) {
    this.noSwitch = noSwitch;
  }
}
