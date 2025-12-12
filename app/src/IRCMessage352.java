import java.util.SequencedMap;

public final class IRCMessage352 extends IRCMessage {
  public static final String COMMAND = "352";
  private final String client;
  private final String channel;
  private final String user;
  private final String host;
  private final String server;
  private final String nick;
  private final String flags;
  private final String hopcount;
  private final String realName;

  public IRCMessage352(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      String user,
      String host,
      String server,
      String nick,
      String flags,
      String hopcount,
      String realName) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.user = user;
    this.host = host;
    this.server = server;
    this.nick = nick;
    this.flags = flags;
    this.hopcount = hopcount;
    this.realName = realName;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public String getUser() {
    return user;
  }

  public String getHost() {
    return host;
  }

  public String getServer() {
    return server;
  }

  public String getNick() {
    return nick;
  }

  public String getFlags() {
    return flags;
  }

  public String getHopcount() {
    return hopcount;
  }

  public String getRealName() {
    return realName;
  }
}
