import java.util.SequencedMap;

public final class IRCMessage351 extends IRCMessage {
  public static final String COMMAND = "351";
  private final String client;
  private final String version;
  private final String server;
  private final String comments;

  public IRCMessage351(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String version,
      String server,
      String comments) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.version = version;
    this.server = server;
    this.comments = comments;
  }

  public String getClient() {
    return client;
  }

  public String getVersion() {
    return version;
  }

  public String getServer() {
    return server;
  }

  public String getComments() {
    return comments;
  }
}
