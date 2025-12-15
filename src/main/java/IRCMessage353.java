import java.util.List;
import java.util.SequencedMap;

public final class IRCMessage353 extends IRCMessage {

  public static final String COMMAND = "353";

  private final String client;
  private final String symbol;
  private final String channel;
  private final List<String> nicks;
  private final List<Character> modes;

  public IRCMessage353(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String symbol,
      String channel,
      List<String> nicks,
      List<Character> modes) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.symbol = symbol;
    this.channel = channel;
    this.nicks = nicks;
    this.modes = modes;
  }

  public String getClient() {
    return client;
  }

  public String getSymbol() {
    return symbol;
  }

  public String getChannel() {
    return channel;
  }

  public List<String> getNicks() {
    return nicks;
  }

  public List<Character> getModes() {
    return modes;
  }
}
