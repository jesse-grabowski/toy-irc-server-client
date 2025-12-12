import java.util.SequencedMap;

public final class IRCMessage332 extends IRCMessage {
  public static final String COMMAND = "332";
  private final String client;
  private final String channel;
  private final String topic;

  public IRCMessage332(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      String topic) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.topic = topic;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public String getTopic() {
    return topic;
  }
}
