import java.util.SequencedMap;

public final class IRCMessage221 extends IRCMessage {
  public static final String COMMAND = "221";
  private final String client;
  private final String userModes;

  public IRCMessage221(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String userModes) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.userModes = userModes;
  }

  public String getClient() {
    return client;
  }

  public String getUserModes() {
    return userModes;
  }
}
