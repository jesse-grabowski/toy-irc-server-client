import java.util.SequencedMap;

public final class IRCMessage256 extends IRCMessage {
  public static final String COMMAND = "256";
  private final String client;
  private final String server;
  private final String text;

  public IRCMessage256(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String server,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.server = server;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getServer() {
    return server;
  }

  public String getText() {
    return text;
  }
}
