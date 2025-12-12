import java.util.SequencedMap;

public final class IRCMessage403 extends IRCMessage {
  public static final String COMMAND = "403";
  private final String client;
  private final String channel;
  private final String text;

  public IRCMessage403(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public String getText() {
    return text;
  }
}
