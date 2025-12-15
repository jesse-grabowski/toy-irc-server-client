import java.util.SequencedMap;

public final class IRCMessage442 extends IRCMessage {
  public static final String COMMAND = "442";
  private final String client;
  private final String channel;

  public IRCMessage442(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }
}
