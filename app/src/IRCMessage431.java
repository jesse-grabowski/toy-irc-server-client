import java.util.SequencedMap;

public final class IRCMessage431 extends IRCMessage {
  public static final String COMMAND = "431";
  private final String client;

  public IRCMessage431(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
  }

  public String getClient() {
    return client;
  }
}
