import java.util.SequencedMap;

public final class IRCMessage254 extends IRCMessage {
  public static final String COMMAND = "254";
  private final String client;
  private final String channels;

  public IRCMessage254(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channels) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channels = channels;
  }

  public String getClient() {
    return client;
  }

  public String getChannels() {
    return channels;
  }
}
