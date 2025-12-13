import java.util.SequencedMap;

public final class IRCMessage212 extends IRCMessage {
  public static final String COMMAND = "212";
  private final String client;
  private final String targetCommand;
  private final Integer count;
  private final Integer byteCount;
  private final Integer remoteCount;

  public IRCMessage212(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String targetCommand,
      Integer count,
      Integer byteCount,
      Integer remoteCount) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.targetCommand = targetCommand;
    this.count = count;
    this.byteCount = byteCount;
    this.remoteCount = remoteCount;
  }

  public String getClient() {
    return client;
  }

  public String getTargetCommand() {
    return targetCommand;
  }

  public Integer getCount() {
    return count;
  }

  public Integer getByteCount() {
    return byteCount;
  }

  public Integer getRemoteCount() {
    return remoteCount;
  }
}
