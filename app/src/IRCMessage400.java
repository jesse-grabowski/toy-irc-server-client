import java.util.List;
import java.util.SequencedMap;

public final class IRCMessage400 extends IRCMessage {
    public static final String COMMAND="400";
    private final String client;
    private final List<String> commands;
    private final String info;
    public IRCMessage400(String rawMessage, SequencedMap<String,String> tags, String prefixName, String prefixUser, String prefixHost, String client, List<String> commands, String info){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.commands = commands;
        this.info = info;
    }
    public String getClient(){return client;}
    public List<String> getCommands(){return commands;}
    public String getInfo(){return info;}
}
