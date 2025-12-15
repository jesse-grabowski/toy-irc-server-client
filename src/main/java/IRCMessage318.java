import java.util.SequencedMap;

public final class IRCMessage318 extends IRCMessage {
  public static final String COMMAND = "318";
  private final String client;
  private final String nick;

  public IRCMessage318(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }
}
