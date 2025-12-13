import java.util.SequencedMap;

public final class IRCMessage265 extends IRCMessage {
  public static final String COMMAND = "265";
  private final String client;
  private final Integer localUsers;
  private final Integer maxLocalUsers;
  private final String text;

  public IRCMessage265(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      Integer localUsers,
      Integer maxLocalUsers,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.localUsers = localUsers;
    this.maxLocalUsers = maxLocalUsers;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public Integer getLocalUsers() {
    return localUsers;
  }

  public Integer getMaxLocalUsers() {
    return maxLocalUsers;
  }

  public String getText() {
    return text;
  }
}
