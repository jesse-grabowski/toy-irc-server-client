import java.util.SequencedMap;

public final class IRCMessage472 extends IRCMessage {
  public static final String COMMAND = "472";
  private final String client;
  private final Character mode;
  private final String text;

  public IRCMessage472(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      Character mode,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.mode = mode;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public Character getMode() {
    return mode;
  }

  public String getText() {
    return text;
  }
}
