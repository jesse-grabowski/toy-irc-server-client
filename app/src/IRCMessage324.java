import java.util.List;
import java.util.SequencedMap;

public final class IRCMessage324 extends IRCMessage {
  public static final String COMMAND = "324";
  private final String client;
  private final String channel;
  private final String modeString;
  private final List<String> modeArguments;

  public IRCMessage324(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String channel,
      String modeString,
      List<String> modeArguments) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.channel = channel;
    this.modeString = modeString;
    this.modeArguments = modeArguments;
  }

  public String getClient() {
    return client;
  }

  public String getChannel() {
    return channel;
  }

  public String getModeString() {
    return modeString;
  }

  public List<String> getModeArguments() {
    return modeArguments;
  }
}
