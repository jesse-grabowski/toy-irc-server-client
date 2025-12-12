import java.util.SequencedMap;

public final class IRCMessage364 extends IRCMessage {
    public static final String COMMAND="364";
    private final String client;
    private final String server1;
    private final String server2;
    private final String hopCount;
    private final String serverInfo;
    public IRCMessage364(String rawMessage, SequencedMap<String,String> tags, String prefixName, String prefixUser, String prefixHost, String client, String server1, String server2, String hopCount, String serverInfo){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.server1 = server1;
        this.server2 = server2;
        this.hopCount = hopCount;
        this.serverInfo=serverInfo;
    }
    public String getClient(){return client;}
    public String getServer1(){return server1;}
    public String getServer2(){return server2;}
    public String getHopCount(){return hopCount;}
    public String getServerInfo(){return serverInfo;}
}
