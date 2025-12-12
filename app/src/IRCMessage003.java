import java.util.SequencedMap;

public final class IRCMessage003 extends IRCMessage {
  public static final String COMMAND = "003";
  private final String client;
  private final String message;

  public IRCMessage003(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String message) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.message = message;
  }

  public String getClient() {
    return client;
  }

  public String getMessage() {
    return message;
  }
}
