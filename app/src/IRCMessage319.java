import java.util.List;
import java.util.SequencedMap;

public final class IRCMessage319 extends IRCMessage {
    public static final String COMMAND="319";
    private final String client;
    private final String nick;
    private final List<String> prefixes;
    private final List<String> channels;
    public IRCMessage319(String rawMessage,SequencedMap<String,String> tags,String prefixName,String prefixUser,String prefixHost,String client,String nick,List<String> prefixes,List<String> channels){
        super(COMMAND,rawMessage,tags,prefixName,prefixUser,prefixHost);
        this.client=client;
        this.nick=nick;
        this.prefixes = prefixes;
        this.channels=channels;
    }
    public String getClient(){return client;}
    public String getNick(){return nick;}

    public List<String> getPrefixes() {
        return prefixes;
    }

    public List<String> getChannels() {
        return channels;
    }
}
