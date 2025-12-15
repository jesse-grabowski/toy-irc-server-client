import java.util.SequencedMap;

public final class IRCMessage378 extends IRCMessage {
  public static final String COMMAND = "378";
  private final String client;
  private final String nick;
  private final String text;

  public IRCMessage378(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public String getText() {
    return text;
  }
}
