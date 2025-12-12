import java.util.SequencedMap;

public final class IRCMessage706 extends IRCMessage {
  public static final String COMMAND = "706";
  private final String client;
  private final String subject;
  private final String text;

  public IRCMessage706(
      String rawMessage,
      SequencedMap<String, String> tags,
      String prefixName,
      String prefixUser,
      String prefixHost,
      String client,
      String subject,
      String text) {
    super(COMMAND, rawMessage, tags, prefixName, prefixUser, prefixHost);
    this.client = client;
    this.subject = subject;
    this.text = text;
  }

  public String getClient() {
    return client;
  }

  public String getSubject() {
    return subject;
  }

  public String getText() {
    return text;
  }
}
