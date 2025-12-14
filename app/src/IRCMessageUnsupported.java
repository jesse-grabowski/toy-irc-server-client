import java.util.SequencedMap;

public final class IRCMessageUnsupported extends IRCMessage {

  private final String reason;

  public IRCMessageUnsupported(
      String command,
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String reason) {
    super(command, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.reason = reason;
  }

  public String getReason() {
    return reason;
  }
}
