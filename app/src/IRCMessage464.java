import java.util.SequencedMap;

public final class IRCMessage464 extends IRCMessage {
  public static final String COMMAND = "464";
  private final String client;

  public IRCMessage464(
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
