import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageCAPEND extends IRCMessage {

  public static final String COMMAND = "CAP";

  public IRCMessageCAPEND(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
  }

  public IRCMessageCAPEND() {
    this(null, new LinkedHashMap<>(), null, null, null);
  }
}
