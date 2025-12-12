import java.util.SequencedMap;

public final class IRCMessage259 extends IRCMessage {
    public static final String COMMAND="259";
    private final String client;
    private final String info;
    public IRCMessage259(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String info){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.info=info;
    }
    public String getClient(){return client;}
    public String getInfo(){return info;}
}
