import java.util.SequencedMap;

public final class IRCMessage212 extends IRCMessage {
    public static final String COMMAND="212";
    private final String client;
    private final String targetCommand;
    private final String count;
    private final String byteCount;
    private final String remoteCount;
    public IRCMessage212(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String targetCommand,String count,String byteCount,String remoteCount){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.targetCommand=targetCommand;
        this.count=count;
        this.byteCount=byteCount;
        this.remoteCount=remoteCount;
    }
    public String getClient(){return client;}
    public String getTargetCommand(){return targetCommand;}
    public String getCount(){return count;}
    public String getByteCount(){return byteCount;}
    public String getRemoteCount(){return remoteCount;}
}
