import java.util.SequencedMap;

public final class IRCMessage329 extends IRCMessage {
  public static final String COMMAND = "329";
  private final String client;
  private final String channel;
  private final long creationTime;

  public IRCMessage329(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      long creationTime) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.creationTime = creationTime;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public long getCreationTime() {
    return creationTime;
  }
}
