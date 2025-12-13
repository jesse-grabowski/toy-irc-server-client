import java.util.SequencedMap;

public final class IRCMessage315 extends IRCMessage {
  public static final String COMMAND = "315";
  private final String client;
  private final String mask;

  public IRCMessage315(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String mask) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.mask = mask;
  }

  public String getClient() {
    return client;
  }

  public String getMask() {
    return mask;
  }
}
