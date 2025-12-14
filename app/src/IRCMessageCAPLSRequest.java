import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageCAPLSRequest extends IRCMessage {

  public static final String COMMAND = "CAP";

  private final String version;

  public IRCMessageCAPLSRequest(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String version) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.version = version;
  }

  public IRCMessageCAPLSRequest(String version) {
    this(null, new LinkedHashMap<>(), null, null, null, version);
  }

  public String getVersion() {
    return version;
  }
}
