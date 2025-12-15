package com.jessegrabowski.irc.client.command.model;

public final class ClientCommandKick implements ClientCommand {

  private String channel;
  private String nick;
  private String reason;

  @Override
  public void validate() {}

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getNick() {
    return nick;
  }

  public void setNick(String nick) {
    this.nick = nick;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
