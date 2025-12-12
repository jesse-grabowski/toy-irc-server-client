import java.util.SequencedMap;

public final class IRCMessage330 extends IRCMessage {
    public static final String COMMAND="330";
    private final String client;
    private final String nick;
    private final String account;
    public IRCMessage330(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String nick,String account){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.nick=nick;
        this.account=account;
    }
    public String getClient(){return client;}
    public String getNick(){return nick;}
    public String getAccount(){return account;}
}
