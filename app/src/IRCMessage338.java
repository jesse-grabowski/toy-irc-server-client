import java.util.SequencedMap;

public final class IRCMessage338 extends IRCMessage {
  public static final String COMMAND = "338";
  private final String client;
  private final String nick;
  private final String username;
  private final String hostname;
  private final String ip;
  private final String text;

  public IRCMessage338(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String nick,
      String username,
      String hostname,
      String ip,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.nick = nick;
    this.username = username;
    this.hostname = hostname;
    this.ip = ip;
    this.text = text;
  }

  public static IRCMessage338 forClientNick(
          String rawMessage,
          SequencedMap<String, String> tags,
          String prefixName,
          String prefixUser,
          String prefixHost,
          String client,
          String nick,
          String text) {
    return new IRCMessage338(
            rawMessage,
            tags,
            prefixName,
            prefixUser,
            prefixHost,
            client,
            nick,
            null,
            null,
            null,
            text);
  }

  public static IRCMessage338 forHost(
          String rawMessage,
          SequencedMap<String, String> tags,
          String prefixName,
          String prefixUser,
          String prefixHost,
          String client,
          String nick,
          String hostname,
          String text) {
    return new IRCMessage338(
        rawMessage,
        tags,
        prefixName,
        prefixUser,
        prefixHost,
        client,
        nick,
        null,
        hostname,
        null,
            text);
  }

  public static IRCMessage338 forIp(
          String rawMessage,
          SequencedMap<String, String> tags,
          String prefixName,
          String prefixUser,
          String prefixHost,
          String client,
          String nick,
          String ip,
          String text) {
    return new IRCMessage338(
            rawMessage,
            tags,
            prefixName,
            prefixUser,
            prefixHost,
            client,
            nick,
            null,
            null,
            ip,
            text);
  }

  public String getClient() {
    return client;
  }

  public String getNick() {
    return nick;
  }

  public String getUsername() {
    return username;
  }

  public String getHostname() {
    return hostname;
  }

  public String getIp() {
    return ip;
  }

  public String getText() {
    return text;
  }
}
