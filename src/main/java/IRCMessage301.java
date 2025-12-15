import java.util.SequencedMap;

public final class IRCMessage301 extends IRCMessage {
  public static final String COMMAND = "301";
  private final String client;
  private final String nick;
  private final String awayMessage;

  public IRCMessage301(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      String awayMessage) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.awayMessage = awayMessage;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public String getAwayMessage() {
    return awayMessage;
  }
}
