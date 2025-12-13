import java.util.SequencedMap;

public final class IRCMessage391 extends IRCMessage {
  public static final String COMMAND = "391";
  private final String client;
  private final String server;
  private final Long timestamp;
  private final String tsOffset;
  private final String timeString;

  public IRCMessage391(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String server,
      Long timestamp,
      String tsOffset,
      String timeString) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.server = server;
    this.timestamp = timestamp;
    this.tsOffset = tsOffset;
    this.timeString = timeString;
  }

  public String getClient() {
    return client;
  }

  public String getServer() {
    return server;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public String getTsOffset() {
    return tsOffset;
  }

  public String getTimeString() {
    return timeString;
  }
}
