import java.util.List;
import java.util.SequencedMap;

public final class IRCMessage302 extends IRCMessage {
  public static final String COMMAND = "302";
  private final String client;
  private final List<String> userhosts;

  public IRCMessage302(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      List<String> userhosts) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.userhosts = userhosts;
  }

  public String getClient() {
    return client;
  }

  public List<String> getUserhosts() {
    return userhosts;
  }
}
