import java.util.SequencedMap;

public final class IRCMessage422 extends IRCMessage {
    public static final String COMMAND="422";
    private final String client;
    public IRCMessage422(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
    }
    public String getClient(){return client;}
}
