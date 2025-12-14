import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class IRCMessageTOPIC extends IRCMessage {

  public static final String COMMAND = "TOPIC";

  private final String channel;
  private final String topic;

  public IRCMessageTOPIC(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String channel,
      String topic) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.channel = channel;
    this.topic = topic;
  }

  public IRCMessageTOPIC(String channel, String topic) {
    this(null, new LinkedHashMap<>(), null, null, null, channel, topic);
  }

  public String getChannel() {
    return channel;
  }

  public String getTopic() {
    return topic;
  }
}
