import java.util.SequencedMap;

public final class IRCMessage443 extends IRCMessage {
    public static final String COMMAND="443";
    private final String client;
    private final String nick;
    private final String channel;
    private final String text;
    public IRCMessage443(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String nick,String channel,String text){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.nick=nick;
        this.channel=channel;
        this.text=text;
    }
    public String getClient(){return client;}
    public String getNick(){return nick;}
    public String getChannel(){return channel;}
    public String getText(){return text;}
}
