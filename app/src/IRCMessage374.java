import java.util.SequencedMap;

public final class IRCMessage374 extends IRCMessage {
    public static final String COMMAND="374";
    private final String client;
    public IRCMessage374(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
    }
    public String getClient(){return client;}
}
