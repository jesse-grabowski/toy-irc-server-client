import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageCAPLSResponse extends IRCMessage {

  public static final String COMMAND = "CAP";

  private final String nick;
  private final boolean hasMore;
  private final SequencedMap<String, String> capabilities;

  public IRCMessageCAPLSResponse(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String nick,
      boolean hasMore,
      SequencedMap<String, String> capabilities) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.nick = nick;
    this.hasMore = hasMore;
    this.capabilities = capabilities;
  }

  public String getNick() {
    return nick;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public SequencedMap<String, String> getCapabilities() {
    return capabilities;
  }
}
