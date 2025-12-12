import java.util.SequencedMap;

public final class IRCMessage382 extends IRCMessage {
    public static final String COMMAND="382";
    private final String client;
    private final String configFile;
    public IRCMessage382(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String configFile){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.configFile=configFile;
    }
    public String getClient(){return client;}
    public String getConfigFile(){return configFile;}
}
