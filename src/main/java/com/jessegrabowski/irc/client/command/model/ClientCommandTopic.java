package com.jessegrabowski.irc.client.command.model;

public final class ClientCommandTopic implements ClientCommand {

  private String channel;
  private String topic;

  @Override
  public void validate() {}

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }
}
