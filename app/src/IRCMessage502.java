import java.util.SequencedMap;

public final class IRCMessage502 extends IRCMessage {
  public static final String COMMAND = "502";
  private final String client;
  private final String text;

  public IRCMessage502(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getText() {
    return text;
  }
}
