import java.util.SequencedMap;

public final class IRCMessage266 extends IRCMessage {
    public static final String COMMAND="266";
    private final String client;
    private final String globalUsers;
    private final String maxGlobalUsers;
    private final String text;
    public IRCMessage266(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String globalUsers,String maxGlobalUsers,String text){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.globalUsers=globalUsers;
        this.maxGlobalUsers=maxGlobalUsers;
        this.text=text;
    }
    public String getClient(){return client;}
    public String getGlobalUsers(){return globalUsers;}
    public String getMaxGlobalUsers(){return maxGlobalUsers;}
    public String getText(){return text;}
}
