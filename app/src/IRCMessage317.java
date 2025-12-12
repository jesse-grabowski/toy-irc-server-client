import java.util.SequencedMap;

public final class IRCMessage317 extends IRCMessage {
    public static final String COMMAND="317";
    private final String client;
    private final String nick;
    private final String secondsIdle;
    private final String signOnTime;
    public IRCMessage317(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String nick,String secondsIdle,String signOnTime){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.nick=nick;
        this.secondsIdle=secondsIdle;
        this.signOnTime=signOnTime;
    }
    public String getClient(){return client;}
    public String getNick(){return nick;}
    public String getSecondsIdle(){return secondsIdle;}
    public String getSignOnTime(){return signOnTime;}
}
