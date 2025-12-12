import java.util.SequencedMap;

public final class IRCMessage338 extends IRCMessage {
    public static final String COMMAND="338";
    private final String client;
    private final String nick;
    private final String username;
    private final String hostname;
    private final String ip;
    public IRCMessage338(String rawMessage, SequencedMap<String,String> tags, String prefixName, String prefixUser, String prefixHost, String client, String nick, String username, String hostname, String ip){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.nick=nick;
        this.username = username;
        this.hostname = hostname;
        this.ip = ip;
    }
    public String getClient(){return client;}
    public String getNick(){return nick;}
    public String getUsername(){return username;}
    public String getHostname(){return hostname;}
    public String getIp(){return ip;}
}
