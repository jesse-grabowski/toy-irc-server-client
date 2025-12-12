import java.util.SequencedMap;

public final class IRCMessage315 extends IRCMessage {
  public static final String COMMAND = "315";
  private final String client;
  private final String mask;
  private final String text;

  public IRCMessage315(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String mask,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.mask = mask;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getMask() {
    return mask;
  }

  public String getText() {
    return text;
  }
}
