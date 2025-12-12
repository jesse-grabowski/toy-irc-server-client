import java.util.SequencedMap;

public final class IRCMessage312 extends IRCMessage {
  public static final String COMMAND = "312";
  private final String client;
  private final String nick;
  private final String server;
  private final String serverInfo;

  public IRCMessage312(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      String server,
      String serverInfo) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.server = server;
    this.serverInfo = serverInfo;
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public String getServer() {
    return server;
  }

  public String getServerInfo() {
    return serverInfo;
  }
}
