import java.util.SequencedMap;

public final class IRCMessage265 extends IRCMessage {
    public static final String COMMAND="265";
    private final String client;
    private final String localUsers;
    private final String maxLocalUsers;
    private final String text;
    public IRCMessage265(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String localUsers,String maxLocalUsers,String text){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.localUsers=localUsers;
        this.maxLocalUsers=maxLocalUsers;
        this.text=text;
    }
    public String getClient(){return client;}
    public String getLocalUsers(){return localUsers;}
    public String getMaxLocalUsers(){return maxLocalUsers;}
    public String getText(){return text;}
}
