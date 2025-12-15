import java.util.SequencedMap;

public final class IRCMessage379 extends IRCMessage {
  public static final String COMMAND = "379";
  private final String client;
  private final String nick;
  private final String modes;

  public IRCMessage379(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      String modes) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.modes = modes;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public String getModes() {
    return modes;
  }
}
