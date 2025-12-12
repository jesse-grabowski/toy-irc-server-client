import java.util.SequencedMap;

public final class IRCMessage346 extends IRCMessage {
  public static final String COMMAND = "346";
  private final String client;
  private final String channel;
  private final String mask;

  public IRCMessage346(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      String mask) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.mask = mask;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public String getMask() {
    return mask;
  }
}
