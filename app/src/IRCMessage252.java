import java.util.SequencedMap;

public final class IRCMessage252 extends IRCMessage {
  public static final String COMMAND = "252";
  private final String client;
  private final Integer ops;

  public IRCMessage252(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      Integer ops) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.ops = ops;
  }

  public String getClient() {
    return client;
  }

  public Integer getOps() {
    return ops;
  }
}
