import java.util.SequencedMap;

public final class IRCMessage421 extends IRCMessage {
    public static final String COMMAND="421";
    private final String client;
    private final String invalidCommand;
    public IRCMessage421(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String invalidCommand){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.invalidCommand=invalidCommand;
    }
    public String getClient(){return client;}
    public String getInvalidCommand(){return invalidCommand;}
}
