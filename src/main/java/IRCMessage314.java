import java.util.SequencedMap;

public final class IRCMessage314 extends IRCMessage {
  public static final String COMMAND = "314";
  private final String client;
  private final String nick;
  private final String user;
  private final String host;
  private final String realName;

  public IRCMessage314(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      String user,
      String host,
      String realName) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.user = user;
    this.host = host;
    this.realName = realName;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public String getUser() {
    return user;
  }

  public String getHost() {
    return host;
  }

  public String getRealName() {
    return realName;
  }
}
