import java.util.SequencedMap;

public final class IRCMessage723 extends IRCMessage {
    public static final String COMMAND="723";
    private final String client;
    private final String priv;
    private final String text;
    public IRCMessage723(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String priv,String text){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.priv=priv;
        this.text=text;
    }
    public String getClient(){return client;}
    public String getPriv(){return priv;}
    public String getText(){return text;}
}
