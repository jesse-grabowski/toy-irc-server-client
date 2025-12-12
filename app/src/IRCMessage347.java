import java.util.SequencedMap;

public final class IRCMessage347 extends IRCMessage {
  public static final String COMMAND = "347";
  private final String client;
  private final String channel;

  public IRCMessage347(
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
