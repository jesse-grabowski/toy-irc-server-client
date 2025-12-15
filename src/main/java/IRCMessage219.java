import java.util.SequencedMap;

public final class IRCMessage219 extends IRCMessage {
  public static final String COMMAND = "219";
  private final String client;
  private final String statsLetter;

  public IRCMessage219(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String statsLetter) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.statsLetter = statsLetter;
  }

  public String getClient() {
    return client;
  }

  public String getStatsLetter() {
    return statsLetter;
  }
}
